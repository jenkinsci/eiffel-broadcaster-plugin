/**
 The MIT License

 Copyright 2021 Axis Communications AB.

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
import hudson.Plugin;
import hudson.PluginWrapper;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
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
    private static final Logger logger = LoggerFactory.getLogger(EiffelBroadcasterConfig.class);

    /** How frequently to attempt to find out the current host's name. */
    private static final Duration HOST_CHECK_INTERVAL = Duration.ofMinutes(2);

    /** The last time we attempted to obtain the current host's name. */
    private Instant lastHostCheck = Instant.MIN;

    private String host;
    private String name;
    private String serializer;
    private URI uri;

    public JenkinsSourceProvider() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            Plugin plugin = jenkins.getPlugin(EiffelBroadcasterConfig.class);
            if (plugin != null) {
                PluginWrapper pluginWrapper = plugin.getWrapper();
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
        source.setHost(getHost());
        source.setName(name);
        source.setSerializer(serializer);
        source.setUri(uri);
    }

    /**
     * Returns the name of the current host. Unless already known it looks up the name against
     * the network stack. DNS lookups may be involved so the call may block. To avoid a complete
     * standstill if there are DNS problems the check won't take place more often that once
     * every {@link #HOST_CHECK_INTERVAL}.
     */
    @CheckForNull
    private synchronized String getHost() {
        if (host == null &&
                Duration.between(lastHostCheck, Instant.now()).compareTo(HOST_CHECK_INTERVAL) > 0) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                lastHostCheck = Instant.now();
                logger.debug("Error looking up the hostname of the Jenkins server", e);
            }
        }
        return host;
    }
}
