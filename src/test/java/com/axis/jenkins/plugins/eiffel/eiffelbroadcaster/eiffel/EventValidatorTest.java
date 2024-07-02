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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class EventValidatorTest {
    @Test
    public void testValidateAcceptsValidEvent() throws Exception {
        var validator = new EventValidator();
        var event = EiffelEventFactory.getInstance().create(EiffelActivityTriggeredEvent.class);
        event.getData().setName("activity name");
        validator.validate(event.getMeta().getType(), event.getMeta().getVersion(),
                new ObjectMapper().valueToTree(event));
    }

    @Test(expected = SchemaUnavailableException.class)
    public void testValidateRejectsUnknownEvent() throws Exception {
        var validator = new EventValidator();
        var event = EiffelEventFactory.getInstance().create(EiffelActivityTriggeredEvent.class);
        event.getData().setName("activity name");
        validator.validate("EiffelBogusEvent", "invalid.version",
                new ObjectMapper().valueToTree(event));
    }

    @Test(expected = EventValidationFailedException.class)
    public void testValidateRejectsInvalidEvent() throws Exception {
        var validator = new EventValidator();
        validator.validate("EiffelActivityTriggeredEvent", "4.0.0",
                new ObjectMapper().readTree("{\"message\": \"this is an invalid event\"}"));
    }
}
