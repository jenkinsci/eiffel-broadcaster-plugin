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

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CreatePackageURLStepTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private WorkflowJob createJob(String purlCreationSnippet) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition(
                String.format("node { def purl = %s\necho \"PURL=${purl}\" }", purlCreationSnippet),
                true));
        return job;
    }

    @Test
    public void testReturnsCorrectPurl() throws Exception {
        var job = createJob(
                "createPackageURL type: 'generic', namespace: 'name/space', " +
                        "name: 'pkgname', version: '1.0', subpath: 'some/path', qualifiers: [a: 'b']");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                "PURL=pkg:generic/name/space/pkgname@1.0?a=b#some/path",
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailsOnMissingRequiredParam() throws Exception {
        // Leaving out the required "name" argument
        var job = createJob("createPackageURL type: 'generic'");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains("Error creating package URL", job.getBuildByNumber(1));
        jenkins.assertLogContains("The PackageURL name specified is invalid", job.getBuildByNumber(1));
    }
}
