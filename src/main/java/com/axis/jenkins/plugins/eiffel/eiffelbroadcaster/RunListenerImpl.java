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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityStartedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives notifications about builds and publish messages on configured MQ server.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {
    private static final Logger logger = LoggerFactory.getLogger(RunListenerImpl.class);

    /**
     * The name given to the Jenkins console log in {@link EiffelActivityStartedEvent.Data.LiveLogs}
     * and {@link EiffelActivityFinishedEvent.Data.PersistentLogs} objects.
     */
    public static final String CONSOLE_LOG_NAME = "Jenkins console log";

    /** The URI path to the plain console log of a {@link Run}, relative to the URI of the Run. */
    public static final String CONSOLE_URI_PATH = "consoleText";

    /**
     * Constructor for RunListenerImpl.
     */
    public RunListenerImpl() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        var targetEvent = EiffelJobTable.getInstance().getAndClearEventTrigger(r.getQueueId());
        if (targetEvent == null) {
            logger.warn("The newly started {} could not be mapped to an emitted ActT event", r);
            return;
        }
        var event = new EiffelActivityStartedEvent(targetEvent);

        var runUri = Util.getRunUri(r);
        if (runUri != null) {
            event.getData().setExecutionUri(runUri);
        }
        var logUri = Util.getRunUri(r, CONSOLE_URI_PATH);
        if (logUri != null) {
            event.getData().getLiveLogs().add(
                    new EiffelActivityStartedEvent.Data.LiveLogs(CONSOLE_LOG_NAME, logUri));
        }

        try {
            r.getAction(EiffelActivityAction.class).setStartedEvent(event);
        } catch (JsonProcessingException e) {
            // If there's a problem serializing the event it'll get logged when we try
            // to publish the event. No need to log the same error message twice.
        }
        Util.publishEvent(event, true);
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        var res = r.getResult();
        var conclusion = EiffelActivityFinishedEvent.Data.Outcome.Conclusion.INCONCLUSIVE;
        if (res != null) {
            conclusion = Util.translateStatus(res.toString());
        }

        var activityAction = r.getAction(EiffelActivityAction.class);
        if (activityAction == null) {
            logger.warn("Unable to locate {} for {}, skipping sending of ActF event",
                    EiffelActivityAction.class.getSimpleName(), r);
            return;
        }
        EiffelActivityFinishedEvent event = null;
        try {
            event = new EiffelActivityFinishedEvent(new EiffelActivityFinishedEvent.Data.Outcome(conclusion),
                    activityAction.getTriggerEvent().getMeta().getId());
        } catch (JsonProcessingException e) {
            logger.warn("JSON deserialization of ActT event for {} unexpectedly failed, " +
                            "skipping sending of ActF event: {}",
                    r, e);
            return;
        }

        var logUri = Util.getRunUri(r, CONSOLE_URI_PATH);
        if (logUri != null) {
            event.getData().getPersistentLogs().add(
                    new EiffelActivityFinishedEvent.Data.PersistentLogs(CONSOLE_LOG_NAME, logUri));
        }

        try {
            r.getAction(EiffelActivityAction.class).setFinishedEvent(event);
        } catch (JsonProcessingException e) {
            // If there's a problem serializing the event it'll get logged when we try
            // to publish the event. No need to log the same error message twice.
        }
        Util.publishEvent(event, true);
    }
}
