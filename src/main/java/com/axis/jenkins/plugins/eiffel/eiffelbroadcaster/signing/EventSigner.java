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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.UnsupportedAlgorithmException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;

/**
 * Simple interface that performs in-place signing of an {@link EiffelEvent}.
 * The selection of the key to be used is left to the implementing classes.
 */
public interface EventSigner {
    /**
     * Request signing of the given event using a private key and identity.
     * Based on its configuration, the implementation may choose to not
     * sign the event.
     *
     * @param event the {@link EiffelEvent} to sign in place
     * @return true if the event was signed, false otherwise
     * @throws InvalidCertificateConfigurationException if the keystore in the certificate credential was entirely
     *         empty or its first item didn't contain a certificate with a private key
     * @throws InvalidKeyException if the given private key was invalid
     * @throws JsonCanonicalizationException if there was an error serializing the event to canonical JSON form
     * @throws KeyStoreException if the {@link java.security.KeyStore} hasn't been initialized
     *         (shouldn't happen and indicates a bug)
     * @throws NoSuchAlgorithmException if the algorithm needed to decrypt the key isn't available
     * @throws SignatureException if there's a general problem in the signing process
     * @throws UnrecoverableKeyException if the key couldn't be decrypted, e.g. because the password is wrong
     * @throws UnsupportedAlgorithmException if the credential's signature algorithm isn't supported
     *         by this implementation of the Eiffel protocol or the available cryptography provider
     */
    boolean sign(@NonNull final EiffelEvent event)
            throws InvalidCertificateConfigurationException, InvalidKeyException, JsonCanonicalizationException,
            KeyStoreException, NoSuchAlgorithmException, SignatureException, UnrecoverableKeyException,
            UnsupportedAlgorithmException;
}
