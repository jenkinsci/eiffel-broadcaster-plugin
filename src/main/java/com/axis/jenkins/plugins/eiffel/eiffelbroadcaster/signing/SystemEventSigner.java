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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelBroadcasterConfig;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.UnsupportedAlgorithmException;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.ItemGroup;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.List;

/**
 * Signs an {@link EiffelEvent} using a system credential, i.e. a global credential
 * that isn't available to normal builds. Events signed with this class shouldn't be
 * under user control.
 */
public class SystemEventSigner implements EventSigner {
    /** {@inheritDoc} */
    @Override
    public boolean sign(@NonNull EiffelEvent event)
            throws InvalidCertificateConfigurationException, InvalidKeyException, JsonCanonicalizationException,
            KeyStoreException, NoSuchAlgorithmException, SignatureException, UnrecoverableKeyException,
            UnsupportedAlgorithmException {
        EiffelBroadcasterConfig config = EiffelBroadcasterConfig.getInstance();
        if (!config.isSystemSigningEnabled()) {
            return false;
        }

        var cred = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardCertificateCredentials.class, (ItemGroup) null, null, List.of()),
                CredentialsMatchers.allOf(CredentialsMatchers.withId(config.getSystemSigningCredentialsId())));
        if (cred == null) {
            throw new InvalidCertificateConfigurationException(
                    String.format("No credentials with the id %s could be found", config.getSystemSigningCredentialsId()));
        }
        var sigData = SigningKeyCache.getInstance().get(cred);
        event.sign(sigData.getKey(), sigData.getIdentity(), config.getSystemSigningHashAlg());
        return true;
    }
}
