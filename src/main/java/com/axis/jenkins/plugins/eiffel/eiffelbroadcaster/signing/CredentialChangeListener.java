/**
 The MIT License

 Copyright 2024 Axis Communications AB.

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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.signing;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to changes in saveable objects and clears the cache of signing keys
 * via {@link SigningKeyCache#clear()} when it's the XML file of
 * the {@link SystemCredentialsProvider} that changed.
 */
@Extension
public class CredentialChangeListener extends SaveableListener {
    private static final Logger logger = LoggerFactory.getLogger(CredentialChangeListener.class);

    /** Called when a change is made to a {@link Saveable} object. */
    public void onChange(Saveable o, XmlFile file) {
        if (!SystemCredentialsProvider.getConfigFile().getFile().equals(file.getFile())) {
            return;
        }
        logger.info("Credentials changed, clearing cache");
        SigningKeyCache.getInstance().clear();
    }
}
