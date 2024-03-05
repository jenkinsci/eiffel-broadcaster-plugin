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

import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;

/**
 * A singleton that implements a simple time-based in-memory pull-through cache of event signing keys
 * to avoid decoding keystores and keys every time an event is sent.
 * <p>
 * Even though an item in the cache is only deemed valid for a limited time (see {@link #TTL}),
 * items are currently never evicted from the cache so the cache will grow over time if
 * a large number of credential objects are created and referenced by code that signs events.
 */
public class SigningKeyCache {
    /** The maximum time a cached item will be reused without requiring a refresh from the credential. */
    private static final TemporalAmount TTL = Duration.ofMinutes(1);

    private final Map<CertificateCredentials, Item> cache = new HashMap<>();

    private SigningKeyCache() { }

    /** Clears the cache of all entries. */
    public synchronized void clear() {
        cache.clear();
    }

    /**
     * Looks up a credential and returns the identity (subject) of the certificate and the private key.
     *
     * @param cred the credentials from which to extract the key and identity
     * @return an {@link Item} with the private key and identity
     * @throws InvalidCertificateConfigurationException if no credential with the given id was found,
     *         if the credential had the wrong type, or if the {@link KeyStore} was entirely empty or
     *         its first item didn't contain a certificate with a private key
     * @throws KeyStoreException if the {@link KeyStore} hasn't been initialized
     * @throws NoSuchAlgorithmException if the algorithm needed to decrypt the key isn't available
     * @throws UnrecoverableKeyException if the key couldn't be decrypted, e.g. because the password is wrong
     */
    public synchronized @NonNull Item get(@NonNull final CertificateCredentials cred)
            throws InvalidCertificateConfigurationException, KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException {
        var cachedItem = cache.get(cred);
        if (cachedItem != null && !cachedItem.hasExpired()) {
            return cachedItem;
        }

        var newCacheItem = new Item(cred);
        cache.put(cred, newCacheItem);
        return newCacheItem;
    }

    /** Returns the current number of (possibly expired) items in the cache. */
    public synchronized int size() {
        return cache.size();
    }

    /**
     * An immutable cache item, containing a signing key, the associated identity,
     * and the time when the data was extracted from the {@link KeyStore}.
     */
    public static class Item {
        private final String identity;

        private final PrivateKey key;

        private final Instant refreshTime = Instant.now();

        /**
         * Creates a new Item by extracting the private key and identity from the first certificate and
         * key found in the given {@link KeyStore} of the given credential.
         *
         * @param cred the {@link CertificateCredentials} from which to extract the key and identity
         * @throws InvalidCertificateConfigurationException if the {@link KeyStore} was entirely empty or
         *         its first item didn't contain a certificate with a private key
         * @throws KeyStoreException if the {@link KeyStore} hasn't been initialized
         * @throws NoSuchAlgorithmException if the algorithm needed to decrypt the key isn't available
         * @throws UnrecoverableKeyException if the key couldn't be decrypted, e.g. because the password is wrong
         */
        public Item(@NonNull final CertificateCredentials cred)
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
            var certificateChain = keyStore.getCertificateChain(alias);
            if (certificateChain != null && certificateChain.length > 0) {
                var certificate = certificateChain[0];
                if (certificate instanceof X509Certificate) {
                    identity = ((X509Certificate) certificate).getSubjectX500Principal().getName();
                }
            }
            if (key == null) {
                throw new InvalidCertificateConfigurationException(
                        "No private key was found in the credential object's keystore.");
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

        public boolean hasExpired() {
            return refreshTime.isBefore(Instant.now().minus(TTL));
        }
    }

    /** Returns the singleton object of this class. */
    public static SigningKeyCache getInstance() {
        return LazySigningKeyCacheSingleton.INSTANCE;
    }

    private static class LazySigningKeyCacheSingleton {
        private static final SigningKeyCache INSTANCE = new SigningKeyCache();
    }
}
