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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * A {@link Configurator} implementation that takes care of CasC serialization/deserialization
 * of {@link SepiaRoutingKeyProvider} objects. CasC tries very hard to not serialize any default
 * values or empty objects, so unless you've set the optional Tag field to a non-empty value it
 * won't serialize the SepiaRoutingKeyProvider object at all, causing us to lose information about
 * which class was chosen as the routing key provider. This configurator works around this by
 * always adding a dummy key/value pair during serialization.
 */
@Extension(optional = true)
public class SepiaRoutingKeyProviderConfigurator extends BaseConfigurator<SepiaRoutingKeyProvider> {
    /** The name of the configuration key used for {@link SepiaRoutingKeyProvider#getTag()}. */
    private static final String KEY_TAG = "tag";

    /** The name of the configuration key used for the dummy value used to avoid optimization. */
    private static final String KEY_DUMMY = "dummy";

    @Override
    protected SepiaRoutingKeyProvider instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        SepiaRoutingKeyProvider provider = new SepiaRoutingKeyProvider();
        if (mapping == null) {
            return provider;
        }
        provider.setTag(mapping.get(KEY_TAG) != null ? mapping.getScalarValue(KEY_TAG) : "");
        return provider;
    }

    @Override
    public boolean canConfigure(Class clazz) {
        return clazz == SepiaRoutingKeyProvider.class;
    }

    @CheckForNull
    @Override
    public CNode describe(SepiaRoutingKeyProvider instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        mapping.put(KEY_DUMMY, "dummy value to make JCasC behave the way we want");
        mapping.put(KEY_TAG, StringUtils.defaultIfBlank(instance.getTag(), ""));
        return mapping;
    }

    @Override
    public Class<SepiaRoutingKeyProvider> getTarget() {
        return SepiaRoutingKeyProvider.class;
    }

    @NonNull
    @Override
    public List<Configurator<SepiaRoutingKeyProvider>> getConfigurators(ConfigurationContext context) {
        return Collections.singletonList(this);
    }
}
