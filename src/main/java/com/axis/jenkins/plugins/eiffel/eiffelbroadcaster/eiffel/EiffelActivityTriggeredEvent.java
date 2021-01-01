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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A Java representation of an Eiffel event of the
 * <a href="https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityTriggeredEvent.md">
 * EiffelActivityTriggeredEvent</a> (ActT) kind.
 *
 * See the Eiffel event documentation for more on the meaning of the attributes.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EiffelActivityTriggeredEvent extends EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final Data data;

    public EiffelActivityTriggeredEvent(@JsonProperty("data") Data data) {
        super("EiffelActivityTriggeredEvent", "1.1.0");
        this.data = data;
    }

    public EiffelActivityTriggeredEvent(String name) {
        this(new EiffelActivityTriggeredEvent.Data(name));
    }

    public Data getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EiffelActivityTriggeredEvent that = (EiffelActivityTriggeredEvent) o;
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
        private String name;

        private final List<String> categories = new ArrayList<>();

        private final List<Trigger> triggers = new ArrayList<>();

        private ExecutionType executionType;

        public List<String> getCategories() {
            return categories;
        }

        public Data(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ExecutionType getExecutionType() {
            return executionType;
        }

        public void setExecutionType(ExecutionType executionType) {
            this.executionType = executionType;
        }

        public List<Trigger> getTriggers() {
            return triggers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return name.equals(data.name) &&
                    categories.equals(data.categories) &&
                    triggers.equals(data.triggers) &&
                    executionType == data.executionType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, categories, triggers, executionType);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("name", name)
                    .append("categories", categories)
                    .append("triggers", triggers)
                    .append("executionType", executionType)
                    .toString();
        }

        public enum ExecutionType {
            MANUAL,
            SEMI_AUTOMATED,
            AUTOMATED,
            OTHER
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Trigger {
            @JsonInclude(JsonInclude.Include.ALWAYS)
            private Type type;

            private String description;

            public Trigger(@JsonProperty("type") Type type) {
                this.type = type;
            }

            public Type getType() {
                return type;
            }

            public void setType(Type type) {
                this.type = type;
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
                Trigger trigger = (Trigger) o;
                return type == trigger.type &&
                        Objects.equals(description, trigger.description);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, description);
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("type", type)
                        .append("description", description)
                        .toString();
            }

            public enum Type {
                MANUAL,
                EIFFEL_EVENT,
                SOURCE_CHANGE,
                TIMER,
                OTHER
            }
        }
    }
}
