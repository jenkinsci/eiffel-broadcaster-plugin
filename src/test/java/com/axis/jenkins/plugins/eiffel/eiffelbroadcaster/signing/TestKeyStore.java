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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Loads a key store for use in tests. Creating X.509 certificates programmatically
 * is surprisingly painful, so we rely on a fixed keystore stored as a test resource.
 * This doesn't make it easy to try all the sad paths but doing the right thing is
 * simply too much work. The keystore was generated like this:
 * <p>
 * <code>
 * keytool -genkey
 *     -keyalg RSA
 *     -alias selfsigned
 *     -dname CN=random-name
 *     -keystore src/test/resources/com/axis/jenkins/plugins/eiffel/eiffelbroadcaster/signing/TestKeyStore.jks
 *     -keypass secret
 *     -storepass secret
 *     -validity 36500
 *     -keysize 4096
 * </code>
 */
public class TestKeyStore {
    private static final String KEYSTORE_PASSWORD = "secret";

    private static final String CERT_SUBJECT_DN = "CN=random-name";

    private final KeyStore keyStore;

    public TestKeyStore() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (var keyStoreStream = getClass().getResourceAsStream(getClass().getSimpleName() + ".jks")) {
            keyStore.load(keyStoreStream, getPassword().toCharArray());
        }
    }

    @NonNull
    public String getPassword() {
        return KEYSTORE_PASSWORD;
    }

    @NonNull
    public String getCertificateSubjectDN() {
        return CERT_SUBJECT_DN;
    }

    @NonNull
    public CertificateCredentials createCredential(@NonNull final String credentialsId)
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        var keyStoreStream = new ByteArrayOutputStream();
        keyStore.store(keyStoreStream, getPassword().toCharArray());
        return new CertificateCredentialsImpl(CredentialsScope.SYSTEM, credentialsId, "", getPassword(),
                new CertificateCredentialsImpl.UploadedKeyStoreSource(
                        null, SecretBytes.fromBytes(keyStoreStream.toByteArray())));
    }
}
