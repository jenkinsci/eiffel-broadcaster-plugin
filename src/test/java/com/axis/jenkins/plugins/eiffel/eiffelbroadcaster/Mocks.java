/*
 The MIT License

 Copyright 2015 Sony Mobile Communications Inc. All rights reserved.
 Copyright 2021 Axis Communications AB.

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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import mockit.Mock;
import mockit.MockUp;

/**
 * Test mocks.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 * @author Magnus Bäck &lt;magnus.back@axis.com&gt;
 */
final class Mocks {
    /** Stores received messages. */
    public static final List<String> messages = new CopyOnWriteArrayList<>();

    // Private constructor to avoid unnecessary instantiation of the class
    private Mocks() { }

    /**
     * Mocks the {@link MQConnection} singleton and captures all received messages in {@link #messages}.
     * */
    public static final class RabbitMQConnectionMock extends MockUp<MQConnection> {
        @Mock
        public void addMessageToQueue(String exchangeName, String routingKey, AMQP.BasicProperties props, byte[] body) {
            messages.add(new String(body));
        }
    }
}
