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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;

/**
 * Maintains an ordered set of events received from the plugin by the mocked {@link MQConnection}.
 * The raw messages are deserialized to {@link EiffelEvent} objects and there are methods
 * to locate an event matching a criterion and pop it from the event set of events. This makes
 * it possible to write tests that aren't tightly coupled to the exact order of events.
 */
public class EventSet {
    private List<EiffelEvent> events = new ArrayList<>();

    /**
     * Initializes a new class instance by deserializing each JSON string in the provided list
     * and collecting the resulting {@link EiffelEvent} objects.
     *
     * @param messages a list of Eiffel events, serialized as JSON strings
     * @throws JsonProcessingException if the JSON deserialization fails, e.g. because
     * the JSON string is malformed or if the mapping of the JSON object to a Java object fails
     */
    public EventSet(List<String> messages) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        for (String message : messages) {
            events.add(mapper.readValue(message, EiffelEvent.class));
        }
    }

    /**
     * Finds and returns all events of a particular class. As opposed to {@link #findNext(Class)}
     * all returned events will be kept in the set.
     *
     * @param clazz the class to look for
     * @return a list of all matching events
     */
    public <T> List<T> all(Class<T> clazz) {
        return events.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    /**
     * Finds the first event of a particular class, pops it from the event set, and returns it.
     *
     * @param clazz the class to look for
     * @return the first matching event
     * @throws java.util.NoSuchElementException if no event of the specified class could be found
     */
    public <T> T findNext(Class<T> clazz) {
        T event = events.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .get();
        events.remove(event);
        return event;
    }

    /**
     * Finds the first event of a particular class that matches a condition, pops it from
     * the event set, and returns it.
     *
     * @param clazz the class to look for
     * @param matcher a {@link Matcher} that will be evaluated against the list of
     * @return the first matching event
     * @throws java.util.NoSuchElementException if no event matching the condition could be found
     */
    public <T> T findNext(Class<T> clazz, Matcher<EiffelEvent> matcher) {
        T event = events.stream()
                .filter(clazz::isInstance)
                .filter(matcher::matches)
                .map(clazz::cast)
                .findFirst()
                .get();
        events.remove(event);
        return event;
    }

    /**
     * Returns whether there are any remaining events in the set.
     *
     * @return true if the event set is empty, otherwise false
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }
}
