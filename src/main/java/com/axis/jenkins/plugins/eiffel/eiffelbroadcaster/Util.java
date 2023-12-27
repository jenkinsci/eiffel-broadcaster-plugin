/**
 The MIT License

 Copyright 2018-2023 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidationFailedException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.SchemaUnavailableException;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.UnsupportedAlgorithmException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Functions;
import hudson.model.AbstractItem;
import hudson.model.Queue;
import hudson.model.Run;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants and helper functions.
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
public final class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class);
    private static final int NON_PERSISTENT_DELIVERY = 1;
    private static final int PERSISTENT_DELIVERY = 2;

    /** Translate jenkins exit status to eiffel status */
    private static final HashMap<String, EiffelActivityFinishedEvent.Data.Outcome.Conclusion> STATUS_TRANSLATION = new HashMap<>();
    static {
        STATUS_TRANSLATION.put("SUCCESS", EiffelActivityFinishedEvent.Data.Outcome.Conclusion.SUCCESSFUL);
        STATUS_TRANSLATION.put("UNSTABLE", EiffelActivityFinishedEvent.Data.Outcome.Conclusion.UNSUCCESSFUL);
        STATUS_TRANSLATION.put("FAILURE", EiffelActivityFinishedEvent.Data.Outcome.Conclusion.FAILED);
        STATUS_TRANSLATION.put("ABORTED", EiffelActivityFinishedEvent.Data.Outcome.Conclusion.ABORTED);
        STATUS_TRANSLATION.put("x", EiffelActivityFinishedEvent.Data.Outcome.Conclusion.TIMED_OUT);
        STATUS_TRANSLATION.put("INCONCLUSIVE", EiffelActivityFinishedEvent.Data.Outcome.Conclusion.INCONCLUSIVE);
    }

    /**
     * Utility classes should not have a public or default constructor.
     */
    private Util() {
    }

    /**
     * Fetches the full name task name if available.
     *
     * @param t
     * Queue.task
     * @return full name if available, else the short name
     */
    public static String getFullName(Queue.Task t) {
        if (t instanceof AbstractItem) {
            return ((AbstractItem)t).getFullName();
        } else {
            return t.getName();
        }
    }

    /**
     * Returns the URI of a {@link Run}, or one of its subresources.
     *
     * @param r the Run to return the URI for
     * @param pathSuffix additional path components to append
     * @return the URI asked for, or null if a URI couldn't be resolved
     */
    public static URI getRunUri(final Run r, String... pathSuffix) {
        Jenkins jenkins = Jenkins.get();
        if (jenkins.getRootUrl() != null) {
            try {
                String uri = Functions.joinPath(jenkins.getRootUrl(), r.getUrl());
                if (pathSuffix.length == 0) {
                    return new URI(uri);
                }
                return new URI(Functions.joinPath(uri, Functions.joinPath(pathSuffix)));
            } catch (URISyntaxException e) {
                logger.warn("Error constructing URI for build", e);
            }
        }
        return null;
    }

    /**
     * Publishes an {@link EiffelEvent} and raises an exception if an error occurs.
     *
     * @param event the Eiffel event to publish
     * @param allowSystemSignature true if the plugin's global credentials may be used to sign the event
     *                             before publishing (not to be used if the end user controls the event payload)
     * @return the published event or null if event publishing is disabled
     * @throws EventValidationFailedException if the validation of the event against the JSON schema fails
     * @throws InvalidCertificateConfigurationException if the keystore in the certificate credential was entirely
     *         empty or its first item didn't contain a certificate with a private key
     * @throws InvalidKeyException if the given private key was invalid
     * @throws JsonCanonicalizationException if there was an error serializing the event to canonical JSON form
     * @throws JsonProcessingException if there's an error during JSON serialization
     * @throws KeyStoreException if the {@link KeyStore} hasn't been initialized (shouldn't happen and indicates a bug)
     * @throws NoSuchAlgorithmException if the algorithm needed to decrypt the key isn't available
     * @throws SchemaUnavailableException if there's no schema available for the supplied event
     * @throws SignatureException if there's a general problem in the signing process
     * @throws UnsupportedAlgorithmException if the credential's signature algorithm isn't supported
     *         by this implementation of the Eiffel protocol or the available cryptography provider
     */
    @CheckForNull
    public static JsonNode mustPublishEvent(@NonNull final EiffelEvent event, final boolean allowSystemSignature)
            throws EventValidationFailedException, InvalidCertificateConfigurationException, InvalidKeyException,
            JsonCanonicalizationException, JsonProcessingException, KeyStoreException, NoSuchAlgorithmException,
            SchemaUnavailableException, SignatureException, UnsupportedAlgorithmException, UnrecoverableKeyException {
        EiffelBroadcasterConfig config = EiffelBroadcasterConfig.getInstance();
        if (config == null || !config.getEnableBroadcaster()) {
            return null;
        }

        if (allowSystemSignature && config.isSystemSigningEnabled()) {
            SigningKeyCache.Item sigData = SigningKeyCache.getInstance().get(config.getSystemSigningCredentialsId());
            event.sign(sigData.getKey(), sigData.getIdentity(), config.getSystemSigningHashAlg());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode eventJson = mapper.valueToTree(event);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .appId(config.getAppId())
                .deliveryMode(config.getPersistentDelivery() ? PERSISTENT_DELIVERY : NON_PERSISTENT_DELIVERY)
                .contentType("application/json")
                .timestamp(Calendar.getInstance().getTime())
                .build();
        config.getEventValidator().validate(event.getMeta().getType(), event.getMeta().getVersion(), eventJson);
        MQConnection.getInstance().addMessageToQueue(config.getExchangeName(),
                config.getRoutingKeyProvider().getRoutingKey(event),
                props, mapper.writeValueAsBytes(eventJson));
        return eventJson;
    }

    /**
     * Publishes an {@link EiffelEvent} and logs a message if there's an error.
     *
     * @param event the Eiffel event to publish
     * @param allowSystemSignature true if the plugin's global credentials may be used to sign the event
     *                             before publishing (not to be used if the end user controls the event payload)
     * @return the published event or null if there was an error or event publishing is disabled
     */
    @CheckForNull
    public static JsonNode publishEvent(@NonNull final EiffelEvent event, final boolean allowSystemSignature) {
        try {
            return mustPublishEvent(event, allowSystemSignature);
        } catch (JsonCanonicalizationException | JsonProcessingException e) {
            logger.error("Unable to serialize object to JSON: {}: {}", e.getMessage(), event);
        } catch (SchemaUnavailableException | EventValidationFailedException e) {
            logger.warn("Unable to validate event: {}", e.getMessage());
        } catch (GeneralSecurityException | InvalidCertificateConfigurationException | UnsupportedAlgorithmException e) {
            logger.error("Error signing event: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Splits the input string into lines, removes leading and trailing whitespace, and returns non-empty
     * lines in a list.
     *
     * @param s the line to split
     */
    public static List<String> getLinesInString(String s) {
        List<String> result = new ArrayList<>();
        for (String line : s.split("\\R")) {  // \R is "any Unicode linebreak sequence"
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Translate Jenkins job status into corresponding eiffel status.
     *
     * @param status
     * jenkins job status to translate.
     * @return translated status.
     */
    public static EiffelActivityFinishedEvent.Data.Outcome.Conclusion translateStatus(String status) {
        EiffelActivityFinishedEvent.Data.Outcome.Conclusion statusTranslated;
        if (STATUS_TRANSLATION.get(status) == null) {
            statusTranslated = STATUS_TRANSLATION.get("INCONCLUSIVE");
        } else {
            statusTranslated = STATUS_TRANSLATION.get(status);
        }
        return statusTranslated;
    }
}
