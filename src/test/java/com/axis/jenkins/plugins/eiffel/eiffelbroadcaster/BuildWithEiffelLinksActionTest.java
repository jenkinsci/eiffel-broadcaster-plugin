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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.util.NameValuePair;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Matchers.hasBuildParameter;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class BuildWithEiffelLinksActionTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public WebResponse postBuildRequest(final Job job, final String jsonPayload) throws IOException {
        JenkinsRule.WebClient wc = jenkins.createWebClient();
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
        URL url = wc.createCrumbedUrl(job.getUrl() + BuildWithEiffelLinksAction.URL_SUFFIX + "/build");
        WebRequest req = new WebRequest(url, HttpMethod.POST);
        if (jsonPayload != null) {
            req.setRequestParameters(Arrays.asList(new NameValuePair("json", jsonPayload)));
        }
        return wc.getPage(req).getWebResponse();
    }

    @Test
    public void testNonParameterizedFreestyleBuild_WithoutParams() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");

        WebResponse resp = postBuildRequest(job, new ObjectMapper().writeValueAsString(reqParams));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_CREATED));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(notNullValue()));

        EiffelCause cause = (EiffelCause) build.getCause(EiffelCause.class);
        assertThat(cause, is(notNullValue()));
        assertThat(cause.getLinks(), is(reqParams.links));

        ParametersAction paramAction = build.getAction(ParametersAction.class);
        assertThat(paramAction, is(nullValue()));
    }

    @Test
    public void testNonParameterizedFreestyleBuild_WithoutParams_WithoutLinks() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();
        // Clear the links that are added by the constructor. The empty list
        // will be omitted when the object is serialized to JSON.
        reqParams.links.clear();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");

        WebResponse resp = postBuildRequest(job, new ObjectMapper().writeValueAsString(reqParams));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_BAD_REQUEST));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(nullValue()));
    }

    @Test
    public void testParameterizedFreestyleBuild_WithParams() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");
        StringParameterDefinition stringParam = new StringParameterDefinition("STRING_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(stringParam));
        reqParams.buildParams.add(new NameValuePair(stringParam.getName(), "overridden value"));

        WebResponse resp = postBuildRequest(job, new ObjectMapper().writeValueAsString(reqParams));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_CREATED));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(notNullValue()));

        EiffelCause cause = (EiffelCause) build.getCause(EiffelCause.class);
        assertThat(cause, is(notNullValue()));
        assertThat(cause.getLinks(), is(reqParams.links));

        assertThat(build, hasBuildParameter(stringParam.getName(), "overridden value"));
    }

    @Test
    public void testParameterizedWorkflowBuild_WithParams() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition("node { echo 'hello' }", true));
        StringParameterDefinition stringParam = new StringParameterDefinition("STRING_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(stringParam));
        reqParams.buildParams.add(new NameValuePair(stringParam.getName(), "overridden value"));

        WebResponse resp = postBuildRequest(job, new ObjectMapper().writeValueAsString(reqParams));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_CREATED));

        WorkflowRun build = job.getBuildByNumber(1);
        assertThat(build, is(notNullValue()));

        EiffelCause cause = (EiffelCause) build.getCause(EiffelCause.class);
        assertThat(cause, is(notNullValue()));
        assertThat(cause.getLinks(), is(reqParams.links));

        assertThat(build, hasBuildParameter(stringParam.getName(), "overridden value"));
    }

    @Test
    public void testParameterizedFreestyleBuild_WithBadJsonPayload() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");
        StringParameterDefinition stringParam = new StringParameterDefinition("STRING_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(stringParam));

        WebResponse resp = postBuildRequest(job, "this is not valid JSON");
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_BAD_REQUEST));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(nullValue()));
    }

    @Test
    public void testParameterizedFreestyleBuild_WithBadParameterJson() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");
        StringParameterDefinition stringParam = new StringParameterDefinition("STRING_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(stringParam));
        reqParams.buildParams.add(new NameValuePair(stringParam.getName(), "overridden value"));

        WebResponse resp = postBuildRequest(job, String.format(
                "{\"%s\": [], \"%s\": [[\"this can't be deserialized\"]]}",
                BuildWithEiffelLinksAction.FORM_PARAM_EIFFELLINKS,
                BuildWithEiffelLinksAction.FORM_PARAM_PARAMETERS));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_BAD_REQUEST));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(nullValue()));
    }

    @Test
    public void testParameterizedFreestyleBuild_WithBadEiffelLinksJson() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");
        StringParameterDefinition stringParam = new StringParameterDefinition("STRING_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(stringParam));
        reqParams.buildParams.add(new NameValuePair(stringParam.getName(), "overridden value"));

        WebResponse resp = postBuildRequest(job, String.format(
                "{\"%s\": [[\"this can't be deserialized\"]]}",
                BuildWithEiffelLinksAction.FORM_PARAM_EIFFELLINKS));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_BAD_REQUEST));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(nullValue()));
    }

    @Test
    public void testParameterizedFreestyleBuild_WithoutParams() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");
        StringParameterDefinition stringParam = new StringParameterDefinition("STRING_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(stringParam));

        WebResponse resp = postBuildRequest(job, new ObjectMapper().writeValueAsString(reqParams));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_CREATED));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(notNullValue()));

        EiffelCause cause = (EiffelCause) build.getCause(EiffelCause.class);
        assertThat(cause, is(notNullValue()));
        assertThat(cause.getLinks(), is(reqParams.links));

        assertThat(build, hasBuildParameter(stringParam.getName(), "value"));
    }

    @Test
    public void testParameterizedFreestyleBuild_WithParamSubset() throws Exception {
        BuildRequestParams reqParams = new BuildRequestParams();

        // Define two parameters, GIVEN_PARAM and EXTRA_PARAM, but include only the former in the build request.
        FreeStyleProject job = jenkins.createProject(FreeStyleProject.class, "test");
        StringParameterDefinition givenParam = new StringParameterDefinition("GIVEN_PARAM", "value");
        reqParams.buildParams.add(new NameValuePair(givenParam.getName(), "overridden value"));
        StringParameterDefinition extraParam = new StringParameterDefinition("EXTRA_PARAM", "value");
        job.addProperty(new ParametersDefinitionProperty(givenParam, extraParam));

        WebResponse resp = postBuildRequest(job, new ObjectMapper().writeValueAsString(reqParams));
        jenkins.waitUntilNoActivity();

        assertThat(resp.getStatusCode(), is(SC_CREATED));

        AbstractBuild build = job.getBuildByNumber(1);
        assertThat(build, is(notNullValue()));

        EiffelCause cause = (EiffelCause) build.getCause(EiffelCause.class);
        assertThat(cause, is(notNullValue()));
        assertThat(cause.getLinks(), is(reqParams.links));

        assertThat(build, hasBuildParameter(givenParam.getName(), "overridden value"));
        assertThat(build, hasBuildParameter(extraParam.getName(), "value"));
    }

    /**
     * Helper to construct the JSON object posted in the "json" form parameter. By default populated
     * with a few Eiffel links.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class BuildRequestParams {
        @JsonProperty(BuildWithEiffelLinksAction.FORM_PARAM_EIFFELLINKS)
        public final List<EiffelEvent.Link> links = new ArrayList<>();

        @JsonProperty(BuildWithEiffelLinksAction.FORM_PARAM_PARAMETERS)
        public final List<NameValuePair> buildParams = new ArrayList<>();

        BuildRequestParams() {
            links.add(new EiffelEvent.Link(EiffelEvent.Link.Type.CAUSE,
                    new EiffelActivityTriggeredEvent("activity name").getMeta().getId()));
            links.add(new EiffelEvent.Link(EiffelEvent.Link.Type.CONTEXT,
                    new EiffelActivityTriggeredEvent("activity name").getMeta().getId()));
        }
    }
}
