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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.signing;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.HashAlgorithm;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.UnsupportedAlgorithmException;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;

/**
 * Signs an event using credentials tied to a {@link Run}. This is
 * obviously only available during a specific build.
 */
public class UserEventSigner extends EventSigner {
    private final String credentialsId;

    private final HashAlgorithm hashAlgorithm;

    private final Run<?, ?> run;

    public UserEventSigner(@NonNull final String credentialsId, @NonNull final HashAlgorithm hashAlgorithm,
                           @NonNull final Run<?, ?> run) {
        this.credentialsId = credentialsId;
        this.hashAlgorithm = hashAlgorithm;
        this.run = run;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    /** {@inheritDoc} */
    @Override
    public boolean sign(@NonNull EiffelEvent event) throws InvalidCertificateConfigurationException,
            InvalidKeyException, JsonCanonicalizationException, KeyStoreException, NoSuchAlgorithmException,
            SignatureException, UnrecoverableKeyException, UnsupportedAlgorithmException {
        var cred = CredentialsProvider.findCredentialById(
                getCredentialsId(), StandardCertificateCredentials.class, getRun());
        if (cred == null) {
            throw new InvalidCertificateConfigurationException(
                    String.format("No credentials with the id %s could be found", getCredentialsId()));
        }
        var sigData = new CertificateSigningInfo(cred);
        event.sign(sigData.getKey(), sigData.getIdentity(), getHashAlgorithm());
        return true;
    }
}
