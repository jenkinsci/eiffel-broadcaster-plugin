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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactPublishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import jenkins.util.VirtualFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class EiffelArtifactPublisherTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test(expected = EmptyArtifactException.class)
    public void testPrepareEvent_WithNoFilesInArtC() throws Exception {
        var jobURI = new URI("http://jenkins/job/MyJob/");
        var artC = new EiffelArtifactCreatedEvent("pkg:generic/foo");
        var actT = new EiffelActivityTriggeredEvent("dummy activity name");
        var publisher = new EiffelArtifactPublisher(actT, jobURI, VirtualFile.forFile(tempDir.getRoot()));

        publisher.prepareEvent(artC);
    }

    @Test
    public void testPrepareEvent_HasExpectedLinks() throws Exception {
        var files = new ArtifactFiles("filename.zip");
        var jobURI = new URI("http://jenkins/job/MyJob/");
        var artC = new EiffelArtifactCreatedEvent("pkg:generic/foo");
        files.addFilesToEvent(artC);
        var actT = new EiffelActivityTriggeredEvent("dummy activity name");
        var publisher = new EiffelArtifactPublisher(actT, jobURI, VirtualFile.forFile(tempDir.getRoot()));

        var artP = publisher.prepareEvent(artC);

        assertThat(artP, linksTo(actT, EiffelEvent.Link.Type.CONTEXT));
        assertThat(artP, linksTo(artC, EiffelEvent.Link.Type.ARTIFACT));
    }

    @Test
    public void testPrepareEvent_WithFilesMatchingArtC() throws Exception {
        var files = new ArtifactFiles("filename.zip", "subdir/filename2.zip");
        var jobURI = new URI("http://jenkins/job/MyJob/");
        var artC = new EiffelArtifactCreatedEvent("pkg:generic/foo");
        files.addFilesToEvent(artC);
        var actT = new EiffelActivityTriggeredEvent("dummy activity name");
        var publisher = new EiffelArtifactPublisher(actT, jobURI, VirtualFile.forFile(tempDir.getRoot()));

        var artP = publisher.prepareEvent(artC);

        assertThat(artP.getData().getLocations(), containsInAnyOrder(files.getLocations(jobURI).toArray()));
    }

    @Test(expected = MissingArtifactException.class)
    public void testPrepareEvent_WithMissingFile() throws Exception {
        var files = new ArtifactFiles("filename.zip");
        var jobURI = new URI("http://jenkins/job/MyJob/");
        var artC = new EiffelArtifactCreatedEvent("pkg:generic/foo");
        // Add an extra artifact file directly to the event. This won't have a physical counterpart in tempDir.
        artC.getData().getFileInformation().add(
                new EiffelArtifactCreatedEvent.Data.FileInformation("otherfile.txt"));
        files.addFilesToEvent(artC);
        var actT = new EiffelActivityTriggeredEvent("dummy activity name");
        var publisher = new EiffelArtifactPublisher(actT, jobURI, VirtualFile.forFile(tempDir.getRoot()));

        publisher.prepareEvent(artC);
    }

    /**
     * Helper class for creating files in {@link EiffelArtifactPublisherTest#tempDir} and transforming
     * the collection of filenames in various ways needed by the tests.
     */
    private class ArtifactFiles {
        private List<String> filenames = new ArrayList<>();

        public ArtifactFiles(@NonNull final String... filenames) throws IOException {
            for (String filename : filenames) {
                this.filenames.add(filename);
                var file = new File(tempDir.getRoot(), filename);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }

        public void addFilesToEvent(@NonNull final EiffelArtifactCreatedEvent event) {
            for (var filename : filenames) {
                event.getData().getFileInformation().add(new EiffelArtifactCreatedEvent.Data.FileInformation(filename));
            }
        }

        public List<EiffelArtifactPublishedEvent.Data.Location> getLocations(@NonNull final URI baseURI) {
            var result = new ArrayList<EiffelArtifactPublishedEvent.Data.Location>();
            for (var filename : filenames) {
                var location = new EiffelArtifactPublishedEvent.Data.Location(
                        EiffelArtifactPublishedEvent.Data.Location.Type.PLAIN, baseURI.resolve("artifact/" + filename));
                location.setName(filename);
                result.add(location);
            }
            return result;
        }
    }
}
