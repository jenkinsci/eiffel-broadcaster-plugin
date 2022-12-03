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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Wraps the name and version of an Eiffel event in a single hashable entity that e.g. can be used a map key. */
class EventVersionKey {
    @NonNull
    private String eventName;

    @NonNull
    private String eventVersion;

    public EventVersionKey(@NonNull String eventName, @NonNull String eventVersion) {
        this.eventName = eventName;
        this.eventVersion = eventVersion;
    }

    public EventVersionKey(@NonNull EiffelEvent event) {
        this.eventName = event.getMeta().getType();
        this.eventVersion = event.getMeta().getVersion();
    }

    @NonNull
    public String getEventName() {
        return eventName;
    }

    @NonNull
    public String getEventVersion() {
        return eventVersion;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("eventName", eventName)
                .append("eventVersion", eventVersion)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventVersionKey that = (EventVersionKey) o;
        return eventName.equals(that.eventName) && eventVersion.equals(that.eventVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventName, eventVersion);
    }
}
