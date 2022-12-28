/**
 The MIT License

 Copyright 2018-2022 Axis Communications AB.

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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EventValidator;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys.FixedRoutingKeyProvider;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys.RoutingKeyProvider;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys.SepiaRoutingKeyProvider;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the EiffelBroadcaster plugin configuration to the system config page.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
@Symbol("eiffel-broadcaster")
public final class EiffelBroadcasterConfig extends GlobalConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EiffelBroadcasterConfig.class);
    private transient final String[] schemes = {};  // Replaced by ALLOWED_URL_SCHEMES but retained to satisfy XStream
    private static final String[] ALLOWED_URL_SCHEMES = {"amqp", "amqps"};
    private static final String SERVER_URI = "serverUri";
    private static final String USERNAME = "userName";
    private static final String PASSWORD = "userPassword";

    /* The name of the configuration file. */
    private static final String CONFIG_XML = "eiffel-broadcaster.xml";

    /* The status whether the plugin is enabled */
    private boolean enableBroadcaster = false;

    /* The MQ server URI */
    private String serverUri;
    private String userName;
    private Secret userPassword;

    /* The plugin sends messages to an exchange which will push the messages to one or several queues.*/
    private String exchangeName;

    /* The virtual host which the connection intends to operate within. */
    private String virtualHost;

    /**
     * Legacy attribute that used to hold the AMQP routing key for outbound messages. The routing key is
     * now determined by {@link #routingKeyProvider} and this attribute is kept only to please XStream.
     */
    private transient String routingKey;

    /**
     * A reference to a {@link RoutingKeyProvider} that can figure out what routing key
     * should be used for an outbound event.
     */
    private RoutingKeyProvider routingKeyProvider = new SepiaRoutingKeyProvider();

    /* Messages delivered to durable queues will be logged to disk if persistent delivery is set. */
    private boolean persistentDelivery = true;
    /* Application id that can be read by the consumer (optional). */
    private String appId;
    /* A list of strings representing categories to include in the ActTs. */
    private final List<String> activityCategories = new ArrayList<>();
    /* How the hostname used in the <code>meta.source.host</code> member should be determined. */
    private HostnameSource hostnameSource = HostnameSource.NETWORK_STACK;

    private transient final EventValidator eventValidator = new EventValidator();

    public EiffelBroadcasterConfig() {
        super.load();
        EiffelEvent.setSourceProvider(new JenkinsSourceProvider());
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
        req.bindJSON(this, formData);
        save();
        MQConnection.getInstance().initialize(userName, userPassword, serverUri, virtualHost);
        return true;
    }

    /** Migrates legacy model fields to the current model. Called by the XStream unmarshaler. */
    protected Object readResolve() {
        // If the legacy routingKey is set to something, migrate its contents
        // to a FixedRoutingKeyProvider for functional equivalence.
        if (StringUtils.isNotBlank(getRoutingKey())) {
            setRoutingKeyProvider(new FixedRoutingKeyProvider(getRoutingKey()));
            setRoutingKey(null);
        }
        return this;
    }

    /**
     * For backwards-compatibility with the previous {@link hudson.Plugin}-derived version.
     *
     * @return XmlFile representing the configuration file.
     */
    @Override
    protected XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.get().getRootDir(), CONFIG_XML));
    }

    /**
     * Gets whether this plugin is enabled or not.
     *
     * @return true if this plugin is enabled.
     */
    public boolean getEnableBroadcaster() {
        return this.enableBroadcaster;
    }

    /**
     * Sets flag whether this plugin is enabled or not.
     *
     * @param enableBroadcaster true if this plugin is enabled.
     */
    @DataBoundSetter
    public void setEnableBroadcaster(boolean enableBroadcaster) {
        this.enableBroadcaster = enableBroadcaster;
    }

    /**
     * Gets URI for MQ server.
     *
     * @return the URI.
     */
    public String getServerUri() {
        return this.serverUri;
    }

    /**
     * Sets URI for MQ server.
     *
     * @param serverUri the URI.
     */
    @DataBoundSetter
    public void setServerUri(final String serverUri) {
        this.serverUri = StringUtils.strip(StringUtils.stripToNull(serverUri), "/");
    }

    /**
     * Gets user name.
     *
     * @return the user name.
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets user name.
     *
     * @param userName the user name.
     */
    @DataBoundSetter
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets user password.
     *
     * @return the user password.
     */
    public Secret getUserPassword() {
        return this.userPassword;
    }

    /**
     * Sets user password.
     *
     * @param userPassword the user password.
     */
    @DataBoundSetter
    public void setUserPassword(Secret userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Gets this extension's instance.
     */
    public static EiffelBroadcasterConfig getInstance() {
        return EiffelBroadcasterConfig.all().get(EiffelBroadcasterConfig.class);
    }

    /**
     * Gets the exchange name.
     *
     * @return the exchange name.
     */
    public String getExchangeName() {
        return this.exchangeName;
    }

    /**
     * Sets the exchange name.
     *
     * @param exchangeName the exchange name.
     */
    @DataBoundSetter
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    /**
     * Gets the virtual host name.
     *
     * @return the virtual host name.
     */
    public String getVirtualHost() {
        return this.virtualHost;
    }

    /**
     * Sets the virtual host name.
     *
     * @param virtualHost the exchange name.
     */
    @DataBoundSetter
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    /**
     * Gets the routing key.
     *
     * @return the routing key.
     */
    public String getRoutingKey() {
        return this.routingKey;
    }

    /**
     * Sets the routing key. This method exists for backwards compatibility reasons.
     * Any non-null value will be passed to {@see #setRoutingKeyProvider(RoutingKeyProvider)}
     * and this setter's underlying attribute will be set to null.
     *
     * @param routingKey the routing key.
     */
    @DataBoundSetter
    public void setRoutingKey(String routingKey) {
        if (routingKey != null) {
            setRoutingKeyProvider(new FixedRoutingKeyProvider(routingKey));
        }
        this.routingKey = null;
    }

    /** Returns the currently configured {@link RoutingKeyProvider} implementation. */
    @NonNull
    public RoutingKeyProvider getRoutingKeyProvider() {
        return this.routingKeyProvider;
    }

    /**
     * Sets which {@link RoutingKeyProvider} implementation to use to generate routing keys for the events.
     * */
    @DataBoundSetter
    public void setRoutingKeyProvider(@NonNull final RoutingKeyProvider routingKeyProvider) {
        this.routingKeyProvider = routingKeyProvider;
    }

    /**
     * Returns true if persistentDelivery is to be used.
     *
     * @return if persistentDelivery is to be used.
     */
    public boolean getPersistentDelivery() {
        return this.persistentDelivery;
    }


    /**
     * Sets persistent delivery mode.
     *
     * @param pd if persistentDelivery is to be used.
     */
    @DataBoundSetter
    public void setPersistentDelivery(boolean pd) {
        this.persistentDelivery = pd;
    }

    /**
     * Returns application id.
     *
     * @return application id.
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * Sets application id.
     *
     * @param appId Application id to use
     */
    @DataBoundSetter
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /** Returns the list of categories to attach to the activities, expressed as a multi-line string. */
    public String getActivityCategories() {
        return StringUtils.join(this.activityCategories, '\n');
    }

    /** Returns the list of categories to attach to the activities. */
    public List<String> getActivityCategoriesList() {
        return this.activityCategories;
    }

    /** Sets the list of categories to attach to the activities. */
    @DataBoundSetter
    public void setActivityCategories(String activityCategories) {
        this.activityCategories.clear();
        this.activityCategories.addAll(Util.getLinesInString(activityCategories));
    }

    /** Returns the hostname source. */
    public HostnameSource getHostnameSource() {
        return hostnameSource;
    }

    /** Sets the hostname source. */
    @DataBoundSetter
    public void setHostnameSource(HostnameSource hostnameSource) {
        this.hostnameSource = hostnameSource;
    }

    @NonNull
    public EventValidator getEventValidator() {
        return eventValidator;
    }

    public ExtensionList<RoutingKeyProvider.RoutingKeyProviderDescriptor> getRoutingKeyProviderDescriptors() {
        return RoutingKeyProvider.RoutingKeyProviderDescriptor.all();
    }

    @Override
    public String getDisplayName() {
        return "EiffelBroadcaster";
    }

    /**
     * Tests connection to the server URI.
     *
     * @param uri  the URI.
     * @param name the user name.
     * @param pw   the user password.
     * @return FormValidation object that indicates ok or error.
     * @throws javax.servlet.ServletException Exception for servlet.
     */
    @RequirePOST
    public FormValidation doTestConnection(@QueryParameter(SERVER_URI) final String uri,
                                           @QueryParameter(USERNAME) final String name,
                                           @QueryParameter(PASSWORD) final Secret pw) throws ServletException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        UrlValidator urlValidator = new UrlValidator(ALLOWED_URL_SCHEMES, UrlValidator.ALLOW_LOCAL_URLS);
        FormValidation result = FormValidation.ok();
        if (urlValidator.isValid(uri)) {
            Connection conn = null;
            try {
                ConnectionFactory connFactory = new ConnectionFactory();
                connFactory.setUri(uri);
                if (StringUtils.isNotEmpty(name)) {
                    connFactory.setUsername(name);
                    if (StringUtils.isNotEmpty(Secret.toString(pw))) {
                        connFactory.setPassword(Secret.toString(pw));
                    }
                }
                conn = connFactory.newConnection();
            } catch (URISyntaxException e) {
                result = FormValidation.error("Invalid Uri");
            } catch (PossibleAuthenticationFailureException e) {
                result = FormValidation.error("Authentication Failure");
            } catch (Exception e) {
                result = FormValidation.error(e.getMessage());
            }
            // Close the connection outside the exception block above so spurious connection
            // closure problems won't flag the configuration as invalid, but do log the exception.
            if (conn != null && conn.isOpen()) {
                try {
                    conn.close();
                } catch (IOException e) {
                    logger.warn("An error occurred when closing the AMQP connection", e);
                }
            }
        } else {
            result = FormValidation.error("Invalid Uri");
        }
        return result;
    }

    /** Returns a custom help file location for fields where the Stapler doesn't locate it automatically. */
    @Override
    public String getHelpFile(final String fieldName) {
        if ("routingKeyProvider".equals(fieldName)) {
            return "/plugin/eiffel-broadcaster/help-routing-key-provider.html";
        }
        return super.getHelpFile(fieldName);
    }
}
