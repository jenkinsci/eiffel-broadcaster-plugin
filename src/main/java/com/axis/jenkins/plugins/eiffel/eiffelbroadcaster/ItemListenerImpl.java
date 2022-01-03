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

import hudson.Extension;
import hudson.model.listeners.ItemListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives job status notifications {@link ItemListener}.
 * Initialize the MQ connection.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
public class ItemListenerImpl extends ItemListener {

    private static final Logger logger = LoggerFactory.getLogger(ItemListenerImpl.class);

    /**
     * Create instance.
     */
    public ItemListenerImpl() {
        super();
    }

    @Override
    public final void onLoaded() {
        logger.info("All jobs have been loaded.");
        EiffelBroadcasterConfig config = EiffelBroadcasterConfig.getInstance();
        if (config != null && config.getEnableBroadcaster()) {
            MQConnection.getInstance().initialize(config.getUserName(), config.getUserPassword(),
                    config.getServerUri(), config.getVirtualHost());
            // initialize EiffelJobTable singleton.
            EiffelJobTable.getInstance();
        }
        super.onLoaded();
    }
}
