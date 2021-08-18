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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelArtifactPublisher;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelArtifactToPublishAction;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EmptyArtifactException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.MissingArtifactException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Util;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactPublishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidationFailedException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.SchemaUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Pipeline step for publishing previously announced Eiffel artifacts, i.e. sending
 * an {@link EiffelArtifactPublishedEvent} that contains URIs to the Jenkins artifacts that correspond to
 * the files in the Eiffel artifact. The artifact creation events are picked up from the {@link Run}'s
 * {@link EiffelArtifactToPublishAction} actions, optionally added by {@link SendEiffelEventStep},
 * or from JSON files stored in the Run's workspace.
 */
public class PublishEiffelArtifactsStep extends Step {
    public static final String ERROR_MESSAGE_PREFIX = "Could not publish Eiffel event";

    /** An Ant-style glob expression that selects which file(s) to read {@link EiffelArtifactCreatedEvent} from. */
    private @CheckForNull String artifactEventFiles;

    @DataBoundConstructor
    public PublishEiffelArtifactsStep() { }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    public @CheckForNull String getArtifactEventFiles() {
        return artifactEventFiles;
    }

    @DataBoundSetter
    public void setArtifactEventFiles(@CheckForNull String artifactEventFiles) {
        this.artifactEventFiles = hudson.Util.fixEmptyAndTrim(artifactEventFiles);
    }

    private static class Execution extends SynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private final transient PublishEiffelArtifactsStep step;

        public Execution(@Nonnull PublishEiffelArtifactsStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            Run run = getContext().get(Run.class);
            EiffelActivityAction action = run.getAction(EiffelActivityAction.class);
            EiffelArtifactPublisher artifactPublisher = new EiffelArtifactPublisher(
                    action.getTriggerEvent(), Util.getRunUri(run), run.getArtifactManager().root());

            try {
                for (EiffelArtifactToPublishAction savedArtifact : run.getActions(EiffelArtifactToPublishAction.class)) {
                    publishArtifact(artifactPublisher, savedArtifact.getEvent());
                }

                if (step.getArtifactEventFiles() != null) {
                    for (FilePath file : getContext().get(FilePath.class).list(step.getArtifactEventFiles())) {
                        publishArtifactsFromFile(artifactPublisher, file);
                    }
                }
            } catch (EmptyArtifactException | EventValidationFailedException | JsonProcessingException
                    | MissingArtifactException | SchemaUnavailableException e) {
                throw new AbortException(String.format(
                        "%s (%s): %s", ERROR_MESSAGE_PREFIX, e.getClass().getSimpleName(), e.getMessage()));
            }
            return null;
        }

        private void publishArtifact(@Nonnull final EiffelArtifactPublisher artifactPublisher,
                                     @Nonnull final EiffelArtifactCreatedEvent creationEvent) throws Exception {
            EiffelArtifactPublishedEvent event = artifactPublisher.prepareEvent(creationEvent);
            JsonNode sentJSON = Util.mustPublishEvent(event);
            if (sentJSON != null) {
                getContext().get(TaskListener.class).getLogger().format(
                        "Successfully sent %s with id %s for artifact with id %s%n",
                        event.getMeta().getType(), event.getMeta().getId(),
                        creationEvent.getMeta().getId());
            }
        }

        private void publishArtifactsFromFile(@Nonnull final EiffelArtifactPublisher artifactPublisher,
                                              @Nonnull final FilePath file) throws Exception {
            getContext().get(TaskListener.class).getLogger().format("Reading events from %s%n", file.getRemote());
            try (InputStream is = file.read()) {
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    try (BufferedReader br = new BufferedReader(isr)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            EiffelEvent event = new ObjectMapper().readValue(line, EiffelEvent.class);
                            if (!(event instanceof EiffelArtifactCreatedEvent)) {
                                throw new AbortException(String.format(
                                        "%s: This event in %s was of the type %s but only " +
                                                "EiffelArtifactCreatedEvent is supported: %s",
                                        ERROR_MESSAGE_PREFIX, file.getRemote(), event.getMeta().getType(), line));
                            }
                            publishArtifact(artifactPublisher, (EiffelArtifactCreatedEvent) event);
                        }
                    }
                }
            }
        }
    }

    @Extension
    public static class Descriptor extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(FilePath.class, Run.class, TaskListener.class);
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Publishes previously announced Eiffel artifacts";
        }

        @Override
        public String getFunctionName() {
            return "publishEiffelArtifacts";
        }
    }
}
