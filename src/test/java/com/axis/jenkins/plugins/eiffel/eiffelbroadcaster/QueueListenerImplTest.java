/**
 The MIT License

 Copyright 2021-2024 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEventFactory;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class QueueListenerImplTest {
    @Test
    public void testAddTriggerFromEiffelCause_AddsAllLinks() {
        var links = Arrays.asList(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CONTEXT, UUID.randomUUID()),
                new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, UUID.randomUUID()),
                new EiffelEvent.Link(EiffelEvent.Link.Type.FLOW_CONTEXT, UUID.randomUUID()));
        var cause = new EiffelCause(links);
        var event = EiffelEventFactory.getInstance().create(EiffelActivityTriggeredEvent.class);
        event.getData().setName("activity name");
        var queueListener = new QueueListenerImpl();
        queueListener.addTriggerFromEiffelCause(cause, event,
                new EiffelActivityTriggeredEvent.Data.Trigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER));

        assertThat(event.getLinks(), is(links));
    }

    @Test
    public void testAddTriggerFromEiffelCause_NoTriggerForNonCauseLink() {
        var links = Arrays.asList(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CONTEXT, UUID.randomUUID()),
                new EiffelEvent.Link(EiffelEvent.Link.Type.FLOW_CONTEXT, UUID.randomUUID()));
        var cause = new EiffelCause(links);
        var event = EiffelEventFactory.getInstance().create(EiffelActivityTriggeredEvent.class);
        event.getData().setName("activity name");
        var queueListener = new QueueListenerImpl();
        queueListener.addTriggerFromEiffelCause(cause, event,
                new EiffelActivityTriggeredEvent.Data.Trigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER));

        assertThat(event.getData().getTriggers(), is(emptyIterable()));
    }

    @Test
    public void testAddTriggerFromEiffelCause_TriggerForCauseLink() {
        var cause1 = UUID.randomUUID();
        var cause2 = UUID.randomUUID();
        var links = Arrays.asList(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, cause1),
                new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, cause2));
        var cause = new EiffelCause(links);
        var event = EiffelEventFactory.getInstance().create(EiffelActivityTriggeredEvent.class);
        event.getData().setName("activity name");
        var queueListener = new QueueListenerImpl();
        queueListener.addTriggerFromEiffelCause(cause, event,
                new EiffelActivityTriggeredEvent.Data.Trigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER));

        assertThat(event.getData().getTriggers(), not(emptyIterable()));
        var trigger = event.getData().getTriggers().get(0);
        assertThat(trigger.getType(), is(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT));
        assertThat(trigger.getDescription(), containsString(cause1.toString()));
        assertThat(trigger.getDescription(), containsString(cause2.toString()));
    }
}
