/**
 The MIT License

 Copyright 2024 Axis Communications AB.

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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline.build;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelActivityDataAction;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.QueueListenerImpl;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerStep;
import org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Defines a pipeline step that extends {@link BuildTriggerStep} to trigger a downstream build
 * with a custom activity name set in the Eiffel activity event {@link EiffelActivityTriggeredEvent} ActT.
 *
 * The step execution is overriden to use a modified copy of {@link BuildTriggerStepExecution} that will
 * add a new build action {@link EiffelActivityDataAction} to the downstream build that stores
 * the ActT activity name. This action is then used in {@link QueueListenerImpl} to override the
 * activity name when a new ActT is fired as the downstream build enters a waiting state in the build queue.
 */
public class BuildWithEiffelStep extends BuildTriggerStep {

    /**
     * The activity name of the Eiffel activity event {@link EiffelActivityTriggeredEvent} ActT
     * for the triggered downstream build.
     * */
    private String activityName;

    @DataBoundConstructor
    public BuildWithEiffelStep(String job) {
        super(job);
    }

    public String getActivityName() {
        return activityName;
    }

    @DataBoundSetter
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    @Extension
    public static class DescriptorImpl extends BuildTriggerStep.DescriptorImpl {

        @Override
        public Step newInstance(@Nullable StaplerRequest req, @NonNull JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }

        @Override
        public String getFunctionName() {
            return "buildWithEiffel";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Build a job with custom Eiffel activity name";
        }
    }

    @Override
    public StepExecution start(StepContext context) {
        return new BuildWithEiffelStepExecution(this, context, new EiffelActivityDataAction(getActivityName()));
    }

}
