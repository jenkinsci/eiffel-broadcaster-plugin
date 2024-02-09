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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelActivityAction;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelBroadcasterConfig;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Mocks;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class BuildWithEiffelStepTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private WorkflowJob createJob(String pipelineCodeResourceFile, String name) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, name);
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
    public void testSuccessful_customActivityName() throws Exception {
        WorkflowJob upstreamJob = createJob("success_build_with_eiffel_step.groovy", "upstream");
        WorkflowJob downstreamJob = createJob("triggered_build.groovy", "downstream");
        jenkins.assertBuildStatus(Result.SUCCESS, upstreamJob.scheduleBuild2(0));
        jenkins.assertBuildStatus(Result.SUCCESS, downstreamJob.getBuildByNumber(1));

        WorkflowRun run = downstreamJob.getBuildByNumber(1);
        EiffelActivityTriggeredEvent actT = run.getAction(EiffelActivityAction.class).getTriggerEvent();
        assertThat(actT.getData().getName(), is("my_custom_activity"));
    }

    @Test
    public void testFailure_activityNameMissing() throws Exception {
        WorkflowJob job = createJob("failed_build_with_eiffel_step_empty_name.groovy", "upstream");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s: %s",
                        IllegalArgumentException.class.getSimpleName(), "The activity name must not be empty"),
                job.getBuildByNumber(1));
    }

}
