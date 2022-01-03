/**
 The MIT License

 Copyright 2021-2022 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.SourceProvider;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import hudson.PluginWrapper;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Eiffel event source information (the <tt>meta.source</tt> member of all events)
 * for a Jenkins plugin.
 *
 * Computes values during initialization and reuses them for all subsequent events, except for
 * the hostname whose resolution might fail. We periodically retry resolving the hostname so
 * that an initial failure caused by a transient DNS outage doesn't cause the hostname field to
 * be left empty forever.
 */
public class JenkinsSourceProvider implements SourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsSourceProvider.class);

    /** How frequently to attempt to find out the current host's name. */
    private static final Duration HOST_CHECK_INTERVAL = Duration.ofMinutes(2);

    /** The last time we attempted to obtain the current host's name. */
    private Instant lastHostCheck = Instant.MIN;

    private String physicalHostname;
    private String name;
    private String serializer;
    private URI uri;

    public JenkinsSourceProvider() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            String pluginShortName = EiffelBroadcasterConfig.class.getAnnotation(Symbol.class).value()[0];
            PluginWrapper pluginWrapper = jenkins.getPluginManager().getPlugin(pluginShortName);
            if (pluginWrapper != null) {
                name = pluginWrapper.getDisplayName();

                if (pluginWrapper.getUrl() != null) {
                    try {
                        uri = new URI(pluginWrapper.getUrl());
                    } catch (URISyntaxException e) {
                        logger.error("Error parsing plugin URL", e);
                    }
                }

                try {
                    serializer = PackageURLBuilder.aPackageURL()
                            .withType(PackageURL.StandardTypes.MAVEN)
                            .withNamespace(pluginWrapper.getManifest().getMainAttributes().getValue("Group-Id"))
                            .withName(pluginWrapper.getShortName())
                            .withVersion(pluginWrapper.getVersion())
                            .build().toString();
                } catch (MalformedPackageURLException e) {
                    logger.error("Error creating package URL", e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void populateSource(@Nonnull EiffelEvent.Meta.Source source) {
        // Most source values can be set by the input event so don't overwrite existing values,
        // except for the serializer since it's always this plugin that does the serialization.
        if (source.getHost() == null) {
            source.setHost(getHost());
        }
        if (source.getName() == null) {
            source.setName(name);
        }
        source.setSerializer(serializer);
        if (source.getUri() == null) {
            source.setUri(uri);
        }
    }

    /**
     * Returns the name of the current host. Depending on {@link EiffelBroadcasterConfig#getHostnameSource()}
     * it will either grab the hostname from the configured Jenkins controller URL or ask the OS and network stack.
     * In the latter case DNS lookups may be involved so the call may block. To avoid a complete standstill if
     * there are DNS problems the check won't take place more often that once every {@link #HOST_CHECK_INTERVAL},
     * and the result will be cached until the next restart.
     */
    @CheckForNull
    private synchronized String getHost() {
        EiffelBroadcasterConfig config = EiffelBroadcasterConfig.getInstance();
        if (config == null) {
            return null;
        }
        switch (config.getHostnameSource()) {
            case NETWORK_STACK:
                if (physicalHostname == null &&
                        Duration.between(lastHostCheck, Instant.now()).compareTo(HOST_CHECK_INTERVAL) > 0) {
                    try {
                        physicalHostname = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        lastHostCheck = Instant.now();
                        logger.debug("Error looking up the hostname of the Jenkins server", e);
                    }
                }
                return physicalHostname;
            case CONFIGURED_URL:
                try {
                    return new URL(Jenkins.get().getRootUrl()).getHost();
                } catch (MalformedURLException e) {
                    logger.debug("Error parsing the configured root URL", e);
                    return null;
                }
            default:
                throw new IllegalStateException(
                        String.format("Unexpected enum value %s encountered", config.getHostnameSource()));
        }
    }
}
