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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A Java representation of an Eiffel event of the
 * <a href="https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelArtifactPublishedEvent.md">
 * EiffelArtifactPublishedEvent</a> (ArtC) kind.
 *
 * See the Eiffel event documentation for more on the meaning of the attributes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class EiffelArtifactPublishedEvent extends EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final EiffelArtifactPublishedEvent.Data data;

    public EiffelArtifactPublishedEvent() {
        super("EiffelArtifactPublishedEvent", "3.1.0");
        this.data = new Data();
    }

    public EiffelArtifactPublishedEvent(UUID artifactID) {
        this();
        getLinks().add(new Link(Link.Type.ARTIFACT, artifactID));
    }

    public EiffelArtifactPublishedEvent.Data getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EiffelArtifactPublishedEvent that = (EiffelArtifactPublishedEvent) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("links", getLinks())
                .append("meta", getMeta())
                .append("data", data)
                .toString();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Data {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private List<Location> locations = new ArrayList<>();

        public List<Location> getLocations() {
            return locations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return locations.equals(data.locations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(locations);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("locations", locations)
                    .toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Location {
            private Type type;
            private String name;
            private URI uri;

            public Location(@JsonProperty("type") Type type, @JsonProperty("uri") URI uri) {
                this.type = type;
                this.uri = uri;
            }

            public Type getType() {
                return type;
            }

            public void setType(Type type) {
                this.type = type;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public URI getUri() {
                return uri;
            }

            public void setUri(URI uri) {
                this.uri = uri;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Location location = (Location) o;
                return type == location.type && Objects.equals(name, location.name) && uri.equals(location.uri);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, name, uri);
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("type", type)
                        .append("name", name)
                        .append("uri", uri)
                        .toString();
            }

            public enum Type {
                ARTIFACTORY,
                NEXUS,
                PLAIN,
                OTHER
            }
        }
    }
}
