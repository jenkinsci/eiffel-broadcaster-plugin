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
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.util.UUID;

/**
 * Receives notifications about builds and publish messages on configured MQ server.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {
    /**
     * Constructor for RunListenerImpl.
     */
    public RunListenerImpl() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        UUID targetEvent = EiffelJobTable.getInstance().getEventTrigger(r.getQueueId());
        EiffelActivityStartedEvent event = new EiffelActivityStartedEvent(targetEvent);
        Util.publishEvent(event);
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        Result res = r.getResult();
        EiffelActivityFinishedEvent.Data.Outcome.Conclusion conclusion =
                EiffelActivityFinishedEvent.Data.Outcome.Conclusion.INCONCLUSIVE;
        if (res != null) {
            conclusion = Util.translateStatus(res.toString());
        }

        EiffelActivityFinishedEvent event = new EiffelActivityFinishedEvent(
                new EiffelActivityFinishedEvent.Data.Outcome(conclusion),
                EiffelJobTable.getInstance().getEventTrigger(r.getQueueId()));
        Util.publishEvent(event);
    }
}
