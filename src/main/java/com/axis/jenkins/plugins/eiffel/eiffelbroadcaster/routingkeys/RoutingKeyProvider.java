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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.io.Serializable;
import jenkins.model.Jenkins;

/**
 * A routing key provider computes the AMQP routing key that should be used when publishing
 * a given {@link EiffelEvent}.
 */
public abstract class RoutingKeyProvider implements Describable<RoutingKeyProvider>, Serializable {
    /**
     * Computes the routing key for an event.
     *
     * @param event the {@link EiffelEvent} whose routing key is wanted
     * @return a string consisting of one or more dot-separated tokens that's usable as a routing key
     */
    public abstract String getRoutingKey(final EiffelEvent event);

    /**
     * Descriptor for {@link RoutingKeyProvider}s.
     */
    public abstract static class RoutingKeyProviderDescriptor extends Descriptor<RoutingKeyProvider> {
        /** Obtain all registered {@link RoutingKeyProviderDescriptor}s. */
        public static ExtensionList<RoutingKeyProviderDescriptor> all() {
            return Jenkins.get().getExtensionList(RoutingKeyProviderDescriptor.class);
        }
    }
}
