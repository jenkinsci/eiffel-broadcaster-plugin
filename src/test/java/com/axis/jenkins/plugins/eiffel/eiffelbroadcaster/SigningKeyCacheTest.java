/**
 The MIT License

 Copyright 2023 Axis Communications AB.

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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Rudimentary tests for {@link SigningKeyCache}. Just the happy path and a couple
 * of sad paths.
 *
 * Creating X.509 certificates programmatically is surprisingly painful, so we rely
 * on a fixed keystore stored as a test resource. This doesn't make it easy to try
 * all the sad paths but in this case doing the right thing is too much work.
 * The keystore was generated like this:
 *
 * <code>
 * keytool -genkey
 *     -keyalg RSA
 *     -alias selfsigned
 *     -dname CN=random-name
 *     -keystore src/test/resources/com/axis/jenkins/plugins/eiffel/eiffelbroadcaster/SigningKeyCacheTest.jks
 *     -keypass secret
 *     -storepass secret
 *     -validity 36500
 *     -keysize 4096
 * </code>
 */
public class SigningKeyCacheTest {
    private static final String KEYSTORE_PASSWORD = "secret";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testFindsDataInCredential() throws Exception {
        final String credentialsId = "any-random-id";

        CertificateCredentials cred = createCredential(loadTestKeyStore(), credentialsId);
        SystemCredentialsProvider credProvider = SystemCredentialsProvider.getInstance();
        credProvider.getCredentials().add(cred);
        credProvider.save();

        SigningKeyCache cache = SigningKeyCache.getInstance();
        SigningKeyCache.Item item = cache.get(credentialsId);

        assertThat(item, is(notNullValue()));
        assertThat(item.getIdentity(), is("CN=random-name"));
        PrivateKey pk = item.getKey();
        assertThat(SigningKeyCache.getInstance().size(), is(1));
    }

    @Test(expected = InvalidCertificateConfigurationException.class)
    public void testNonexistentCredentialsIdRaisesException() throws Exception {
        CertificateCredentials cred = createCredential(loadTestKeyStore(), "any-random-id");
        SystemCredentialsProvider credProvider = SystemCredentialsProvider.getInstance();
        credProvider.getCredentials().add(cred);
        credProvider.save();

        SigningKeyCache.getInstance().get("some-other-id");
    }

    private CertificateCredentials createCredential(@NonNull final KeyStore keyStore,
                                                    @NonNull final String credentialsId)
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream keyStoreStream = new ByteArrayOutputStream();
        keyStore.store(keyStoreStream, KEYSTORE_PASSWORD.toCharArray());
        return new CertificateCredentialsImpl(CredentialsScope.SYSTEM, credentialsId, "", KEYSTORE_PASSWORD,
                new CertificateCredentialsImpl.UploadedKeyStoreSource(
                        null, SecretBytes.fromBytes(keyStoreStream.toByteArray())));
    }

    private KeyStore loadTestKeyStore()
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream keyStoreStream = getClass().getResourceAsStream(getClass().getSimpleName() + ".jks")) {
            keyStore.load(keyStoreStream, KEYSTORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }
}
