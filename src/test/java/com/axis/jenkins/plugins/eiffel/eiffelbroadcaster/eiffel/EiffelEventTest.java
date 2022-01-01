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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class EiffelEventTest {
    @Test
    public void testJsonDeserialization_ChoosesCorrectSubclass() throws JsonProcessingException {
        // We instantiate an arbitrary concrete EiffelEvent subclass and make sure that when we
        // deserialize the string back into an EiffelEvent we get an instance of the same class.
        EiffelActivityTriggeredEvent originalEvent = new EiffelActivityTriggeredEvent("activity name");
        EiffelEvent deserializedEvent = new ObjectMapper().readValue(originalEvent.toJSON(), EiffelEvent.class);
        assertThat(deserializedEvent, instanceOf(originalEvent.getClass()));
    }

    @Test
    public void testJsonDeserialization_WithGenericEventType() throws IOException {
        EiffelEvent event = new ObjectMapper().readValue(
                getClass().getResourceAsStream("EiffelCompositionDefinedEvent.json"), EiffelEvent.class);
        assertThat(event, instanceOf(GenericEiffelEvent.class));
    }

    @Test(expected = InvalidJsonPayloadException.class)
    public void testJsonDeserialization_WithMissingTypeInfo() throws IOException {
        new ObjectMapper().readValue(
                getClass().getResourceAsStream("EiffelCompositionDefinedEvent_without_meta.json"), EiffelEvent.class);
    }

    @Test
    public void testSourceProvider_WithDirectConstruction() throws IOException {
        EiffelEvent.setSourceProvider(new DummyDomainIdProvider());
        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent("activity name");
        assertThat(event.getMeta().getSource().getDomainId(), is(DummyDomainIdProvider.DOMAIN_ID));
    }

    @Test
    public void testSourceProvider_FromJsonToConcreteClass() throws IOException {
        EiffelEvent.setSourceProvider(new DummyDomainIdProvider());
        EiffelEvent event = new ObjectMapper().readValue(
                getClass().getResourceAsStream("EiffelActivityTriggeredEvent.json"), EiffelEvent.class);
        assertThat(event.getMeta().getSource().getDomainId(), is(DummyDomainIdProvider.DOMAIN_ID));
    }

    @Test
    public void testSourceProvider_FromJsonToGenericClass() throws IOException {
        EiffelEvent.setSourceProvider(new DummyDomainIdProvider());
        EiffelEvent event = new ObjectMapper().readValue(
                getClass().getResourceAsStream("EiffelCompositionDefinedEvent.json"), EiffelEvent.class);
        // First make sure we actually get a GenericEiffelEvent object. If we introduce a concrete
        // class for EiffelCompositionDefinedEvent this testcase must be updated to not follow
        // the same code path as testSourceProvider_FromJsonToConcreteClass.
        assertThat(event, instanceOf(GenericEiffelEvent.class));
        assertThat(event.getMeta().getSource().getDomainId(), is(DummyDomainIdProvider.DOMAIN_ID));
    }

    class DummyDomainIdProvider implements SourceProvider {
        static final String DOMAIN_ID = "dummy";

        @Override
        public void populateSource(EiffelEvent.Meta.Source source) {
            source.setDomainId(DOMAIN_ID);
        }
    }
}
