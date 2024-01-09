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

import hudson.Extension;
import hudson.model.Job;
import java.util.List;
import jenkins.model.OptionalJobProperty;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * An {@link OptionalJobProperty} that can be used to populate additional members in
 * in the Eiffel activity events emitted for the builds of a job.
 * <p>
 * Also defines a <code>eiffelActivity</code> pipeline step that can be used to set
 * the properties via pipeline code:
 * <pre>
 * properties([
 *     eiffelActivity(categories: ['a', 'b'])
 * ])
 * </pre>
 */
public class EiffelActivityJobProperty extends OptionalJobProperty<Job<?, ?>> {
    private List<String> categories;

    @DataBoundConstructor
    public EiffelActivityJobProperty(final List<String> categories) {
        this.categories = categories;
    }

    public List<String> getCategories() {
        return categories;
    }

    @DataBoundSetter
    public void setCategories(final List<String> categories) {
        this.categories = categories;
    }

    @Extension
    @Symbol("eiffelActivity")
    public static class EiffelActivityJobPropertyDescriptor extends OptionalJobPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "Eiffel activity attributes";
        }

        @Override
        public OptionalJobProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // The form uses a textarea for the categories but we split out the lines and store
            // them individually in a list so we need to override the Stapler's default behavior.
            var categories = Util.getLinesInString(formData.getString("categories"));
            return new EiffelActivityJobProperty(categories);
        }
    }
}
