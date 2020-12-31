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

import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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
    public void testExpectedMessageCountForFreestyleBuild() throws Exception {
        FreeStyleProject job = jenkins.createFolder("testfolder")
                .createProject(FreeStyleProject.class, "test");
        jenkins.buildAndAssertSuccess(job);

        assertThat(Mocks.messages.size(), is(3));
    }

    @Test
    public void testExpectedMessageCountForMatrixBuild() throws Exception {
        MatrixProject job = jenkins.createFolder("testfolder")
                .createProject(MatrixProject.class, "test");
        job.setAxes(new AxisList(new Axis("axislabel", "value1", "value2")));
        jenkins.buildAndAssertSuccess(job);

        // ActT/ActS/ActF for the top-level build and builds for a two-by-one matrix results in
        // 3 * (1 + 2 * 1) = 9 events.
        assertThat(Mocks.messages.size(), is(9));
    }

    @Test
    public void testExpectedMessageCountForPipelineBuild() throws Exception {
        WorkflowJob job = jenkins.createFolder("testfolder")
                .createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition("node { echo 'hello' }", true));
        jenkins.buildAndAssertSuccess(job);

        assertThat(Mocks.messages.size(), is(4));
    }
}
