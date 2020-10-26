/**
 The MIT License

 Copyright 2018-2021 Axis Communications AB.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import hudson.util.Secret;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Creates an MQ connection.
 *
 * @author Isac Holm &lt;isac.holm@axis..com&gt;
 */
public final class MQConnection implements ShutdownListener {
    private static final Logger logger = LoggerFactory.getLogger(MQConnection.class);
    private static final int HEARTBEAT_INTERVAL = 30;
    private static final int MESSAGE_QUEUE_SIZE = 1000;
    private static final int SENDMESSAGE_TIMEOUT = 100;
    private static final int CONNECTION_WAIT = 10000;

    private volatile boolean initialized = false;
    private String userName;
    private Secret userPassword;
    private String serverUri;
    private String virtualHost;
    private Connection connection = null;
    private Channel channel = null;

    private volatile LinkedBlockingQueue messageQueue = new LinkedBlockingQueue(MESSAGE_QUEUE_SIZE);
    private Thread messageQueueThread;

    /**
     * Lazy-loaded singleton using the initialization-on-demand holder pattern.
     */
    private MQConnection() { }

    /**
     * Is only executed on {@link #getInstance()} invocation.
     */
    private static class LazyRabbit {
        private static final MQConnection INSTANCE = new MQConnection();
        private static final ConnectionFactory CF = new ConnectionFactory();
    }

    /**
     * Gets the instance.
     *
     * @return the instance
     */
    public static MQConnection getInstance() {
        return LazyRabbit.INSTANCE;
    }

    /**
     * Stores data for a RabbitMQ message.
     */
    private static final class MessageData {
        private String exchange;
        private String routingKey;
        private AMQP.BasicProperties props;
        private byte[] body;

        /**
         * Constructor.
         *
         * @param exchange the exchange to publish the message to
         * @param routingKey the routing key
         * @param props other properties for the message - routing headers etc
         * @param body the message body
         */
        private MessageData(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.props = props;
            this.body = body;
        }

        /**
         * Gets the exchange name.
         *
         * @return the exchange name
         */
        private String getExchange() {
            return exchange;
        }

        /**
         * Gets the routing key.
         *
         * @return the routing key
         */
        private String getRoutingKey() {
            return routingKey;
        }

        /**
         * Gets the connection properties.
         *
         * @return the connection properties
         */
        private AMQP.BasicProperties getProps() {
            return props;
        }

        /**
         * Gets the message body.
         *
         * @return the message body
         */
        private byte[] getBody() {
            return body;
        }
    }

    /**
     * Puts a message in the message queue.
     *
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     */
    public void addMessageToQueue(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
        startMessageQueueThread();
        MessageData messageData = new MessageData(exchange, routingKey, props, body);
        if (!messageQueue.offer(messageData)) {
            logger.error("addMessageToQueue() failed, RabbitMQ queue is full!");
        }
    }

    /**
     * Sends messages from the message queue.
     */
    private void sendMessages() {
        while (true) {
            try {
                MessageData messageData = (MessageData)messageQueue.poll(SENDMESSAGE_TIMEOUT,
                                                                         TimeUnit.MILLISECONDS);
                if (messageData != null) {
                    getInstance().send(messageData.getExchange(), messageData.getRoutingKey(),
                            messageData.getProps(), messageData.getBody());
                }
            } catch (InterruptedException ie) {
                logger.info("sendMessages() poll() was interrupted: ", ie);
            }
        }
    }

    /**
     * Start or restart the message queue thread as necessary. Requires that
     * the MQConnection has been initialized with the needed configuration.
     *
     * @return true if the message queue thread was started, otherwise false
     */
    private synchronized boolean startMessageQueueThread() {
        if (!initialized || (messageQueueThread != null && messageQueueThread.isAlive())) {
            return false;
        }
        messageQueueThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessages();
            }
        });
        messageQueueThread.start();
        logger.info("messageQueueThread recreated since it was null or not alive.");
        return true;
    }

    /**
     * Gets the connection factory that will enable a connection to the AMQP server.
     *
     * @return the connection factory
     */
    private ConnectionFactory getConnectionFactory() {
        if (LazyRabbit.CF != null) {
            try {
                // Try to recover the topology along with the connection.
                LazyRabbit.CF.setAutomaticRecoveryEnabled(true);
                // set requested heartbeat interval, in seconds
                LazyRabbit.CF.setRequestedHeartbeat(HEARTBEAT_INTERVAL);
                LazyRabbit.CF.setUri(serverUri);
                if (StringUtils.isNotEmpty(virtualHost)) {
                    LazyRabbit.CF.setVirtualHost(virtualHost);
                }
            } catch (KeyManagementException e) {
                logger.error("KeyManagementException: ", e);
            } catch (NoSuchAlgorithmException e) {
                logger.error("NoSuchAlgorithmException: ", e);
            } catch (URISyntaxException e) {
                logger.error("URISyntaxException: ", e);
            }
            if (StringUtils.isNotEmpty(userName)) {
                LazyRabbit.CF.setUsername(userName);
                if (StringUtils.isNotEmpty(Secret.toString(userPassword))) {
                    LazyRabbit.CF.setPassword(Secret.toString(userPassword));
                }
            }
        }
        return LazyRabbit.CF;
    }

    /**
     * Gets the connection.
     *
     * @return the connection.
     */
    public Connection getConnection() {
        if (connection == null) {
            try {
                connection = getConnectionFactory().newConnection();
                connection.addShutdownListener(this);
            } catch (IOException e) {
                logger.warn("Connection refused", e);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    /**
     * Initializes this instance with supplied values.
     *
     * @param name the user name
     * @param password the user password
     * @param uri the server uri
     * @param vh the virtual host
     */
    public void initialize(String name, Secret password, String uri, String vh) {
        userName = name;
        userPassword = password;
        serverUri = uri;
        virtualHost = vh;
        connection = null;
        channel = null;
        initialized = true;
        startMessageQueueThread();
    }

    /**
     * Sends a message.
     * Keeps trying to get a connection indefinitely.
     *
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     */
    private void send(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
        if (exchange == null) {
            logger.error("Invalid configuration, exchange must not be null.");
            return;
        }

        while (true) {
            try {
                if (channel == null || !channel.isOpen()) {
                    connection = getConnection();
                    if (connection != null) {
                        channel = connection.createChannel();
                        if (!getConnection().getAddress().isLoopbackAddress()) {
                            channel.exchangeDeclarePassive(exchange);
                        }
                    }
                }
            } catch (IOException|ShutdownSignalException e) {
                logger.error("Cannot create channel", e);
                channel = null; // reset
                break;
            }
            if (channel != null) {
                try {
                    channel.basicPublish(exchange, routingKey, props, body);
                } catch (IOException e) {
                    logger.error("Cannot publish message", e);
                } catch (AlreadyClosedException e) {
                    logger.error("Connection is already closed", e);
                }

                break;
            } else {
                try {
                    Thread.sleep(CONNECTION_WAIT);
                } catch (InterruptedException ie) {
                    logger.error("Thread.sleep() was interrupted", ie);
                }
            }
        }
    }

    @Override
    public void shutdownCompleted(ShutdownSignalException cause) {
        if (cause.isHardError()) {
            if (!cause.isInitiatedByApplication()) {
                logger.warn("MQ connection was suddenly disconnected.");
                try {
                    if (connection != null && connection.isOpen()) {
                        connection.close();
                    }
                    if (channel != null && channel.isOpen()) {
                        channel.close();
                    }
                } catch (IOException|AlreadyClosedException e) {
                    logger.error("MQ Connection disconnected: ", e);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } finally {
                    channel = null;
                    connection = null;
                }
            }
        } else {
            logger.warn("MQ channel was suddenly disconnected.");
        }
    }
}
