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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelActivityAction;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Util;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidationFailedException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.InvalidJsonPayloadException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.SchemaUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Defines a pipeline step for sending an Eiffel event expressed as a {@link Map}. By default
 * the event passed by the user will be decorated with a CONTEXT link to the current build's
 * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}.
 * Optionally a CAUSE link can be created instead or the link can be omitted entirely.
 * <pre>
 * def event = [
 *     "meta": [
 *         "type": "EiffelCompositionDefinedEvent",
 *         "version": "3.0.0",
 *     ],
 *     "data": [
 *         "name": "my-composition",
 *     ],
 * ]
 * def sent = sendEiffelEvent event: event, activityLinkType: "CAUSE"
 * echo "This event was sent: ${sent}"
 * </pre>
 * This step returns immediately as soon as the event has been validated and put on
 * the internal outbound queue. The actual delivery of the event to the broker might
 * not have happened at the time of the return.
 */
public class SendEiffelEventStep extends Step {
    public static final String ERROR_MESSAGE_PREFIX = "Could not publish Eiffel event";

    private static final EnumSet<EiffelEvent.Link.Type> VALID_ACTIVITY_LINK_TYPES =
            EnumSet.of(EiffelEvent.Link.Type.CAUSE, EiffelEvent.Link.Type.CONTEXT);

    /** A {@link Map} representation of the event to send. */
    private final Map event;

    /**
     * If true, a link to the run's
     * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}
     * will be added. The link type is determined by the value of {@link #activityLinkType}.
     */
    private boolean linkToActivity = true;

    /** The type of the link that's added if {@link #linkToActivity} is true. */
    private EiffelEvent.Link.Type activityLinkType = EiffelEvent.Link.Type.CONTEXT;

    @DataBoundConstructor
    public SendEiffelEventStep(@Nonnull final Map event) {
        this.event = event;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    @Nonnull
    public Map getEvent() {
        return event;
    }

    public boolean getLinkToActivity() {
        return linkToActivity;
    }

    @DataBoundSetter
    public void setLinkToActivity(boolean linkToActivity) {
        this.linkToActivity = linkToActivity;
    }

    public EiffelEvent.Link.Type getActivityLinkType() {
        return activityLinkType;
    }

    @DataBoundSetter
    public void setActivityLinkType(EiffelEvent.Link.Type activityLinkType) {
        if (!VALID_ACTIVITY_LINK_TYPES.contains(activityLinkType)) {
            throw new IllegalArgumentException(
                    String.format("The activity link type must be one of: %s", VALID_ACTIVITY_LINK_TYPES));
        }
        this.activityLinkType = activityLinkType;
    }

    private static class Execution extends SynchronousStepExecution<Map> {
        private static final long serialVersionUID = 1L;
        private final transient SendEiffelEventStep step;

        public Execution(@Nonnull SendEiffelEventStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Map run() throws Exception {
            try {
                ObjectMapper mapper = new ObjectMapper();
                EiffelEvent event = mapper.convertValue(step.getEvent(), EiffelEvent.class);
                Run run = getContext().get(Run.class);
                if (step.getLinkToActivity()) {
                    EiffelActivityAction action = run.getAction(EiffelActivityAction.class);
                    // There should always be an EiffelActivityAction connected to the Run,
                    // but if not we can't do much than to crash the build.
                    event.getLinks().add(new EiffelEvent.Link(
                            step.getActivityLinkType(), action.getTriggerEvent().getMeta().getId()));
                }
                JsonNode sentJSON = Util.mustPublishEvent(event);
                TaskListener taskListener = getContext().get(TaskListener.class);
                if (sentJSON != null && taskListener != null) {
                    taskListener.getLogger().format(
                            "Successfully sent %s with id %s%n",
                            event.getMeta().getType(), event.getMeta().getId());
                }
                return mapper.convertValue(event, Map.class);
            } catch (EventValidationFailedException | IllegalArgumentException | InvalidJsonPayloadException
                    | SchemaUnavailableException e) {
                throw new AbortException(String.format(
                        "%s (%s): %s", ERROR_MESSAGE_PREFIX, e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    @Extension
    public static class Descriptor extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Send an Eiffel event";
        }

        @Override
        public String getFunctionName() {
            return "sendEiffelEvent";
        }
    }
}
