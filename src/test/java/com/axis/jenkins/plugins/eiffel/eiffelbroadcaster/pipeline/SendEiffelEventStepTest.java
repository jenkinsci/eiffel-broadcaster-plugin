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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelBroadcasterConfig;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EventSet;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Mocks;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidationFailedException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.GenericEiffelEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Result;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SendEiffelEventStepTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private WorkflowJob createJob(String pipelineCodeResourceFile) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test");
        String pipelineCode = new String(
                Files.readAllBytes(Paths.get(getClass().getResource(pipelineCodeResourceFile).toURI())),
                StandardCharsets.UTF_8.name());
        job.setDefinition(new CpsFlowDefinition(pipelineCode, true));
        return job;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new Mocks.RabbitMQConnectionMock();
    }

    @Before
    public void setUp() {
        Mocks.messages.clear();
        EiffelBroadcasterConfig.getInstance().setEnableBroadcaster(true);
    }

    @Test
    public void testSuccessful_WithDefaultLinkType() throws Exception {
        WorkflowJob job = createJob("successful_send_event_step_with_default_linktype.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        GenericEiffelEvent cD = events.findNext(GenericEiffelEvent.class);
        assertThat(cD.getMeta().getType(), is("EiffelCompositionDefinedEvent"));
        assertThat(cD, linksTo(actT, EiffelEvent.Link.Type.CONTEXT));
    }

    @Test
    public void testSuccessful_LogsEventDetails() throws Exception {
        WorkflowJob job = createJob("successful_send_event_step_with_default_linktype.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        GenericEiffelEvent cD = events.findNext(GenericEiffelEvent.class);
        jenkins.assertLogContains(
                String.format("Successfully sent %s with id %s", cD.getMeta().getType(), cD.getMeta().getId()),
                job.getBuildByNumber(1));
    }

    @Test
    public void testSuccessful_WithCustomLinkType() throws Exception {
        WorkflowJob job = createJob("successful_send_event_step_with_custom_linktype.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        GenericEiffelEvent cD = events.findNext(GenericEiffelEvent.class);
        assertThat(cD.getMeta().getType(), is("EiffelCompositionDefinedEvent"));
        assertThat(cD, linksTo(actT, EiffelEvent.Link.Type.CAUSE));
    }

    @Test
    public void testSuccessful_WithoutLink() throws Exception {
        WorkflowJob job = createJob("successful_send_event_step_without_link.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        GenericEiffelEvent cD = events.findNext(GenericEiffelEvent.class);
        assertThat(cD.getLinks(), hasSize(0));
    }

    @Test
    public void testSuccessful_ReturnsSentMessage() throws Exception {
        WorkflowJob job = createJob("successful_send_event_step_with_payload_saved.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        // This job uses the writeJSON step to write the returned payload to event.json.
        // Deserialize that file and compare it against the event sent on the bus.
        EiffelEvent eventWrittenToWorkspace = new ObjectMapper().readValue(
                jenkins.jenkins.getWorkspaceFor(job).child("event.json").readToString(), EiffelEvent.class);
        GenericEiffelEvent publishedEvent = events.findNext(GenericEiffelEvent.class);
        assertThat(publishedEvent, is(eventWrittenToWorkspace));
    }

    @Test
    public void testFailed_EventValidationError() throws Exception {
        WorkflowJob job = createJob("failed_send_event_step_event_validation_error.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s (%s)",
                        SendEiffelEventStep.ERROR_MESSAGE_PREFIX,
                        EventValidationFailedException.class.getSimpleName()),
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailed_EventWithoutType() throws Exception {
        WorkflowJob job = createJob("failed_send_event_step_event_without_type.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s (%s)",
                        SendEiffelEventStep.ERROR_MESSAGE_PREFIX,
                        IllegalArgumentException.class.getSimpleName()),
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailed_EventWithInvalidLinkType() throws Exception {
        WorkflowJob job = createJob("failed_send_event_step_event_with_invalid_linktype.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s: %s",
                        IllegalArgumentException.class.getSimpleName(), "The activity link type must be one of"),
                job.getBuildByNumber(1));
    }
}
