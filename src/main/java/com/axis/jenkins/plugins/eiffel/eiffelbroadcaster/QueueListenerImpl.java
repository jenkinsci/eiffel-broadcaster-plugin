/**
 The MIT License

 Copyright 2018-2024 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityCanceledEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.signing.EventSigner;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.signing.SystemEventSigner;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Receives notifications about when tasks are submitted to the
 * queue and publishes messages on configured MQ server.
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
public class QueueListenerImpl extends QueueListener {
    private static final Logger logger = LoggerFactory.getLogger(QueueListenerImpl.class);

    private final EventSigner signer = new SystemEventSigner();

    @Override
    public void onEnterWaiting(Queue.WaitingItem wi) {
        var taskName = Util.getFullName(wi.task);

        // Filter out queue items we're not interested in, e.g. pipeline steps (modeled as
        // ExecutorStepExecution$PlaceholderTask).
        if (!(wi.task instanceof BuildableItem)) {
            logger.debug("Not emitting ActT event for {} object since it's not a BuildableItem: {}",
                    wi.task.getClass().getName(), taskName);
            return;
        }

        var data = new EiffelActivityTriggeredEvent.Data(taskName);
        var event = new EiffelActivityTriggeredEvent(data);
        EiffelJobTable.getInstance().setEventTrigger(wi.getId(), event.getMeta().getId());

        // Override activity name if item has action EiffelActivityDataAction
        wi.getAllActions().stream()
                .filter(action -> action instanceof EiffelActivityDataAction)
                .findFirst()
                .ifPresent(action -> {
                    // This should be moved into a new method when more overrides are added to buildWithEiffelStep
                    var activityName = ((EiffelActivityDataAction) action).getName();
                    data.setName(activityName == null ? data.getName() : activityName);
                });

        // Populate activity categories
        var categories = new TreeSet<String>();

        // ...with globally configured categories
        categories.addAll(EiffelBroadcasterConfig.getInstance().getActivityCategoriesList());

        // ...with job-specific categories
        if (wi.task instanceof Job<?, ?>) {
            var activityJobProperty =
                    ((Job<?, ?>) wi.task).getProperty(EiffelActivityJobProperty.class);
            if (activityJobProperty != null) {
                categories.addAll(activityJobProperty.getCategories());
            }
        }

        data.getCategories().addAll(categories);

        // Populate causes
        for (Cause cause : wi.getCauses()) {
            // Default to OTHER as the default trigger type and override it for specific known causes.
            var trigger = new EiffelActivityTriggeredEvent.Data.Trigger(
                    EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER);
            trigger.setDescription(cause.getShortDescription());

            if (cause instanceof TimerTrigger.TimerTriggerCause) {
                trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.TIMER);
                data.getTriggers().add(trigger);
            } else if (cause instanceof SCMTrigger.SCMTriggerCause) {
                trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.SOURCE_CHANGE);
                data.getTriggers().add(trigger);
            } else if (cause instanceof Cause.UpstreamCause) {
                addTriggerFromUpstreamCause((Cause.UpstreamCause) cause, event, trigger);
            } else if (cause instanceof EiffelCause) {
                addTriggerFromEiffelCause((EiffelCause) cause, event, trigger);
            } else {
                data.getTriggers().add(trigger);
            }
        }

        try {
            wi.addOrReplaceAction(new EiffelActivityAction(event));
        } catch (JsonProcessingException e) {
            // If there's a problem serializing the event it'll get logged when we try
            // to publish the event. No need to log the same error message twice.
        }
        Util.publishEvent(event, signer);
    }

    @Override
    public void onLeft(Queue.LeftItem li) {
        if (li.isCancelled()) {
            var targetEvent = EiffelJobTable.getInstance().getAndClearEventTrigger(li.getId());
            if (targetEvent == null) {
                logger.debug("A cancelled queue item could not be mapped to an emitted ActT event: {}", li);
                return;
            }
            var event = new EiffelActivityCanceledEvent(targetEvent);
            Util.publishEvent(event, signer);
        }
    }

    /**
     * Updates an {@link EiffelActivityTriggeredEvent} with trigger information and links based on
     * an {@link EiffelCause}. All links in the cause will unconditionally be added to the event,
     * and additionally if any of the links is a CAUSE link the trigger will be updated to
     * an EIFFEL_EVENT and added to the event.
     *
     * @param cause the cause from which to extract data
     * @param event the event to potentially update with additional links and trigger(s)
     * @param trigger a trigger pre-populated with the cause's short description that can be further modified
     *                and added to the event
     */
    void addTriggerFromEiffelCause(final EiffelCause cause, final EiffelActivityTriggeredEvent event,
                                   final EiffelActivityTriggeredEvent.Data.Trigger trigger) {
        event.getLinks().addAll(cause.getLinks());

        // Unless the EiffelCause contains a CAUSE link don't add an EIFFEL_EVENT trigger.
        var causeEvents = cause.getLinks().stream()
                .filter(link -> link.getType() == EiffelEvent.Link.Type.CAUSE)
                .collect(Collectors.toList());
        if (!causeEvents.isEmpty()) {
            var causeEventCommaString = causeEvents.stream()
                    .map(link -> link.getTarget().toString())
                    .collect(Collectors.joining(", "));
            if (causeEvents.size() == 1) {
                trigger.setDescription("Triggered by this Eiffel event: " + causeEventCommaString);
            } else {
                trigger.setDescription("Triggered by these Eiffel events: " + causeEventCommaString);
            }
            trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT);
            event.getData().getTriggers().add(trigger);
        }
    }

    /**
     * Updates an {@link EiffelActivityTriggeredEvent} with trigger information and links based on the build
     * referenced by an {@link Cause.UpstreamCause}. If the upstream build has an EiffelActivityTriggeredEvent
     * associated with it it will be added as a CAUSE link.
     *
     * @param cause the cause for which to locate the Eiffel event
     * @param event the event to potentially update with additional links and trigger(s)
     * @param trigger a trigger pre-populated with the cause's short description that can be further modified
     *                and added to the event
     */
    void addTriggerFromUpstreamCause(final Cause.UpstreamCause cause, final EiffelActivityTriggeredEvent event,
                                     final EiffelActivityTriggeredEvent.Data.Trigger trigger) {
        var upstreamRun = cause.getUpstreamRun();
        if (upstreamRun != null) {
            var upstreamAction = upstreamRun.getAction(EiffelActivityAction.class);
            if (upstreamAction != null) {
                try {
                    event.getLinks().add(new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE,
                            upstreamAction.getTriggerEvent().getMeta().getId()));
                    trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT);
                } catch (JsonProcessingException e) {
                    logger.warn("JSON payload stored in {} in {} couldn't be deserialized ({}): {}",
                            upstreamAction.getClass().getSimpleName(), upstreamRun, e, upstreamAction.getTriggerEventJSON());
                }
            }
        }
        event.getData().getTriggers().add(trigger);
    }
}
