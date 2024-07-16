// The following code is copied from https://github.com/jenkinsci/pipeline-build-step-plugin

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline.build.regression;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline.build.BuildWithEiffelStep;
import org.htmlunit.AlertHandler;
import org.htmlunit.Page;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlTextInput;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.support.steps.build.BuildTriggerStep;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

;

/**
 * Test that {@link BuildWithEiffelStep} has not broken any functionality in {@link BuildTriggerStep}.
 */
public class BuildWithEiffelStepConfigTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue("JENKINS-26692")
    @Test
    public void configRoundTrip() throws Exception {
        BuildWithEiffelStep s = new BuildWithEiffelStep("ds");
        s = new StepConfigTester(r).configRoundTrip(s);
        assertNull(s.getQuietPeriod());
        assertTrue(s.isPropagate());
        s.setPropagate(false);
        s.setQuietPeriod(5);
        s = new StepConfigTester(r).configRoundTrip(s);
        assertEquals(Integer.valueOf(5), s.getQuietPeriod());
        assertFalse(s.isPropagate());
        s.setQuietPeriod(0);
        s = new StepConfigTester(r).configRoundTrip(s);
        assertEquals(Integer.valueOf(0), s.getQuietPeriod());
    }

    @Issue("JENKINS-38114")
    @Test public void helpWait() throws Exception {
        assertThat(r.createWebClient().goTo(r.executeOnServer(()
                                -> r.jenkins.getDescriptorByType(BuildWithEiffelStep.DescriptorImpl.class).getHelpFile("wait"))
                        .replaceFirst("^/", ""), /* TODO why is no content type set? */null)
                .getWebResponse().getContentAsString(), containsString("<dt><code>buildVariables</code></dt>"));
    }

    @Test @Issue("SECURITY-3019")
    public void escapedSnippetConfig() throws IOException, SAXException {
        final String jobName = "'+alert(123)+'";
        WorkflowJob j = r.createProject(WorkflowJob.class, jobName);
        try (JenkinsRule.WebClient webClient = r.createWebClient()) {
            Alerter alerter = new Alerter();
            webClient.setAlertHandler(alerter);
            HtmlPage page = webClient.getPage(j, "pipeline-syntax");
            final HtmlSelect select = (HtmlSelect)page.getElementsByTagName("select").get(0);
            page = select.setSelectedAttribute("buildWithEiffel: Build a job with custom Eiffel activity name", true);
            webClient.waitForBackgroundJavaScript(2000);
            //final HtmlForm config = page.getFormByName("config");
            final List<DomElement> inputs = page.getElementsByName("_.job"); //config.getInputsByName("_.job");
            assertThat(inputs, hasSize(1));
            final HtmlTextInput jobNameInput = (HtmlTextInput)inputs.get(0);
            jobNameInput.focus();
            jobNameInput.blur();
            assertThat(alerter.messages, empty()); //Fails before the fix
        }

    }

    static class Alerter implements AlertHandler {
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        @Override
        public void handleAlert(final Page page, final String message) {
            messages.add(message);
        }
    }

}