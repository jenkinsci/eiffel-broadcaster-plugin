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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class EiffelEnvironmentContributorTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new Mocks.RabbitMQConnectionMock();
    }

    @Test
    public void testEnvironmentVariablesAreAvailable_FreeStyle() throws Exception {
        FreeStyleProject job = jenkins.createFreeStyleProject("test");
        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(capture);
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        FreeStyleBuild run = job.getBuildByNumber(1);
        EiffelActivityAction activityAction = run.getAction(EiffelActivityAction.class);
        assertThat(activityAction, is(notNullValue()));

        assertThat(capture.getEnvVars().get(EiffelEnvironmentContributor.ACTIVITY_STARTED),
                is(activityAction.getStartedEventJSON()));
        assertThat(capture.getEnvVars().get(EiffelEnvironmentContributor.ACTIVITY_TRIGGERED),
                is(activityAction.getTriggerEventJSON()));
    }

    @Test
    public void testEnvironmentVariablesAreAvailable_Pipeline() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test");
        String pipelineCode = String.format(
                "node {" +
                        "  if (isUnix()) {" +
                        "    sh('echo STARTED=$%s ; echo TRIGGER=$%s')" +
                        "  } else {" +
                        "    bat('echo STARTED=%%%s%% ; echo TRIGGER=%%%s%%')" +
                        "  }" +
                        "}",
                EiffelEnvironmentContributor.ACTIVITY_STARTED,
                EiffelEnvironmentContributor.ACTIVITY_TRIGGERED,
                EiffelEnvironmentContributor.ACTIVITY_STARTED,
                EiffelEnvironmentContributor.ACTIVITY_TRIGGERED);
        job.setDefinition(new CpsFlowDefinition(pipelineCode, true));
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        WorkflowRun run = job.getBuildByNumber(1);
        EiffelActivityAction activityAction = run.getAction(EiffelActivityAction.class);
        assertThat(activityAction, is(notNullValue()));

        jenkins.assertLogContains(String.format("STARTED=%s", activityAction.getStartedEventJSON()), run);
        jenkins.assertLogContains(String.format("TRIGGER=%s", activityAction.getTriggerEventJSON()), run);
    }
}
