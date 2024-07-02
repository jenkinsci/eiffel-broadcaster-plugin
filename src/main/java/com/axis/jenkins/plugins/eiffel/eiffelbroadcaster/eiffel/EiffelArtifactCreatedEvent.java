/**
 The MIT License

 Copyright 2021-2024 Axis Communications AB.

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
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A Java representation of an Eiffel event of the
 * <a href="https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelArtifactCreatedEvent.md">
 * EiffelArtifactCreatedEvent</a> (ArtC) kind.
 *
 * See the Eiffel event documentation for more on the meaning of the attributes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class EiffelArtifactCreatedEvent extends EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Data data = new Data();

    public EiffelArtifactCreatedEvent(final String version) {
        super(EiffelArtifactCreatedEvent.class.getSimpleName(), version);
    }

    /** Package-private constructor needed for JSON deserialization. */
    EiffelArtifactCreatedEvent() {
        this("");
    }

    public EiffelArtifactCreatedEvent.Data getData() {
        return data;
    }

    public void setData(EiffelArtifactCreatedEvent.Data data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EiffelArtifactCreatedEvent that = (EiffelArtifactCreatedEvent) o;
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
        private String identity;

        private List<FileInformation> fileInformation = new ArrayList<>();

        private String buildCommand;

        private RequiresImplementation requiresImplementation;

        @JsonProperty("implements")
        private List<String> implementsPurls = new ArrayList<>();

        private List<String> dependsOn = new ArrayList<>();;

        private String name;

        public String getIdentity() {
            return identity;
        }

        public void setIdentity(String identity) {
            this.identity = identity;
        }

        public List<FileInformation> getFileInformation() {
            return fileInformation;
        }

        public String getBuildCommand() {
            return buildCommand;
        }

        public void setBuildCommand(String buildCommand) {
            this.buildCommand = buildCommand;
        }

        public RequiresImplementation getRequiresImplementation() {
            return requiresImplementation;
        }

        public void setRequiresImplementation(RequiresImplementation requiresImplementation) {
            this.requiresImplementation = requiresImplementation;
        }

        public List<String> getImplementsPurls() {
            return implementsPurls;
        }

        public List<String> getDependsOn() {
            return dependsOn;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public enum RequiresImplementation {
            NONE,
            ANY,
            EXACTLY_ONE,
            AT_LEAST_ONE
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return identity.equals(data.identity) && fileInformation.equals(data.fileInformation) &&
                    Objects.equals(buildCommand, data.buildCommand) &&
                    requiresImplementation == data.requiresImplementation &&
                    implementsPurls.equals(data.implementsPurls) && dependsOn.equals(data.dependsOn) &&
                    Objects.equals(name, data.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identity, fileInformation, buildCommand, requiresImplementation,
                    implementsPurls, dependsOn, name);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("identity", identity)
                    .append("fileInformation", fileInformation)
                    .append("buildCommand", buildCommand)
                    .append("requiresImplementation", requiresImplementation)
                    .append("implementsPurls", implementsPurls)
                    .append("dependsOn", dependsOn)
                    .append("name", name)
                    .toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class FileInformation {
            @JsonInclude(JsonInclude.Include.ALWAYS)
            private String name;

            private IntegrityProtection integrityProtection;

            private List<String> tags = new ArrayList<>();

            public FileInformation(@JsonProperty("name") String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public IntegrityProtection getIntegrityProtection() {
                return integrityProtection;
            }

            public void setIntegrityProtection(IntegrityProtection integrityProtection) {
                this.integrityProtection = integrityProtection;
            }

            public List<String> getTags() {
                return tags;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                FileInformation that = (FileInformation) o;
                return name.equals(that.name) &&
                        Objects.equals(integrityProtection, that.integrityProtection) &&
                        tags.equals(that.tags);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, integrityProtection, tags);
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("name", name)
                        .append("integrityProtection", integrityProtection)
                        .append("tags", tags)
                        .toString();
            }

            public static class IntegrityProtection {
                @JsonInclude(JsonInclude.Include.ALWAYS)
                private Alg alg;

                @JsonInclude(JsonInclude.Include.ALWAYS)
                private String digest;

                public IntegrityProtection(@JsonProperty("alg") Alg alg, @JsonProperty("digest") String digest) {
                    this.alg = alg;
                    this.digest = digest;
                }

                public Alg getAlg() {
                    return alg;
                }

                public void setAlg(Alg alg) {
                    this.alg = alg;
                }

                public String getDigest() {
                    return digest;
                }

                public void setDigest(String digest) {
                    this.digest = digest;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    IntegrityProtection that = (IntegrityProtection) o;
                    return alg == that.alg && digest.equals(that.digest);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(alg, digest);
                }

                @Override
                public String toString() {
                    return new ToStringBuilder(this)
                            .append("alg", alg)
                            .append("digest", digest)
                            .toString();
                }

                public enum Alg {
                    SHA224("SHA-224"),
                    SHA256("SHA-256"),
                    SHA384("SHA-384"),
                    SHA512("SHA-512"),
                    SHA512_224("SHA-512/224"),
                    SHA512_256("SHA-512/256");

                    @JsonValue
                    private final String algName;

                    Alg(String algName) {
                        this.algName = algName;
                    }

                    @Override
                    public String toString() {
                        return algName;
                    }
                }
            }
        }
    }
}
