/**
 The MIT License

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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

/**
 * An {@link EnvironmentContributor} implementation that injects environment variables with the JSON payload of
 * the {@link Run}'s {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}
 * and {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityStartedEvent}. Theoretically
 * also the {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent} if this
 * class for some reason is used for a completed Run.
 */
@Extension
public class EiffelEnvironmentContributor extends EnvironmentContributor {
    /**
     * The name of the environment variable containing the
     * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent}.
     */
    public static final String ACTIVITY_FINISHED = "EIFFEL_ACTIVITY_FINISHED";

    /**
     * The name of the environment variable containing the
     * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityStartedEvent}.
     */
    public static final String ACTIVITY_STARTED = "EIFFEL_ACTIVITY_STARTED";

    /**
     * The name of the environment variable containing the
     * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}.
     */
    public static final String ACTIVITY_TRIGGERED = "EIFFEL_ACTIVITY_TRIGGERED";

    @Override
    public void buildEnvironmentFor(@NonNull Run r, @NonNull EnvVars envs, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        var action = r.getAction(EiffelActivityAction.class);
        if (action != null) {
            envs.put(EiffelEnvironmentContributor.ACTIVITY_TRIGGERED, action.getTriggerEventJSON());
            var startedEvent = action.getStartedEventJSON();
            if (startedEvent != null) {
                envs.put(EiffelEnvironmentContributor.ACTIVITY_STARTED, startedEvent);
            }
            var finishedEvent = action.getFinishedEventJSON();
            if (finishedEvent != null) {
                envs.put(EiffelEnvironmentContributor.ACTIVITY_FINISHED, finishedEvent);
            }
        }
    }
}
