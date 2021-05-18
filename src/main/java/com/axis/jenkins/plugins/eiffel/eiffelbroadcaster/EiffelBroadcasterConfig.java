/**
 The MIT License

 Copyright 2018-2021 Axis Communications AB.

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
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.kohsuke.stapler.DataBoundConstructor;
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
public final class EiffelBroadcasterConfig extends Plugin implements Describable<EiffelBroadcasterConfig> {
    private static final Logger logger = LoggerFactory.getLogger(EiffelBroadcasterConfig.class);
    private final String[] schemes = {"amqp", "amqps"};
    private static final String SERVER_URI = "serverUri";
    private static final String USERNAME = "userName";
    private static final String PASSWORD = "userPassword";

    /* The status whether the plugin is enabled */
    private boolean enableBroadcaster;

    /* The MQ server URI */
    private String serverUri;
    private String userName;
    private Secret userPassword;

    /* The plugin sends messages to an exchange which will push the messages to one or several queues.*/
    private String exchangeName;

    /* The virtual host which the connection intends to operate within. */
    private String virtualHost;

    /* Messages will be sent with a routing key which allows messages to be delivered to queues that are bound with a
     * matching binding key. The routing key must be a list of words, delimited by dots.
     */
    private String routingKey;
    /* Messages delivered to durable queues will be logged to disk if persistent delivery is set. */
    private boolean persistentDelivery;
    /* Application id that can be read by the consumer (optional). */
    private String appId;
    /* A list of strings representing categories to include in the ActTs. */
    private final List<String> activityCategories = new ArrayList<>();

    /**
     * Creates an instance with specified parameters.
     *
     * @param enableBroadcaster  if this plugin is enabled
     * @param serverUri          the server uri
     * @param userName           the user name
     * @param userPassword       the user password
     * @param exchangeName       the name of the exchange
     * @param virtualHost        the name of the virtual host
     * @param routingKey         the routing key
     * @param persistentDelivery if using persistent delivery mode
     * @param appId              the application id
     */
    @DataBoundConstructor
    public EiffelBroadcasterConfig(boolean enableBroadcaster, String serverUri, String userName, Secret userPassword,
                            String exchangeName, String virtualHost, String routingKey, boolean persistentDelivery,
                            String appId, String activityCategories) {
        this.enableBroadcaster = enableBroadcaster;
        this.serverUri = serverUri;
        this.userName = userName;
        this.userPassword = userPassword;
        this.exchangeName = exchangeName;
        this.virtualHost = virtualHost;
        this.routingKey = routingKey;
        this.persistentDelivery = persistentDelivery;
        this.appId = appId;
        this.activityCategories.addAll(Util.getLinesInString(activityCategories));
    }

    @Override
    public void start() throws Exception {
        super.start();
        logger.info("Starting EiffelBroadcaster Plugin");
        EiffelEvent.setSourceProvider(new JenkinsSourceProvider());
        load();
        MQConnection.getInstance().initialize(userName, userPassword, serverUri, virtualHost);
    }

    /**
     * Load configuration on invoke.
     */
    public EiffelBroadcasterConfig() {
        this.enableBroadcaster = false; // default value
        this.persistentDelivery = true; // default value
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException,
            Descriptor.FormException {
        req.bindJSON(this, formData);
        save();
        MQConnection.getInstance().initialize(userName, userPassword, serverUri, virtualHost);
    }

    /**
     * Gets whether this plugin is enabled or not.
     *
     * @return true if this plugin is enabled.
     */
    public boolean isBroadcasterEnabled() {
        return this.enableBroadcaster;
    }

    /**
     * Sets flag whether this plugin is enabled or not.
     *
     * @param enableBroadcaster true if this plugin is enabled.
     */
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
    public void setUserPassword(Secret userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Gets this extension's instance.
     *
     * If {@link Jenkins#getInstanceOrNull()} isn't available
     * or the plugin class isn't registered null will be returned.
     *
     * @return the instance of this extension.
     */
    public static EiffelBroadcasterConfig getInstance() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            return jenkins.getPlugin(EiffelBroadcasterConfig.class);
        } else {
            logger.error("Error, Jenkins could not be found, so no plugin!");
            return null;
        }
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
     * Sets the routing key.
     *
     * @param routingKey the routing key.
     */
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
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
    public void setActivityCategories(String activityCategories) {
        this.activityCategories.clear();
        this.activityCategories.addAll(Util.getLinesInString(activityCategories));
    }

    /**
     * Returns the descriptor instance.
     *
     * @return descriptor instance.
     */
    @Override
    public Descriptor<EiffelBroadcasterConfig> getDescriptor() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            return instance.getDescriptorOrDie(getClass());
        }
        return null;
    }

    /**
     * Implementation of the Descriptor interface.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<EiffelBroadcasterConfig> {
        @Override
        public String getDisplayName() {
            return "EiffelBroadcaster";
        }

        /**
         * Tests connection to the server URI.
         *
         * @param uri the URI.
         * @param name the user name.
         * @param pw the user password.
         * @return FormValidation object that indicates ok or error.
         * @throws javax.servlet.ServletException Exception for servlet.
         */
        @RequirePOST
        public FormValidation doTestConnection(@QueryParameter(SERVER_URI) final String uri,
                                               @QueryParameter(USERNAME) final String name,
                                               @QueryParameter(PASSWORD) final Secret pw) throws ServletException {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            UrlValidator urlValidator = new UrlValidator(getInstance().schemes, UrlValidator.ALLOW_LOCAL_URLS);
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
    }

}
