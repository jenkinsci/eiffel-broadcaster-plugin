/**
 The MIT License

 Copyright 2021-2023 Axis Communications AB.

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A base class for Eiffel events that defines the common event attributes (<code>meta</code> and <code>links</code>)
 * and makes it easy for subclasses to add an event-specific <code>data</code> attribute.
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

    @JsonIgnore
    private static SourceProvider sourceProvider;

    public EiffelEvent(String type, String version) {
        this.meta = new Meta(type, version);
        populateSource();
    }

    public List<Link> getLinks() {
        return links;
    }

    public Meta getMeta() {
        return meta;
    }

    /**
     * Provide a {@link SourceProvider} instance that will be request to provide a {@link Meta.Source}
     * object for each event created after that point.
     */
    public static void setSourceProvider(final SourceProvider provider) {
        sourceProvider = provider;
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

    private void populateSource() {
        if (sourceProvider != null) {
            if (getMeta().getSource() == null) {
                getMeta().setSource(new Meta.Source());
            }
            sourceProvider.populateSource(getMeta().getSource());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Link {
        private String domainId;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private UUID target;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Type type;

        public Link(@JsonProperty("type") Type type, @JsonProperty("target") UUID target) {
            this.target = target;
            this.type = type;
        }

        public String getDomainId() {
            return domainId;
        }

        public void setDomainId(String domainId) {
            this.domainId = domainId;
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
            return Objects.equals(domainId, link.domainId) &&
                    target.equals(link.target) &&
                    type.equals(link.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(domainId, target, type);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("domainId", domainId)
                    .append("target", target)
                    .append("type", type)
                    .toString();
        }

        public enum Type {
            ACTIVITY_EXECUTION,
            ARTIFACT,
            BASE,
            CAUSE,
            CHANGE,
            COMPOSITION,
            CONFIGURATION,
            CONTEXT,
            DERESOLVED_ISSUE,
            ELEMENT,
            ENVIRONMENT,
            FAILED_ISSUE,
            FLOW_CONTEXT,
            INCONCLUSIVE_ISSUE,
            IUT,
            MODIFIED_ANNOUNCEMENT,
            ORIGINAL_TRIGGER,
            PARTIALLY_RESOLVED_ISSUE,
            PRECURSOR,
            PREVIOUS_ACTIVITY_EXECUTION,
            PREVIOUS_VERSION,
            RESOLVED_ISSUE,
            REUSED_ARTIFACT,
            RUNTIME_ENVIRONMENT,
            SUB_CONFIDENCE_LEVEL,
            SUBJECT,
            SUCCESSFUL_ISSUE,
            TERC,
            TEST_CASE_EXECUTION,
            TEST_SUITE_EXECUTION,
            VERIFICATION_BASIS
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Meta {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private UUID id = UUID.randomUUID();

        private String schemaUri;

        private Security security;

        private Source source = new Source();

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private long time = Instant.now().toEpochMilli();

        private final List<String> tags = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private String type;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private final String version;

        public Meta(@JsonProperty("type") String type, @JsonProperty("version") String version) {
            if (StringUtils.isBlank(type)) {
                throw new IllegalArgumentException("The event type must be set to a non-empty string");
            }
            if (StringUtils.isBlank(version)) {
                throw new IllegalArgumentException("The event version must be set to a non-empty string");
            }
            this.type = type;
            this.version = version;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getSchemaUri() {
            return schemaUri;
        }

        public void setSchemaUri(String schemaUri) {
            this.schemaUri = schemaUri;
        }

        public Security getSecurity() {
            return security;
        }

        public void setSecurity(Security security) {
            this.security = security;
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

        public void setTime(long time) {
            this.time = time;
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
                    Objects.equals(schemaUri, meta.schemaUri) &&
                    Objects.equals(security, meta.security) &&
                    Objects.equals(source, meta.source) &&
                    tags.equals(meta.tags) &&
                    type.equals(meta.type) &&
                    version.equals(meta.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, schemaUri, source, time, tags, type, version);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("id", id)
                    .append("schemaUri", schemaUri)
                    .append("security", security)
                    .append("source", source)
                    .append("time", time)
                    .append("tags", tags)
                    .append("type", type)
                    .append("version", version)
                    .toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Security {
            @JsonInclude(JsonInclude.Include.ALWAYS)
            private String authorIdentity;

            private IntegrityProtection integrityProtection;

            public Security(@JsonProperty("authorIdentity") String authorIdentity) {
                this.authorIdentity = authorIdentity;
            }

            public String getAuthorIdentity() {
                return authorIdentity;
            }

            public void setAuthorIdentity(String authorIdentity) {
                this.authorIdentity = authorIdentity;
            }

            public IntegrityProtection getIntegrityProtection() {
                return integrityProtection;
            }

            public void setIntegrityProtection(IntegrityProtection integrityProtection) {
                this.integrityProtection = integrityProtection;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Security security = (Security) o;
                return authorIdentity.equals(security.authorIdentity) &&
                        Objects.equals(integrityProtection, security.integrityProtection);
            }

            @Override
            public int hashCode() {
                return Objects.hash(authorIdentity, integrityProtection);
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("authorIdentity", authorIdentity)
                        .append("integrityProtection", integrityProtection)
                        .toString();
            }

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            public static class IntegrityProtection {
                @JsonInclude(JsonInclude.Include.ALWAYS)
                private String signature;

                @JsonInclude(JsonInclude.Include.ALWAYS)
                private Alg alg;

                private String publicKey;

                private List<SequenceProtection> sequenceProtection = new ArrayList<>();

                public IntegrityProtection(@JsonProperty("signature") String signature, @JsonProperty("alg") Alg alg) {
                    this.signature = signature;
                    this.alg = alg;
                }

                public String getSignature() {
                    return signature;
                }

                public void setSignature(String signature) {
                    this.signature = signature;
                }

                public Alg getAlg() {
                    return alg;
                }

                public void setAlg(Alg alg) {
                    this.alg = alg;
                }

                public String getPublicKey() {
                    return publicKey;
                }

                public void setPublicKey(String publicKey) {
                    this.publicKey = publicKey;
                }

                public List<SequenceProtection> getSequenceProtection() {
                    return sequenceProtection;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    IntegrityProtection that = (IntegrityProtection) o;
                    return signature.equals(that.signature) &&
                            alg == that.alg &&
                            Objects.equals(publicKey, that.publicKey) &&
                            Objects.equals(sequenceProtection, that.sequenceProtection);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(signature, alg, publicKey, sequenceProtection);
                }

                @Override
                public String toString() {
                    return new ToStringBuilder(this)
                            .append("signature", signature)
                            .append("alg", alg)
                            .append("publicKey", publicKey)
                            .append("sequenceProtection", sequenceProtection)
                            .toString();
                }

                public enum Alg {
                    HS256,
                    HS384,
                    HS512,
                    RS256,
                    RS384,
                    RS512,
                    ES256,
                    ES384,
                    ES512,
                    PS256,
                    PS384,
                    PS512;
                }

                @JsonInclude(JsonInclude.Include.ALWAYS)
                public static class SequenceProtection {
                    private String sequenceName;

                    private int position;

                    public SequenceProtection(@JsonProperty("sequenceName") String sequenceName,
                                              @JsonProperty("position") int position) {
                        this.sequenceName = sequenceName;
                        this.position = position;
                    }

                    public String getSequenceName() {
                        return sequenceName;
                    }

                    public void setSequenceName(String sequenceName) {
                        this.sequenceName = sequenceName;
                    }

                    public int getPosition() {
                        return position;
                    }

                    public void setPosition(int position) {
                        this.position = position;
                    }

                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        SequenceProtection that = (SequenceProtection) o;
                        return position == that.position && sequenceName.equals(that.sequenceName);
                    }

                    @Override
                    public int hashCode() {
                        return Objects.hash(sequenceName, position);
                    }

                    @Override
                    public String toString() {
                        return new ToStringBuilder(this)
                                .append("sequenceName", sequenceName)
                                .append("position", position)
                                .toString();
                    }
                }
            }
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Source {
            private String domainId;

            private String host;

            private String name;

            private String serializer;

            private URI uri;

            public Source() { }

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
     * the payload's <code>meta.type</code> member.
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
            // with the same name as the event type. If that fails, deserialize
            // into GenericEiffelEvent where the data attribute is a JsonNode.
            EiffelEvent event;
            try {
                event = (EiffelEvent) p.getCodec().treeToValue(node,
                        Class.forName(getClass().getPackage().getName() + "." + eventType));
            } catch (ClassNotFoundException e) {
                event = p.getCodec().treeToValue(node, GenericEiffelEvent.class);
            }
            event.populateSource();
            return event;
        }
    }
}
