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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityCanceledEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.QueueListener;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import java.util.UUID;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives notifications about when tasks are submitted to the
 * queue and publishes messages on configured MQ server.
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
public class QueueListenerImpl extends QueueListener {
    private static final Logger logger = LoggerFactory.getLogger(QueueListenerImpl.class);

    @Override
    public void onEnterWaiting(Queue.WaitingItem wi) {
        String taskName = Util.getFullName(wi.task);

        // Filter out queue items we're not interested in, e.g. pipeline steps (modeled as
        // ExecutorStepExecution$PlaceholderTask).
        if (!(wi.task instanceof BuildableItem)) {
            logger.debug("Not emitting ActT event for {} object since it's not a BuildableItem: {}",
                    wi.task.getClass().getName(), taskName);
            return;
        }

        EiffelActivityTriggeredEvent.Data data = new EiffelActivityTriggeredEvent.Data(taskName);
        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent(data);
        EiffelJobTable.getInstance().setEventTrigger(wi.getId(), event.getMeta().getId());

        for (Cause cause : wi.getCauses()) {
            // Default to OTHER as the default trigger type and override it for specific known causes.
            EiffelActivityTriggeredEvent.Data.Trigger trigger = new EiffelActivityTriggeredEvent.Data.Trigger(
                    EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER);
            trigger.setDescription(cause.getShortDescription());

            if (cause instanceof TimerTrigger.TimerTriggerCause) {
                trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.TIMER);
            } else if (cause instanceof SCMTrigger.SCMTriggerCause) {
                trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.SOURCE_CHANGE);
            } else if (cause instanceof Cause.UpstreamCause) {
                UUID triggerId = getUpstreamTriggerId((Cause.UpstreamCause) cause);
                if (triggerId != null) {
                    event.getLinks().add(new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE, triggerId));
                    trigger.setType(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT);
                }
            }
            data.getTriggers().add(trigger);
        }

        try {
            wi.addOrReplaceAction(new EiffelActivityAction(event));
        } catch (JsonProcessingException e) {
            // If there's a problem serializing the event it'll get logged when we try
            // to publish the event. No need to log the same error message twice.
        }
        Util.publishEvent(event);
    }

    @Override
    public void onLeft(Queue.LeftItem li) {
        if (li.isCancelled()) {
            UUID targetEvent = EiffelJobTable.getInstance().getEventTrigger(li.getId());
            if (targetEvent == null) {
                logger.debug("A cancelled queue item could not be mapped to an emitted ActT event: {}", li);
                return;
            }
            EiffelActivityCanceledEvent event = new EiffelActivityCanceledEvent(targetEvent);
            Util.publishEvent(event);
        }
    }

    /**
     * Locates the ID of the EiffelActivityTriggeredEvent for the build referenced by
     * an {@link hudson.model.Cause.UpstreamCause}, or null if no such event was found.
     *
     * @param cause the cause for which to locate the Eiffel event
     */
    @CheckForNull
    private UUID getUpstreamTriggerId(Cause.UpstreamCause cause) {
        Run upstreamRun = cause.getUpstreamRun();
        if (upstreamRun == null) {
            return null;
        }
        EiffelActivityAction upstreamAction = upstreamRun.getAction(EiffelActivityAction.class);
        if (upstreamAction == null) {
            return null;
        }
        try {
            return upstreamAction.getTriggerEvent().getMeta().getId();
        } catch (JsonProcessingException e) {
            logger.warn("JSON payload stored in {} in {} couldn't be deserialized ({}): {}",
                    upstreamAction.getClass().getSimpleName(), upstreamRun, e, upstreamAction.getTriggerEventJSON());
            return null;
        }
    }
}
