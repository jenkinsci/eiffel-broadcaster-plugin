package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class QueueListenerImplTest {
    @Test
    public void testAddTriggerFromEiffelCause_AddsAllLinks() {
        List<EiffelEvent.Link> links = Arrays.asList(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CONTEXT, UUID.randomUUID()),
                new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, UUID.randomUUID()),
                new EiffelEvent.Link(EiffelEvent.Link.Type.FLOW_CONTEXT, UUID.randomUUID()));
        EiffelCause cause = new EiffelCause(links);
        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent("activity name");
        QueueListenerImpl queueListener = new QueueListenerImpl();
        queueListener.addTriggerFromEiffelCause(cause, event,
                new EiffelActivityTriggeredEvent.Data.Trigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER));

        assertThat(event.getLinks(), is(links));
    }

    @Test
    public void testAddTriggerFromEiffelCause_NoTriggerForNonCauseLink() {
        List<EiffelEvent.Link> links = Arrays.asList(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CONTEXT, UUID.randomUUID()),
                new EiffelEvent.Link(EiffelEvent.Link.Type.FLOW_CONTEXT, UUID.randomUUID()));
        EiffelCause cause = new EiffelCause(links);
        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent("activity name");
        QueueListenerImpl queueListener = new QueueListenerImpl();
        queueListener.addTriggerFromEiffelCause(cause, event,
                new EiffelActivityTriggeredEvent.Data.Trigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER));

        assertThat(event.getData().getTriggers(), is(emptyIterable()));
    }

    @Test
    public void testAddTriggerFromEiffelCause_TriggerForCauseLink() {
        UUID cause1 = UUID.randomUUID();
        UUID cause2 = UUID.randomUUID();
        List<EiffelEvent.Link> links = Arrays.asList(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, cause1),
                new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, cause2));
        EiffelCause cause = new EiffelCause(links);
        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent("activity name");
        QueueListenerImpl queueListener = new QueueListenerImpl();
        queueListener.addTriggerFromEiffelCause(cause, event,
                new EiffelActivityTriggeredEvent.Data.Trigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER));

        assertThat(event.getData().getTriggers(), not(emptyIterable()));
        EiffelActivityTriggeredEvent.Data.Trigger trigger = event.getData().getTriggers().get(0);
        assertThat(trigger.getType(), is(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT));
        assertThat(trigger.getDescription(), containsString(cause1.toString()));
        assertThat(trigger.getDescription(), containsString(cause2.toString()));
    }
}
