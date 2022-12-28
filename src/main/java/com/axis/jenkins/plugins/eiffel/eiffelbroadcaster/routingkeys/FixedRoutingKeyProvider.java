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
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * A {@link RoutingKeyProvider} implementation that always produces
 * the same configurable fixed string regardless of the event input.
 */
public class FixedRoutingKeyProvider extends RoutingKeyProvider {
    @NonNull
    private String fixedRoutingKey;

    @DataBoundConstructor
    public FixedRoutingKeyProvider(@NonNull String fixedRoutingKey) {
        this.fixedRoutingKey = fixedRoutingKey;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public String getRoutingKey(final EiffelEvent event) {
        return getFixedRoutingKey();
    }

    @NonNull
    public String getFixedRoutingKey() {
        return fixedRoutingKey;
    }

    @DataBoundSetter
    public void setFixedRoutingKey(@NonNull String fixedRoutingKey) {
        this.fixedRoutingKey = fixedRoutingKey;
    }

    @Override
    public Descriptor<RoutingKeyProvider> getDescriptor() {
        return Jenkins.get().getDescriptorByType(FixedRoutingKeyProvider.FixedRoutingKeyProviderDescriptor.class);
    }

    /** Descriptor for {@link FixedRoutingKeyProvider}. */
    @Extension
    public static class FixedRoutingKeyProviderDescriptor extends RoutingKeyProviderDescriptor {
        public FormValidation doCheckFixedRoutingKey(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("The routing key must be a non-empty string.");
            }
            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Fixed string";
        }
    }
}
