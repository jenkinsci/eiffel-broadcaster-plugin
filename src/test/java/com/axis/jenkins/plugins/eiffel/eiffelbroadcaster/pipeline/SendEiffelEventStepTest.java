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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelArtifactToPublishAction;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelBroadcasterConfig;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EventSet;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.JobCreatingJenkinsRule;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Mocks;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidationFailedException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.GenericEiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.signing.TestKeyStore;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Result;
import java.io.IOException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Matchers.linksTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class SendEiffelEventStepTest {
    @Rule
    public JobCreatingJenkinsRule jenkins = new JobCreatingJenkinsRule();

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
    public void testSuccessful_WithDefaultLinkType() throws Exception {
        var job = jenkins.createPipeline("successful_send_event_step_with_default_linktype.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        var actT = events.findNext(EiffelActivityTriggeredEvent.class);
        var cD = events.findNext(GenericEiffelEvent.class);
        assertThat(cD.getMeta().getType(), is("EiffelCompositionDefinedEvent"));
        assertThat(cD, linksTo(actT, EiffelEvent.Link.Type.CONTEXT));
    }

    @Test
    public void testSuccessful_LogsEventDetails() throws Exception {
        var job = jenkins.createPipeline("successful_send_event_step_with_default_linktype.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        var cD = events.findNext(GenericEiffelEvent.class);
        jenkins.assertLogContains(
                String.format("Successfully sent %s with id %s", cD.getMeta().getType(), cD.getMeta().getId()),
                job.getBuildByNumber(1));
    }

    @Test
    public void testSuccessful_WithCustomLinkType() throws Exception {
        var job = jenkins.createPipeline("successful_send_event_step_with_custom_linktype.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        var actT = events.findNext(EiffelActivityTriggeredEvent.class);
        var cD = events.findNext(GenericEiffelEvent.class);
        assertThat(cD.getMeta().getType(), is("EiffelCompositionDefinedEvent"));
        assertThat(cD, linksTo(actT, EiffelEvent.Link.Type.CAUSE));
    }

    @Test
    public void testSuccessful_WithoutLink() throws Exception {
        var job = jenkins.createPipeline("successful_send_event_step_without_link.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        var cD = events.findNext(GenericEiffelEvent.class);
        assertThat(cD.getLinks(), hasSize(0));
    }

    @Test
    public void testSuccessful_ReturnsSentMessage() throws Exception {
        var job = jenkins.createPipeline("successful_send_event_step_with_payload_saved.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        // This job uses the writeJSON step to write the returned payload to event.json.
        // Deserialize that file and compare it against the event sent on the bus.
        var eventWrittenToWorkspace = new ObjectMapper().readValue(
                jenkins.jenkins.getWorkspaceFor(job).child("event.json").readToString(), EiffelEvent.class);
        var publishedEvent = events.findNext(GenericEiffelEvent.class);
        assertThat(publishedEvent, is(eventWrittenToWorkspace));
    }

    @Test
    public void testSuccessful_RecordsArtifacts() throws Exception {
        var job = jenkins.createPipeline("successful_send_event_step_with_artifacts.groovy");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        var run = job.getBuildByNumber(1);
        var savedArtifacts = run.getActions(EiffelArtifactToPublishAction.class);
        assertThat(savedArtifacts, hasSize(2));
        assertThat(events.findNext(EiffelArtifactCreatedEvent.class), is(savedArtifacts.get(0).getEvent()));
        assertThat(events.findNext(EiffelArtifactCreatedEvent.class), is(savedArtifacts.get(1).getEvent()));
    }

    @Test
    public void testSuccessful_WithFolderCredentialSignature() throws Exception {
        var folder = jenkins.createProject(Folder.class, "testfolder");
        var testKeyStore = new TestKeyStore();
        addFolderCredential(folder, testKeyStore.createCredential("event_signing"));
        var job = jenkins.createPipeline(folder, "send_event_step_with_signing.groovy", "test");
        jenkins.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        // We're already testing elsewhere that the signing is done correctly, so here we
        // just check that the event has been signed with the correct identity and algorithm.
        var cD = events.findNext(EiffelArtifactCreatedEvent.class);
        assertThat(cD.getMeta().getSecurity(), is(notNullValue()));
        assertThat(cD.getMeta().getSecurity().getAuthorIdentity(), is(testKeyStore.getCertificateSubjectDN()));
        assertThat(cD.getMeta().getSecurity().getIntegrityProtection(), is(notNullValue()));
        assertThat(cD.getMeta().getSecurity().getIntegrityProtection().getAlg(),
                is(EiffelEvent.Meta.Security.IntegrityProtection.Alg.RS512));
        assertThat(cD.getMeta().getSecurity().getIntegrityProtection().getSignature(), not(emptyOrNullString()));
    }

    @Test
    public void testFailed_EventValidationError() throws Exception {
        var job = jenkins.createPipeline("failed_send_event_step_event_validation_error.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s (%s)",
                        SendEiffelEventStep.ERROR_MESSAGE_PREFIX,
                        EventValidationFailedException.class.getSimpleName()),
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailed_EventWithoutType() throws Exception {
        var job = jenkins.createPipeline("failed_send_event_step_event_without_type.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s (%s)",
                        SendEiffelEventStep.ERROR_MESSAGE_PREFIX,
                        IllegalArgumentException.class.getSimpleName()),
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailed_EventWithInvalidLinkType() throws Exception {
        var job = jenkins.createPipeline("failed_send_event_step_event_with_invalid_linktype.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        jenkins.assertLogContains(
                String.format("%s: %s",
                        IllegalArgumentException.class.getSimpleName(), "The activity link type must be one of"),
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailed_UseOfSystemCredential() throws Exception {
        var credProvider = SystemCredentialsProvider.getInstance();
        credProvider.getCredentials().add(new TestKeyStore().createCredential("event_signing"));
        credProvider.save();
        var job = jenkins.createPipeline("send_event_step_with_signing.groovy");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        jenkins.assertLogContains("No credentials with the id event_signing could be found",
                job.getBuildByNumber(1));
    }

    @Test
    public void testFailed_UseOfFolderCredentialsFromOtherFolder() throws Exception {
        var jobFolder = jenkins.createProject(Folder.class, "jobFolder");
        var credentialFolder = jenkins.createProject(Folder.class, "credentialFolder");
        addFolderCredential(credentialFolder, new TestKeyStore().createCredential("event_signing"));
        var job = jenkins.createPipeline(jobFolder, "send_event_step_with_signing.groovy", "test");
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));

        var events = new EventSet(Mocks.messages);

        jenkins.assertLogContains("No credentials with the id event_signing could be found",
                job.getBuildByNumber(1));
    }

    private void addFolderCredential(@NonNull final Folder folder, @NonNull final Credentials cred) throws IOException {
        for (var store : CredentialsProvider.lookupStores(folder)) {
            if (store.getProvider() instanceof FolderCredentialsProvider && store.getContext() == folder) {
                store.addCredentials(Domain.global(), cred);
                return;
            }
        }
    }
}
