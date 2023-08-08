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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A placeholder representation of an arbitrary Eiffel event that doesn't have a dedicated POJO class defined
 * in this package. This class behaves exactly like the specific classes except that the event's <code>data</code>
 * attribute is just saved as a {@link JsonNode}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(using = JsonDeserializer.None.class)
public class GenericEiffelEvent extends EiffelEvent {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private JsonNode data;

    public GenericEiffelEvent(@JsonProperty("meta") final EiffelEvent.Meta meta,
                              @JsonProperty("data") final JsonNode data,
                              @JsonProperty("links") final List<Link> links) {
        super(meta.getType(), meta.getVersion());
        this.getMeta().setId(meta.getId());
        this.getMeta().setTime(meta.getTime());
        this.getMeta().getTags().addAll(meta.getTags());
        this.getMeta().setSource(meta.getSource());
        this.setData(data);
        if (links != null) {
            this.getLinks().addAll(links);
        }
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GenericEiffelEvent that = (GenericEiffelEvent) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("data", data)
                .append("links", getLinks())
                .append("meta", getMeta())
                .toString();
    }
}
