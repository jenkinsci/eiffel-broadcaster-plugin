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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.RestoreSourceProviderJenkinsRule;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactPublishedEvent;
import hudson.model.Result;
import hudson.model.Run;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PublishEiffelArtifactsStepTest {
    @Rule
    public RestoreSourceProviderJenkinsRule jenkins = new RestoreSourceProviderJenkinsRule();

    private WorkflowJob createJob(String pipelineCodeResourceFile) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test");
        String pipelineCode = new String(
                Files.readAllBytes(Paths.get(getClass().getResource(pipelineCodeResourceFile).toURI())),
                StandardCharsets.UTF_8.name());
        job.setDefinition(new CpsFlowDefinition(pipelineCode, true));
        return job;
    }

    private List<String> getLocationNames(EventSet events) {
        return events.all(EiffelArtifactPublishedEvent.class).stream()
                .flatMap(event -> event.getData().getLocations().stream())
                .map(EiffelArtifactPublishedEvent.Data.Location::getName)
                .collect(Collectors.toList());
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
    public void testSuccessful_PublishesArtifacts() throws Exception {
        WorkflowJob job = createJob("successful_publish_artifact_step.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        Run run = job.getBuildByNumber(1);
        // We have pretty thorough tests of the ArtP contents for a given ArtC so here it's enough to
        // verify that we get the correct number of events and that the filenames in the events are what
        // we expect (so we're not getting two copies of the same event).
        assertThat(getLocationNames(events), containsInAnyOrder("a.txt", "b.txt"));
    }

    @Test
    public void testSuccessful_PublishesArtifactsFromFile() throws Exception {
        WorkflowJob job = createJob("successful_publish_artifact_step_from_file.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        Run run = job.getBuildByNumber(1);
        // We have pretty thorough tests of the ArtP contents for a given ArtC so here it's enough to
        // verify that we get the correct number of events and that the filenames in the events are what
        // we expect (so we're not getting two copies of the same event).
        assertThat(getLocationNames(events), containsInAnyOrder("a.txt", "b.txt", "c.txt", "d.txt"));
    }


    @Test
    public void testFailed_EventFromFileWithWrongType() throws Exception {
        WorkflowJob job = createJob("failed_publish_artifact_step_from_file_bad_type.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                "was of the type EiffelCompositionDefinedEvent but only EiffelArtifactCreatedEvent is supported",
                job.getBuildByNumber(1));
    }
}
