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
 * <a href="https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityStartedEvent.md">
 * EiffelActivityStartedEvent</a> (ActF) kind.
 *
 * See the Eiffel event documentation for more on the meaning of the attributes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class EiffelActivityFinishedEvent extends EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final Data data;

    public EiffelActivityFinishedEvent(@JsonProperty("data") Data data) {
        super("EiffelActivityFinishedEvent", "3.0.0");
        this.data = data;
    }

    public EiffelActivityFinishedEvent(Data.Outcome outcome, UUID activityID) {
        this(new EiffelActivityFinishedEvent.Data(outcome));
        getLinks().add(new EiffelEvent.Link(Link.Type.ACTIVITY_EXECUTION, activityID));
    }

    public Data getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EiffelActivityFinishedEvent that = (EiffelActivityFinishedEvent) o;
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
        private Outcome outcome;

        private final List<PersistentLogs> persistentLogs = new ArrayList<>();

        public Data(@JsonProperty("outcome") Outcome outcome) {
            this.outcome = outcome;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public void setOutcome(Outcome outcome) {
            this.outcome = outcome;
        }

        public List<PersistentLogs> getPersistentLogs() {
            return persistentLogs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return Objects.equals(outcome, data.outcome) &&
                    persistentLogs.equals(data.persistentLogs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outcome, persistentLogs);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("outcome", outcome)
                    .append("persistentLogs", persistentLogs)
                    .toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Outcome {
            @JsonInclude(JsonInclude.Include.ALWAYS)
            private Conclusion conclusion;

            private String description;

            public Outcome(@JsonProperty("conclusion") Conclusion conclusion) {
                this.conclusion = conclusion;
            }

            public Conclusion getConclusion() {
                return conclusion;
            }

            public void setConclusion(Conclusion conclusion) {
                this.conclusion = conclusion;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Outcome outcome = (Outcome) o;
                return conclusion == outcome.conclusion &&
                        Objects.equals(description, outcome.description);
            }

            @Override
            public int hashCode() {
                return Objects.hash(conclusion, description);
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("conclusion", conclusion)
                        .append("description", description)
                        .toString();
            }

            public enum Conclusion {
                SUCCESSFUL,
                UNSUCCESSFUL,
                FAILED,
                ABORTED,
                TIMED_OUT,
                INCONCLUSIVE
            }
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class PersistentLogs {
            @JsonInclude(JsonInclude.Include.ALWAYS)
            private String name;

            @JsonInclude(JsonInclude.Include.ALWAYS)
            private URI uri;

            public PersistentLogs(@JsonProperty("name") String name, @JsonProperty("uri") URI uri) {
                this.name = name;
                this.uri = uri;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public URI getURI() {
                return uri;
            }

            public void setURI(URI uri) {
                this.uri = uri;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                PersistentLogs persistentLogs = (PersistentLogs) o;
                return name.equals(persistentLogs.name) &&
                        uri.equals(persistentLogs.uri);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, uri);
            }
        }
    }
}
