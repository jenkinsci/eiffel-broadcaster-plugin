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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GenericEiffelEventTest {
    @Test
    public void testJsonDeserialization() throws IOException {
        EiffelEvent event = new ObjectMapper().readValue(
                getClass().getResourceAsStream("EiffelCompositionDefinedEvent.json"), EiffelEvent.class);

        // Basic metadata assertions. Make sure to include value apart from
        // the type and version since they're currently handled in different ways.
        assertThat(event.getMeta().getType(), is("EiffelCompositionDefinedEvent"));
        assertThat(event.getMeta().getVersion(), is("3.0.0"));
        assertThat(event.getMeta().getId(), is(UUID.fromString("aaaaaaaa-bbbb-5ccc-8ddd-eeeeeeeeeee0")));
        assertThat(event.getMeta().getTime(), is(1234567890L));

        // Links. Let's not check every element for equality. Correct length and
        // equality of a single sample is enough.
        assertThat(event.getLinks(), hasSize(5));
        assertThat(event.getLinks(),
                hasItem(new EiffelEvent.Link(EiffelEvent.Link.Type.ELEMENT,
                        UUID.fromString("aaaaaaaa-bbbb-5ccc-8ddd-eeeeeeeeeee1"))));

        // Make sure we get the expected concrete generic type and that its data is correct.
        assertThat(event, instanceOf(GenericEiffelEvent.class));
        GenericEiffelEvent genericEvent = (GenericEiffelEvent) event;
        assertThat(genericEvent.getData().path("name").asText(), is("myCompositionName"));
        assertThat(genericEvent.getData().path("version").asText(), is("42.0.7"));
    }
}
