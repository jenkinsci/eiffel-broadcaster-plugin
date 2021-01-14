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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityCanceledEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityStartedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BuildTrigger;
import java.util.Collections;
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

/**
 * Tests of the correctness of the events emitted from all listeners.
 * For now we only check that we get the correct number of events,
 * i.e. the contents of the actual events is never inspected.
 */
public class EmittedEventsTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

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
    public void testEventsForSuccessfulFreestyleBuild() throws Exception {
        FreeStyleProject job = jenkins.createFolder("testfolder")
                .createProject(FreeStyleProject.class, "test");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0, new Cause.UserIdCause()).get());

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(actT.getData().getName(), is("testfolder/test"));
        assertThat(actT,
                hasTrigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER, "SYSTEM"));
        assertThat(actT.getData().getCategories(), is(empty()));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class);
        assertThat(actS.getData().getExecutionUri().getPath(), containsString("testfolder/job/test/1"));
        assertThat(actS.getData().getLiveLogs(), not(emptyIterable()));
        EiffelActivityStartedEvent.Data.LiveLogs liveLog = actS.getData().getLiveLogs().get(0);
        assertThat(liveLog.getName(), is(RunListenerImpl.CONSOLE_LOG_NAME));
        assertThat(liveLog.getURI().getPath(), containsString("testfolder/job/test/1/consoleText"));
        assertThat(actS, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class);
        assertThat(actF.getData().getOutcome().getConclusion(),
                is(EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL));
        assertThat(actF.getData().getPersistentLogs(), not(emptyIterable()));
        EiffelActivityFinishedEvent.Data.PersistentLogs persistentLog = actF.getData().getPersistentLogs().get(0);
        assertThat(persistentLog.getName(), is(RunListenerImpl.CONSOLE_LOG_NAME));
        assertThat(persistentLog.getURI().getPath(), containsString("testfolder/job/test/1/consoleText"));
        assertThat(actF, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForSuccessfulFreestyleBuildSequence() throws Exception {
        FreeStyleProject downstreamJob = jenkins.createProject(FreeStyleProject.class, "downstream");

        FreeStyleProject upstreamJob = jenkins.createProject(FreeStyleProject.class, "upstream");
        upstreamJob.getPublishersList().add(new BuildTrigger(Collections.singletonList(downstreamJob), Result.SUCCESS));

        jenkins.jenkins.rebuildDependencyGraph();
        upstreamJob.scheduleBuild2(0);
        jenkins.waitUntilNoActivity();

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent upstreamActT = events.findNext(EiffelActivityTriggeredEvent.class);
        // Ignore the contents of the ActT event and the subsequent ActS and ActF events for
        // the upstream job. We're verifying them in other test cases and this test cases focuses
        // on trigger information for the downstream job's events.
        events.findNext(EiffelActivityStartedEvent.class);
        events.findNext(EiffelActivityFinishedEvent.class);

        EiffelActivityTriggeredEvent downstreamActT = events.findNext(EiffelActivityTriggeredEvent.class);
        assertThat(downstreamActT,
                hasTrigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT, "upstream"));
        assertThat(downstreamActT, linksTo(upstreamActT, EiffelEvent.Link.Type.CAUSE));

        events.findNext(EiffelActivityStartedEvent.class);
        events.findNext(EiffelActivityFinishedEvent.class);

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForCanceledFreestyleBuild() throws Exception {
        // Attempt to allocate a node with a label expression that won't ever be satisfied,
        // forcing it to end up in the queue so that we have time to cancel it.
        FreeStyleProject job = jenkins.createFolder("testfolder")
                .createProject(FreeStyleProject.class, "test");
        job.setAssignedLabel(jenkins.jenkins.getLabel("no-such-node"));

        // Unfortunately the QueueTaskFuture returned by schedulebuild2() only allows us to
        // wait for the start or completion of the build (neither of which will ever happen),
        // so to access the queued-up build and cancel it we have to locate it in the queue.
        // But our goal is just to cancel it so we might as well clear the whole queue.
        job.scheduleBuild2(0);
        jenkins.jenkins.getQueue().clear();

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        // Ignore the contents of the ActT event; we're verifying its contents elsewhere.

        EiffelActivityCanceledEvent actC = events.findNext(EiffelActivityCanceledEvent.class);
        assertThat(actC, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForSuccessfulForMatrixBuild() throws Exception {
        MatrixProject job = jenkins.createFolder("testfolder")
                .createProject(MatrixProject.class, "test");
        // Using more than one value on the axis would theoretically cover more cases,
        // but we'd have to spend more effort evaluating the resulting events.
        String axisValue = "value1";
        job.setAxes(new AxisList(new Axis("axislabel", axisValue)));
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0, new Cause.UserIdCause()).get());

        EventSet events = new EventSet(Mocks.messages);

        // First check the ActT/ActS/ActF sequence of the top-level build.
        EiffelActivityTriggeredEvent toplevelActT = events.findNext(EiffelActivityTriggeredEvent.class);
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(toplevelActT.getData().getName(), is("testfolder/test"));
        assertThat(toplevelActT,
                hasTrigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER, "SYSTEM"));
        assertThat(toplevelActT.getData().getCategories(), is(empty()));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class,
                linksTo(toplevelActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        // Ignore the data members of the ActS event; we're verifying them elsewhere.

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class,
                linksTo(toplevelActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        // Ignore the data members of the ActF event; we're verifying them elsewhere.

        // Check that we get a child activity that has a CAUSE link to the main build's ActT event.
        EiffelActivityTriggeredEvent childActT = events.findNext(EiffelActivityTriggeredEvent.class);
        // To avoid tight coupling to the exact build name let's just perform a sanity check
        // to make sure it contains the job name and the matrix axis value.
        assertThat(childActT.getData().getName(), containsString("test"));
        assertThat(childActT.getData().getName(), containsString(axisValue));
        assertThat(childActT,
                hasTrigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT, "upstream"));
        assertThat(childActT, linksTo(toplevelActT, EiffelEvent.Link.Type.CAUSE));

        events.findNext(EiffelActivityStartedEvent.class,
                linksTo(childActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        // Ignore the data members of the ActS event; we're verifying them elsewhere.

        EiffelActivityFinishedEvent childActF = events.findNext(EiffelActivityFinishedEvent.class,
                linksTo(childActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        // Ignore the contents of the ActF event; we're verifying its contents elsewhere.

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForSuccessfulPipelineBuild() throws Exception {
        WorkflowJob job = jenkins.createFolder("testfolder")
                .createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition("node { echo 'hello' }", true));
        jenkins.assertBuildStatus(Result.SUCCESS,
                job.scheduleBuild2(0, new CauseAction(new Cause.UserIdCause())).get());

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(actT.getData().getName(), is("testfolder/test"));
        assertThat(actT,
                hasTrigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.OTHER, "SYSTEM"));
        assertThat(actT.getData().getCategories(), is(empty()));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class);
        // Ignore the data members of the ActS event; we're verifying them elsewhere.
        assertThat(actS, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class);
        assertThat(actF.getData().getOutcome().getConclusion(),
                is(EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL));
        assertThat(actF.getData().getPersistentLogs(), not(emptyIterable()));
        EiffelActivityFinishedEvent.Data.PersistentLogs persistentLog = actF.getData().getPersistentLogs().get(0);
        assertThat(persistentLog.getName(), is(RunListenerImpl.CONSOLE_LOG_NAME));
        assertThat(persistentLog.getURI().getPath(), containsString("testfolder/job/test/1/consoleText"));
        assertThat(actF, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForSuccessfulPipelineBuildSequence() throws Exception {
        WorkflowJob downstreamJob = jenkins.createProject(WorkflowJob.class, "downstream");
        downstreamJob.setDefinition(new CpsFlowDefinition("echo 'hello'", true));

        WorkflowJob upstreamJob = jenkins.createProject(WorkflowJob.class, "upstream");
        String upstreamPipelineCode = String.format("build '%s'", downstreamJob.getFullName());
        upstreamJob.setDefinition(new CpsFlowDefinition(upstreamPipelineCode, true));

        upstreamJob.scheduleBuild2(0);
        jenkins.waitUntilNoActivity();

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent upstreamActT = events.findNext(EiffelActivityTriggeredEvent.class);
        // Ignore the contents of the ActT event and the subsequent ActS and ActF events for
        // the upstream job. We're verifying them in other test cases and this test case focuses
        // on trigger information for the downstream job's events.
        events.findNext(EiffelActivityStartedEvent.class);
        events.findNext(EiffelActivityFinishedEvent.class);

        EiffelActivityTriggeredEvent downstreamActT = events.findNext(EiffelActivityTriggeredEvent.class);
        assertThat(downstreamActT,
                hasTrigger(EiffelActivityTriggeredEvent.Data.Trigger.Type.EIFFEL_EVENT, "upstream"));
        assertThat(downstreamActT, linksTo(upstreamActT, EiffelEvent.Link.Type.CAUSE));

        events.findNext(EiffelActivityStartedEvent.class);
        events.findNext(EiffelActivityFinishedEvent.class);

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForFailingPipelineBuild() throws Exception {
        WorkflowJob job = jenkins.createFolder("testfolder")
                .createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition("node { error 'something went bad' }", true));
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        // Ignore the contents of the ActT event; we're verifying its contents elsewhere.

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class);
        // Ignore the data members of the ActS event; we're verifying them elsewhere.
        assertThat(actS, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class);
        assertThat(actF.getData().getOutcome().getConclusion(),
                is(EiffelActivityFinishedEvent.Data.Outcome.Conclusion.FAILED));
        assertThat(actF.getData().getPersistentLogs(), not(emptyIterable()));
        EiffelActivityFinishedEvent.Data.PersistentLogs persistentLog = actF.getData().getPersistentLogs().get(0);
        assertThat(persistentLog.getName(), is(RunListenerImpl.CONSOLE_LOG_NAME));
        assertThat(persistentLog.getURI().getPath(), containsString("testfolder/job/test/1/consoleText"));
        assertThat(actF, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        assertThat(events.isEmpty(), is(true));
    }

    // Here we should have a test that verifies that an ActC event with a correct link
    // to the ActT event of a pipeline build is emitted, but because of the bug and race condition
    // described in https://github.com/jenkinsci/eiffel-broadcaster-plugin/issues/18 it's not
    // realistic to write such a test right now (not even one that expects a failure since there's
    // more than one possible failure).
}
