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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TopLevelItemDescriptor;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import jenkins.model.Jenkins;
import jenkins.model.ModifiableTopLevelItemGroup;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * A {@link JenkinsRule} specialization that contains a few convenience methods
 * for creating jobs with a specific configuraiton.
 */
public class JobCreatingJenkinsRule extends JenkinsRule {
    /**
     * Create a pipeline job in a particular location and preload it with a pipeline
     * definition from a given resource file.
     *
     * @param itemGroup the location of the new job
     * @param sourceFile the filename of the resource file, relative to the test class,
     *                   from which to load the pipeline definition
     * @return the newly created job
     * @throws IOException when the resource can't be loaded
     * @throws URISyntaxException when the path to the resource can't be turned into a URL
     */
    public WorkflowJob createPipeline(@NonNull final ModifiableTopLevelItemGroup itemGroup,
                                      @NonNull final String sourceFile)
            throws IOException, URISyntaxException {
        var job = (WorkflowJob) itemGroup.createProject(
                (TopLevelItemDescriptor) Jenkins.get().getDescriptor(WorkflowJob.class), "test", true);
        var pipelineCode = Files.readString(
                Paths.get(getTestDescription().getTestClass().getResource(sourceFile).toURI()));
        job.setDefinition(new CpsFlowDefinition(pipelineCode, true));
        return job;
    }

    /**
     * Create a pipeline job and preload it with a pipeline definition from a given resource file.
     *
     * @param sourceFile the filename of the resource file, relative to the test class,
     *                   from which to load the pipeline definition
     * @return the newly created job
     * @throws IOException when the resource can't be loaded
     * @throws URISyntaxException when the path to the resource can't be turned into a URL
     */
    public WorkflowJob createPipeline(@NonNull final String sourceFile)
            throws IOException, URISyntaxException {
        return createPipeline(Jenkins.get(), sourceFile);
    }
}
