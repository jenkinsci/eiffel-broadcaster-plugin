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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Extends {@link JenkinsRule} to also clear the
 * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.SourceProvider}
 * stored in a static attribute in {@link EiffelEvent}. It gets set to an instance
 * of {@link JenkinsSourceProvider} when the plugin is run in a Jenkins instance,
 * but that class requires a Jenkins instance to be available so subsequently run
 * tests will fail unless we clear the source provider.
 *
 * We should refactor the code to not use a static attribute like this, but until
 * that's done this'll have to do.
 */
public class RestoreSourceProviderJenkinsRule extends JenkinsRule {
    @Override
    public void after() throws Exception {
        super.after();
        EiffelEvent.setSourceProvider(null);
    }
}
