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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactPublishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Functions;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import jenkins.util.VirtualFile;

/**
 * Transforms an {@link EiffelArtifactCreatedEvent} with one or more files declared into an
 * {@link EiffelArtifactPublishedEvent} that contains the URIs to the Jenkins artifacts that
 * correspond to the Eiffel artifact's files.
 */
public class EiffelArtifactPublisher {
    private final VirtualFile artifactRoot;
    private final EiffelEvent contextEvent;
    private final URI runURI;

    /**
     * Constructs a new object instance.
     *
     * @param contextEvent the event that the {@link EiffelArtifactPublishedEvent} should link to with a CONTEXT link
     * @param runURI the {@link URI} of the {@link hudson.model.Run} that contains the files for the artifact
     * @param artifactRoot a {@link VirtualFile} that represents the Jenkins artifact root directory
     */
    public EiffelArtifactPublisher(@NonNull final EiffelEvent contextEvent,
                                   @NonNull final URI runURI,
                                   @NonNull final VirtualFile artifactRoot) {
        this.contextEvent = contextEvent;
        this.runURI = runURI;
        this.artifactRoot = artifactRoot;
    }

    /**
     * Prepares a {@link EiffelArtifactPublishedEvent} that's ready to be sent.
     * @param creationEvent the {@link EiffelArtifactCreatedEvent} that should be published
     * @throws EmptyArtifactException when the {@link EiffelArtifactCreatedEvent} doesn't contain any files
     *                                in <tt>data.fileInformation</tt>
     * @throws IOException when there was a problem correlating the files in the {@link EiffelArtifactCreatedEvent}
     *                     with the files in the {@link hudson.model.Run}'s artifacts
     * @throws MissingArtifactException when one or more of the files listed in
     *                                  the {@link EiffelArtifactCreatedEvent}'s <tt>data.fileInformation</tt> array
     *                                  doesn't exist in Jenkins's artifact directory
     * @throws URISyntaxException when an error occurred when trying to construct a URI to an artifact file
     */
    public EiffelArtifactPublishedEvent prepareEvent(@NonNull final EiffelArtifactCreatedEvent creationEvent)
            throws EmptyArtifactException, IOException, MissingArtifactException, URISyntaxException {
        // It's okay for an ArtC to not have any files in its data.fileInformation array but if we're
        // explicitly asked to emit an ArtP for the artifact and we can't do that it's reasonable to fail.
        if (creationEvent.getData().getFileInformation().isEmpty()) {
            throw new EmptyArtifactException(creationEvent);
        }

        // Before creating an ArtP event, verify that all files specified in the ArtC (data.fileInformation)
        // actually exist as Jenkins artifacts for the Run in question.
        SortedSet<String> missingArtifacts = new TreeSet<>();
        List<String> artifactFilenames = creationEvent.getData().getFileInformation().stream()
                .map(EiffelArtifactCreatedEvent.Data.FileInformation::getName)
                .collect(Collectors.toList());
        for (String filename : artifactFilenames) {
            if (!artifactRoot.child(filename).isFile() ) {
                missingArtifacts.add(filename);
            }
        }
        if (!missingArtifacts.isEmpty()) {
            throw new MissingArtifactException(creationEvent, missingArtifacts);
        }

        // Sanity check okay, continue with the construction of the ArtP event.
        EiffelArtifactPublishedEvent publishEvent = new EiffelArtifactPublishedEvent(creationEvent.getMeta().getId());
        publishEvent.getLinks().add(
                new EiffelEvent.Link(EiffelEvent.Link.Type.CONTEXT, contextEvent.getMeta().getId()));
        for (EiffelArtifactCreatedEvent.Data.FileInformation fileInfo : creationEvent.getData().getFileInformation()) {
            EiffelArtifactPublishedEvent.Data.Location location = new EiffelArtifactPublishedEvent.Data.Location(
                    EiffelArtifactPublishedEvent.Data.Location.Type.PLAIN,
                    new URI(Functions.joinPath(runURI.toString(), "artifact", fileInfo.getName())));
            location.setName(fileInfo.getName());
            publishEvent.getData().getLocations().add(location);
        }
        return publishEvent;
    }
}
