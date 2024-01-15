/**
 The MIT License

 Copyright 2022 Axis Communications AB.

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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys.FixedRoutingKeyProvider;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys.SepiaRoutingKeyProvider;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.jenkinsci.Symbol;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class ConfigurationAsCodeTest {
    // {@link JenkinsConfiguredWithCodeRule} requires that the formal type of the rule attribute
    // is JenkinsConfiguredWithCodeRule.
    @Rule
    public JenkinsConfiguredWithCodeRule jenkins = new JenkinsConfiguredWithCodeRule();

    /**
     * Test that an old JCasC configuration that uses the old way of configuring a routing key
     * (prior to the introduction of
     * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys.RoutingKeyProvider})
     * is translated to the equivalent {@link FixedRoutingKeyProvider}.
     */
    @Test
    @ConfiguredWithCode("jcasc-input-with-legacy-routing-key.yml")
    public void testSupportsConfigurationAsCode_WithLegacyRoutingKey() throws Exception {
        var config = EiffelBroadcasterConfig.getInstance();
        assertThat(config.getAppId(), is("random-appid"));
        assertThat(config.getEnableBroadcaster(), is(true));
        assertThat(config.getExchangeName(), is("eiffel-exchange"));
        assertThat(config.getHostnameSource(), is(HostnameSource.CONFIGURED_URL));
        assertThat(config.getPersistentDelivery(), is(false));
        assertThat(config.getRoutingKeyProvider(), instanceOf(FixedRoutingKeyProvider.class));
        assertThat(((FixedRoutingKeyProvider) config.getRoutingKeyProvider()).getFixedRoutingKey(),
                is("random-legacy-key"));
        assertThat(config.getServerUri(), is("amqp://rabbitmq.example.com"));
        assertThat(config.getUserName(), is("johndoe"));
        assertThat(config.getVirtualHost(), is("/"));
    }

    @Test
    @ConfiguredWithCode("jcasc-input-without-routing-key-provider.yml")
    public void testSupportsConfigurationAsCode_WithoutRoutingKeyProvider() throws Exception {
        var config = EiffelBroadcasterConfig.getInstance();
        assertThat(config.getAppId(), is("random-appid"));
        assertThat(config.getEnableBroadcaster(), is(true));
        assertThat(config.getExchangeName(), is("eiffel-exchange"));
        assertThat(config.getHostnameSource(), is(HostnameSource.CONFIGURED_URL));
        assertThat(config.getPersistentDelivery(), is(false));
        assertThat(config.getRoutingKeyProvider(), instanceOf(SepiaRoutingKeyProvider.class));
        assertThat(config.getServerUri(), is("amqp://rabbitmq.example.com"));
        assertThat(config.getUserName(), is("johndoe"));
        assertThat(config.getVirtualHost(), is("/"));
    }

    @Test
    @ConfiguredWithCode("jcasc-input-with-fixed-routing-key-provider.yml")
    public void testSupportsConfigurationAsCode_WithFixedRoutingKeyProvider() throws Exception {
        var config = EiffelBroadcasterConfig.getInstance();
        assertThat(config.getAppId(), is("random-appid"));
        assertThat(config.getEnableBroadcaster(), is(true));
        assertThat(config.getExchangeName(), is("eiffel-exchange"));
        assertThat(config.getHostnameSource(), is(HostnameSource.CONFIGURED_URL));
        assertThat(config.getPersistentDelivery(), is(false));
        assertThat(config.getRoutingKeyProvider(), instanceOf(FixedRoutingKeyProvider.class));
        assertThat(((FixedRoutingKeyProvider) config.getRoutingKeyProvider()).getFixedRoutingKey(),
                is("random-routing-key"));
        assertThat(config.getServerUri(), is("amqp://rabbitmq.example.com"));
        assertThat(config.getUserName(), is("johndoe"));
        assertThat(config.getVirtualHost(), is("/"));
    }

    @Test
    @ConfiguredWithCode("jcasc-input-with-sepia-routing-key-provider.yml")
    public void testSupportsConfigurationAsCode_WithSepiaRoutingKeyProvider() throws Exception {
        var config = EiffelBroadcasterConfig.getInstance();
        assertThat(config.getAppId(), is("random-appid"));
        assertThat(config.getEnableBroadcaster(), is(true));
        assertThat(config.getExchangeName(), is("eiffel-exchange"));
        assertThat(config.getHostnameSource(), is(HostnameSource.CONFIGURED_URL));
        assertThat(config.getPersistentDelivery(), is(false));
        assertThat(config.getRoutingKeyProvider(), instanceOf(SepiaRoutingKeyProvider.class));
        assertThat(((SepiaRoutingKeyProvider) config.getRoutingKeyProvider()).getTag(),
                is("random-tag"));
        assertThat(config.getServerUri(), is("amqp://rabbitmq.example.com"));
        assertThat(config.getUserName(), is("johndoe"));
        assertThat(config.getVirtualHost(), is("/"));
    }

    @Test
    @ConfiguredWithCode("jcasc-input-with-sepia-routing-key-provider.yml")
    public void testSupportsConfigurationExport() throws Exception {
        var context = new ConfigurationContext(ConfiguratorRegistry.get());
        var pluginShortName = EiffelBroadcasterConfig.class.getAnnotation(Symbol.class).value()[0];
        var pluginNode = getUnclassifiedRoot(context).get(pluginShortName);
        var sanitizedYAML = toYamlString(pluginNode)
                .replaceFirst("(?m)^userPassword: .*(?:\\r?\\n)?", "")
                .replaceFirst("(?m)^    dummy: .*(?:\\r?\\n)?", "");
        assertThat(sanitizedYAML, is(toStringFromYamlFile(this, "jcasc-expected-output.yml")));
    }
}
