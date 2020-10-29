/**
 The MIT License

 Copyright 2020 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidationFailedException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.SchemaUnavailableException;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.RabbitMQContainer;

import static org.junit.Assert.*;

/**
 * Integration tests for the MQConnection
 *
 * @author Hampus Johansson
 */
public class ConnectionIntegrationTest {
    private static final int DEFAULT_MESSAGE_WAIT = 10;

    RabbitMQContainer defaultMQContainer = TestUtil.getDefaultMQContainer();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(defaultMQContainer)
            .around(new JenkinsRule());

    /**
     * Test that the publisher sends messages correctly.
     */
    @Test
    public void testSentMessagesHaveCorrectFormat()
            throws EventValidationFailedException, IOException, InterruptedException, SchemaUnavailableException {
        EiffelBroadcasterConfig config = EiffelBroadcasterConfig.getInstance();
        assertNotNull("No config available: EiffelBroadcasterConfig", config);
        TestUtil.setDefaultConfig(config);
        config.setServerUri(defaultMQContainer.getAmqpUrl());

        MQConnection conn = MQConnection.getInstance();
        conn.initialize(config.getUserName(), config.getUserPassword(), config.getServerUri(), config.getVirtualHost());

        int messageCount = 10;
        ArrayList<EiffelEvent> expectedMessages = TestUtil.createEvents(messageCount);
        TestUtil.sendEventsWithinTimeframe(expectedMessages, DEFAULT_MESSAGE_WAIT);
        ArrayList<EiffelEvent> actualMessages = TestUtil.waitForMessages(
                conn,
                messageCount,
                DEFAULT_MESSAGE_WAIT,
                TestUtil.QUEUE_NAME
        );
        assertEquals(expectedMessages, actualMessages);
    }

}
