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

import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

/**
 * Extracts and holds the private key and subject DN from a certificate in a {@link CertificateCredentials}.
 */
class CertificateSigningInfo {
    private final String identity;

    private final PrivateKey key;

    /**
     * Creates a new CertificateSigningInfo by extracting the private key and identity from the first certificate and
     * key found in the {@link KeyStore} of the given credential.
     *
     * @param cred the {@link CertificateCredentials} from which to extract the key and identity
     * @throws InvalidCertificateConfigurationException if the {@link KeyStore} was entirely empty or
     *                                                  its first item didn't contain a certificate with a private key
     * @throws KeyStoreException                        if the {@link KeyStore} hasn't been initialized
     * @throws NoSuchAlgorithmException                 if the algorithm needed to decrypt the key isn't available
     * @throws UnrecoverableKeyException                if the key couldn't be decrypted, e.g. because the password is wrong
     */
    public CertificateSigningInfo(@NonNull final CertificateCredentials cred)
            throws InvalidCertificateConfigurationException, KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException {
        String identity = null;
        PrivateKey key = null;

        // Extract the first private key in the key store, then extract
        // the subject DN from the certificate associated with that key.
        var keyStore = cred.getKeyStore();
        if (!keyStore.aliases().hasMoreElements()) {
            throw new InvalidCertificateConfigurationException("The keystore in the credential object was empty.");
        }
        var alias = keyStore.aliases().nextElement();
        if (keyStore.isKeyEntry(alias)) {
            key = (PrivateKey) keyStore.getKey(alias, cred.getPassword().getPlainText().toCharArray());
        }
        if (key == null) {
            throw new InvalidCertificateConfigurationException(
                    "No private key was found in the credential object's keystore.");
        }
        var certificateChain = keyStore.getCertificateChain(alias);
        if (certificateChain != null && certificateChain.length > 0) {
            var certificate = certificateChain[0];
            if (certificate instanceof X509Certificate) {
                identity = ((X509Certificate) certificate).getSubjectX500Principal().getName();
            }
        }
        if (identity == null) {
            throw new InvalidCertificateConfigurationException(
                    "No X.509 certificate was found in the credential object's keystore.");
        }
        this.identity = identity;
        this.key = key;
    }

    @NonNull
    public String getIdentity() {
        return identity;
    }

    @NonNull
    public PrivateKey getKey() {
        return key;
    }
}
