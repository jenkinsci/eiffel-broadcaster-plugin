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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Raised when an {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactPublishedEvent}
 * can't be created from an {@link EiffelArtifactCreatedEvent} because one or more files declare in the latter
 * doesn't exist as a Jenkins artifact for the build, making it impossible to create URIs for the Eiffel
 * artifact's files.
 */
public class MissingArtifactException extends Exception {
    public MissingArtifactException(@Nonnull final EiffelArtifactCreatedEvent creationEvent,
                                    @Nonnull final Collection<String> missingArtifacts) {
        super(String.format(
                "Unable to send EiffelArtifactPublishedEvent for Eiffel artifact with id %s and " +
                        "identity %s because the following files included in the Eiffel artifact " +
                        "aren't available as Jenkins artifacts in this build: %s",
                creationEvent.getMeta().getId(), creationEvent.getData().getIdentity(), missingArtifacts));
    }
}
