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
import hudson.model.FreeStyleProject;
import hudson.model.Result;
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
        jenkins.buildAndAssertSuccess(job);

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(actT.getData(), is(actTData));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class);
        EiffelActivityStartedEvent.Data actSData = new EiffelActivityStartedEvent.Data();
        assertThat(actS.getData(), is(actSData));
        assertThat(actS, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class);
        EiffelActivityFinishedEvent.Data actFData = new EiffelActivityFinishedEvent.Data(
                new EiffelActivityFinishedEvent.Data.Outcome(
                        EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL));
        assertThat(actF.getData(), is(actFData));
        assertThat(actF, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

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
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(actT.getData(), is(actTData));

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
        jenkins.buildAndAssertSuccess(job);

        EventSet events = new EventSet(Mocks.messages);

        // First check the ActT/ActS/ActF sequence of the top-level build.
        EiffelActivityTriggeredEvent toplevelActT = events.findNext(EiffelActivityTriggeredEvent.class);
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(toplevelActT.getData(), is(actTData));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class,
                linksTo(toplevelActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        EiffelActivityStartedEvent.Data actSData = new EiffelActivityStartedEvent.Data();
        assertThat(actS.getData(), is(actSData));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class,
                linksTo(toplevelActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        EiffelActivityFinishedEvent.Data actFData = new EiffelActivityFinishedEvent.Data(
                new EiffelActivityFinishedEvent.Data.Outcome(
                        EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL));
        assertThat(actF.getData(), is(actFData));

        // Check that we get a child activity. Eventually it should have a CAUSE link
        // pointing to the top-level activity.
        EiffelActivityTriggeredEvent childActT = events.findNext(EiffelActivityTriggeredEvent.class);
        // To avoid tight coupling to the exact build name let's just perform a sanity check
        // to make sure it contains the job name and the matrix axis name.
        assertThat(childActT.getData().getName(), containsString("test"));
        assertThat(childActT.getData().getName(), containsString(axisValue));

        EiffelActivityStartedEvent childActS = events.findNext(EiffelActivityStartedEvent.class,
                linksTo(childActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        EiffelActivityStartedEvent.Data childActSData = new EiffelActivityStartedEvent.Data();
        assertThat(childActS.getData(), is(childActSData));

        EiffelActivityFinishedEvent childActF = events.findNext(EiffelActivityFinishedEvent.class,
                linksTo(childActT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));
        EiffelActivityFinishedEvent.Data childActFData = new EiffelActivityFinishedEvent.Data(
                new EiffelActivityFinishedEvent.Data.Outcome(
                        EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL));
        assertThat(childActF.getData(), is(childActFData));

        assertThat(events.isEmpty(), is(true));
    }

    @Test
    public void testEventsForSuccessfulPipelineBuild() throws Exception {
        WorkflowJob job = jenkins.createFolder("testfolder")
                .createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition("node { echo 'hello' }", true));
        jenkins.buildAndAssertSuccess(job);

        EventSet events = new EventSet(Mocks.messages);

        EiffelActivityTriggeredEvent actT = events.findNext(EiffelActivityTriggeredEvent.class);
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(actT.getData(), is(actTData));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class);
        EiffelActivityStartedEvent.Data actSData = new EiffelActivityStartedEvent.Data();
        assertThat(actS.getData(), is(actSData));
        assertThat(actS, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class);
        EiffelActivityFinishedEvent.Data actFData = new EiffelActivityFinishedEvent.Data(
                new EiffelActivityFinishedEvent.Data.Outcome(
                        EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL));
        assertThat(actF.getData(), is(actFData));
        assertThat(actF, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

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
        EiffelActivityTriggeredEvent.Data actTData = new EiffelActivityTriggeredEvent.Data("testfolder/test");
        assertThat(actT.getData(), is(actTData));

        EiffelActivityStartedEvent actS = events.findNext(EiffelActivityStartedEvent.class);
        EiffelActivityStartedEvent.Data actSData = new EiffelActivityStartedEvent.Data();
        assertThat(actS.getData(), is(actSData));
        assertThat(actS, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        EiffelActivityFinishedEvent actF = events.findNext(EiffelActivityFinishedEvent.class);
        EiffelActivityFinishedEvent.Data actFData = new EiffelActivityFinishedEvent.Data(
                new EiffelActivityFinishedEvent.Data.Outcome(
                        EiffelActivityFinishedEvent.Data.Outcome.Conclusion.FAILED));
        assertThat(actF.getData(), is(actFData));
        assertThat(actF, linksTo(actT, EiffelEvent.Link.Type.ACTIVITY_EXECUTION));

        assertThat(events.isEmpty(), is(true));
    }

    // Here we should have a test that verifies that an ActC event with a correct link
    // to the ActT event of a pipeline build is emitted, but because of the bug and race condition
    // described in https://github.com/jenkinsci/eiffel-broadcaster-plugin/issues/18 it's not
    // realistic to write such a test right now (not even one that expects a failure since there's
    // more than one possible failure).
}
