/**
 The MIT License

 Copyright 2021-2024 Axis Communications AB.

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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.JobCreatingJenkinsRule;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.MQConnectionEventCaptureRule;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactPublishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class PublishEiffelArtifactsStepTest {
    @Rule
    public JobCreatingJenkinsRule jenkins = new JobCreatingJenkinsRule();

    @Rule
    public MQConnectionEventCaptureRule eventCapture = new MQConnectionEventCaptureRule();

    private List<String> getLocationNames(EventSet events) {
        return events.all(EiffelArtifactPublishedEvent.class).stream()
                .flatMap(event -> event.getData().getLocations().stream())
                .map(EiffelArtifactPublishedEvent.Data.Location::getName)
                .collect(Collectors.toList());
    }

    @Before
    public void setUp() {
        EiffelBroadcasterConfig.getInstance().setEnableBroadcaster(true);
    }

    @Test
    public void testSuccessful_PublishesArtifacts() throws Exception {
        var job = jenkins.createPipeline("successful_publish_artifact_step.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = eventCapture.getEvents();

        var run = job.getBuildByNumber(1);
        // We have pretty thorough tests of the ArtP contents for a given ArtC so here it's enough to
        // verify that we get the correct number of events and that the filenames in the events are what
        // we expect (so we're not getting two copies of the same event).
        assertThat(getLocationNames(events), containsInAnyOrder("a.txt", "b.txt"));
    }

    @Test
    public void testSuccessful_PublishesArtifactsFromFile() throws Exception {
        var job = jenkins.createPipeline("successful_publish_artifact_step_from_file.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = eventCapture.getEvents();

        var run = job.getBuildByNumber(1);
        // We have pretty thorough tests of the ArtP contents for a given ArtC so here it's enough to
        // verify that we get the correct number of events and that the filenames in the events are what
        // we expect (so we're not getting two copies of the same event).
        assertThat(getLocationNames(events), containsInAnyOrder("a.txt", "b.txt", "c.txt", "d.txt"));
    }

    @Test
    public void testSuccessful_ReturnsSentEvents() throws Exception {
        var job = jenkins.createPipeline("successful_publish_artifact_step_with_payloads_saved.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = eventCapture.getEvents();

        // This job uses the writeFile step to write the returned payloads to events.json.
        // Deserialize each line of that file and compare them against the events sent on the bus.
        var eventFile = jenkins.jenkins.getWorkspaceFor(job).child("events.json").readToString();
        var mapper = new ObjectMapper();
        var savedEvents = new ArrayList<EiffelEvent>();
        for (var line : eventFile.split("\\n")) {
            savedEvents.add(mapper.readValue(line, EiffelEvent.class));
        }
        assertThat(savedEvents, is(events.all(EiffelArtifactPublishedEvent.class)));
    }

    @Test
    public void testFailed_EventFromFileWithWrongType() throws Exception {
        var job = jenkins.createPipeline("failed_publish_artifact_step_from_file_bad_type.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                "was of the type EiffelCompositionDefinedEvent but only EiffelArtifactCreatedEvent is supported",
                job.getBuildByNumber(1));
    }
}
