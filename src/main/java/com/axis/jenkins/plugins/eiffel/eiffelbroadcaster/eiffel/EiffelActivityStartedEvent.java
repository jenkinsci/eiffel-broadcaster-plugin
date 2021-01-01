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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A Java representation of an Eiffel event of the
 * <a href="https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityStartedEvent.md">
 * EiffelActivityStartedEvent</a> (ActS) kind.
 *
 * See the Eiffel event documentation for more on the meaning of the attributes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EiffelActivityStartedEvent extends EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final Data data;

    public EiffelActivityStartedEvent() {
        super("EiffelActivityStartedEvent", "1.1.0");
        this.data = new Data();
    }

    public EiffelActivityStartedEvent(UUID activityID) {
        this();
        getLinks().add(new Link(Link.Type.ACTIVITY_EXECUTION, activityID));
    }

    public Data getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EiffelActivityStartedEvent that = (EiffelActivityStartedEvent) o;
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
        private URI executionUri;

        private final List<LiveLogs> liveLogs = new ArrayList<>();

        public URI getExecutionUri() {
            return executionUri;
        }

        public void setExecutionUri(URI executionUri) {
            this.executionUri = executionUri;
        }

        public List<LiveLogs> getLiveLogs() {
            return liveLogs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return Objects.equals(executionUri, data.executionUri) &&
                    liveLogs.equals(data.liveLogs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(executionUri, liveLogs);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("executionUri", executionUri)
                    .append("liveLogs", liveLogs)
                    .toString();
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class LiveLogs {
            @JsonInclude(JsonInclude.Include.ALWAYS)
            private String name;

            @JsonInclude(JsonInclude.Include.ALWAYS)
            private URI uri;

            public LiveLogs(@JsonProperty("name") String name, @JsonProperty("uri") URI uri) {
                this.name = name;
                this.uri = uri;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                LiveLogs liveLogs = (LiveLogs) o;
                return name.equals(liveLogs.name) &&
                        uri.equals(liveLogs.uri);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, uri);
            }
        }
    }
}
