/**
 The MIT License

 Copyright 2023-2024 Axis Communications AB.

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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.signing;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/** Rudimentary happy path tests for {@link CertificateSigningInfo}. */
public class CertificateSigningInfoTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testFindsDataInCredential() throws Exception {
        final String credentialsId = "any-random-id";

        var testKeyStore = new TestKeyStore();
        var cred = testKeyStore.createCredential(credentialsId);
        var credProvider = SystemCredentialsProvider.getInstance();
        credProvider.getCredentials().add(cred);
        credProvider.save();

        var signingInfo = new CertificateSigningInfo(cred);

        assertThat(signingInfo.getIdentity(), is(testKeyStore.getCertificateSubjectDN()));
        assertThat(signingInfo.getKey(), notNullValue());
    }
}