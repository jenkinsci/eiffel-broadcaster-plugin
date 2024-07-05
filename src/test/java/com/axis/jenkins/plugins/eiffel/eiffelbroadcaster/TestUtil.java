/**
 The MIT License

 Copyright 2020-2024 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEventFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import hudson.util.Secret;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Utility methods for running e.g. integration tests and configuration setup.
 *
 * @author Hampus Johansson
 */
public final class TestUtil {

    public static final String EXCHANGE = "jenkins";
    public static final String QUEUE_NAME = "test-queue";
    public static final int PORT = 5672;

    private static RabbitMQContainer defaultMQContainer = null;

    /**
     * Creates a default RabbitMQ container to run tests against. It declares an Exchange (EXCHANGE)
     * with a binding to a queue (QUEUE_NAME). No routing queue should be specified. The container may
     * be started by, for example, using a jUnit @Rule.
     *
     * @return an RabbitMQ container singleton. A container is not started.
     */
    public static RabbitMQContainer getDefaultMQContainer() {
        if (defaultMQContainer == null) {
            defaultMQContainer = new RabbitMQContainer("rabbitmq:3-management")
                    .withExposedPorts(PORT, 15672)
                    .withExchange(EXCHANGE, "direct")
                    .withQueue(QUEUE_NAME)
                    .withBinding(EXCHANGE, QUEUE_NAME);
        }
        return defaultMQContainer;
    }

    /**
     * Set common configuration values, intended for use together with the RabbitMQ container.
     *
     * @param config A configuration object to set default configuration for.
     */
    public static void setDefaultConfig(EiffelBroadcasterConfig config) {
        config.setUserName("guest");
        config.setUserPassword(Secret.fromString("guest"));
        config.setExchangeName(EXCHANGE);
        config.setRoutingKey("");
        config.setVirtualHost(null);
        config.setEnableBroadcaster(true);
    }

    /**
     * Wait for x events on the queue to be published.
     *
     * @param conn A connection to be used for connecting to RabbitMQ
     * @param numberExpectedEvents stop after finding this many events
     * @param secondsWaitPerEvent the max time to wait for an event
     * @param queueName the queue to listen to events on
     *
     * @return a list of the found EiffelEvent
     */
    public static ArrayList<EiffelEvent> waitForMessages(
            MQConnection conn,
            int numberExpectedEvents,
            int secondsWaitPerEvent,
            String queueName
    ) throws IOException, InterruptedException {
        Channel channel = null;

        while (channel == null) {
            try {
                channel = conn.getConnection().createChannel();
                Thread.sleep(1000);
            } catch (Exception ignored){}
        }
        LinkedBlockingQueue<EiffelEvent> foundMessages = new LinkedBlockingQueue<>();
        ArrayList<EiffelEvent> foundMessagesArray = new ArrayList<>();

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            ObjectMapper mapper = new ObjectMapper();

            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                foundMessages.offer(mapper.readValue(body, EiffelEvent.class));
                synchronized (foundMessages) {
                    foundMessages.notify();
                }
            }
        };

        channel.basicConsume(queueName, true, consumer);

        synchronized (foundMessages) {
            while(foundMessages.size() != numberExpectedEvents) {
                foundMessages.wait(Duration.ofSeconds(secondsWaitPerEvent).toMillis());
            }
            foundMessages.drainTo(foundMessagesArray);
        }
        return foundMessagesArray;
    }

    /**
     * Creates a list of x events. Useful for generating messages during testing.
     *
     * @param count the number of events to create
     *
     * @return list of events
     */
    public static ArrayList<EiffelEvent> createEvents(int count) {
        var result = new ArrayList<EiffelEvent>();
        for (int i = 0; i < count; i++) {
            var artC = EiffelEventFactory.getInstance().create(EiffelArtifactCreatedEvent.class);
            artC.getData().setIdentity(String.format("pkg:test@%d", i));
            result.add(artC);
        }
        return result;
    }

}
