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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A base class for Eiffel events that defines the common event attributes (<tt>meta</tt> and <tt>links</tt>)
 * and makes it easy for subclasses to add an event-specific <tt>data</tt> attribute.
 *
 * See the Eiffel event documentation for each concrete event type for more on the meaning of the attributes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(using = EiffelEvent.Deserializer.class)
public class EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final List<Link> links = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final Meta meta;

    public EiffelEvent(String type, String version) {
        this.meta = new Meta(type, version);
    }

    public List<Link> getLinks() {
        return links;
    }

    public Meta getMeta() {
        return meta;
    }

    public String toJSON() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EiffelEvent that = (EiffelEvent) o;
        return links.equals(that.links) &&
                meta.equals(that.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(links, meta);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("links", links)
                .append("meta", meta)
                .toString();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Link {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private UUID target;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Type type;

        public Link(@JsonProperty("type") Type type, @JsonProperty("target") UUID target) {
            this.target = target;
            this.type = type;
        }

        public UUID getTarget() {
            return target;
        }

        public void setTarget(UUID target) {
            this.target = target;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Link link = (Link) o;
            return target.equals(link.target) &&
                    type.equals(link.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, type);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("target", target)
                    .append("type", type)
                    .toString();
        }

        public enum Type {
            ACTIVITY_EXECUTION,
            CAUSE,
            CONTEXT,
            FLOW_CONTEXT,
            PREVIOUS_ACTIVITY_EXECUTION
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Meta {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private final UUID id = UUID.randomUUID();

        private Source source = new Source();

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private final long time = Instant.now().toEpochMilli();

        private final List<String> tags = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private String type;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private final String version;

        public Meta(@JsonProperty("type") String type, @JsonProperty("version") String version) {
            this.type = type;
            this.version = version;
        }

        public UUID getId() {
            return id;
        }

        public Source getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = source;
        }

        public long getTime() {
            return time;
        }

        public List<String> getTags() {
            return tags;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Meta meta = (Meta) o;
            return time == meta.time &&
                    id.equals(meta.id) &&
                    Objects.equals(source, meta.source) &&
                    tags.equals(meta.tags) &&
                    type.equals(meta.type) &&
                    version.equals(meta.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, source, time, tags, type, version);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("source", source)
                    .append("time", time)
                    .append("tags", tags)
                    .append("type", type)
                    .append("version", version)
                    .toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Source {
            public final static String DEFAULT_NAME = "JENKINS_EIFFEL_BROADCASTER";

            private String domainId;

            private String host;

            private String name;

            private String serializer;

            private URI uri;

            public Source() {
                this.name = DEFAULT_NAME;
            }

            public String getDomainId() {
                return domainId;
            }

            public void setDomainId(String domainId) {
                this.domainId = domainId;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getSerializer() {
                return serializer;
            }

            public void setSerializer(String serializer) {
                this.serializer = serializer;
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
                Source source = (Source) o;
                return Objects.equals(domainId, source.domainId) &&
                        Objects.equals(host, source.host) &&
                        Objects.equals(name, source.name) &&
                        Objects.equals(serializer, source.serializer) &&
                        Objects.equals(uri, source.uri);
            }

            @Override
            public int hashCode() {
                return Objects.hash(domainId, host, name, serializer, uri);
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("domainId", domainId)
                        .append("host", host)
                        .append("name", name)
                        .append("serializer", serializer)
                        .append("uri", uri)
                        .toString();
            }
        }
    }

    /**
     * Deserializes a JSON payload into an instance of a subclass of {@link EiffelEvent} based on
     * the payload's <tt>meta.type</tt> member.
     */
    static class Deserializer extends StdDeserializer<Object> {
        protected Deserializer() {
            super(EiffelEvent.class);
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            TreeNode node = p.readValueAsTree();

            // Obtain event type from the meta.type member.
            TreeNode typeNode = node.path("meta").path("type");
            if (typeNode.isMissingNode()) {
                throw new InvalidJsonPayloadException(
                        "Unable to figure out type of Eiffel event: no 'meta.type' key found");
            }
            String eventType = p.getCodec().treeToValue(typeNode, String.class);

            // Attempt to deserialize the TreeNode into a class in this package
            // with the same name as the event type.
            try {
                return p.getCodec().treeToValue(node,
                        Class.forName(getClass().getPackage().getName() + "." + eventType));
            } catch (ClassNotFoundException e) {
                throw new UnsupportedEventTypeException("Unsupported event type: " + eventType, e);
            }
        }
    }
}
