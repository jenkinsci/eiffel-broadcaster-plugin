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

/**
 * Describes how the hostname of the Jenkins controller should be determined. This hostname
 * is currently used to populate the <tt>meta.source.host</tt> member of all events.
 */
public enum HostnameSource {
    /** The hostname of the Jenkins controller according to the network stack */
    NETWORK_STACK("The hostname of the Jenkins controller according to the network stack"),

    /** The hostname from the configured URL of the Jenkins controller */
    CONFIGURED_URL("The hostname from the configured URL of the Jenkins controller");

    private final String description;

    HostnameSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
