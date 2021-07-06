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

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import java.util.Set;

/** Thrown when an Eiffel event has failed a validation against a schema. */
public class EventValidationFailedException extends Exception {
    private final Set<ValidationMessage> validationResult;
    private final JsonNode inputDocument;

    public EventValidationFailedException(final Set<ValidationMessage> validationResult,
                                          final JsonNode inputDocument) {
        super(String.format("Schema validation failed (%s) for event: %s", validationResult, inputDocument));
        this.validationResult = validationResult;
        this.inputDocument = inputDocument;
    }

    /** Returns the set of {@link ValidationMessage} instances that describe the problems reported by the validator. */
    public Set<ValidationMessage> getValidationResult() {
        return validationResult;
    }

    /** Returns the JSON document that failed the validation. */
    public JsonNode getInputDocument() {
        return inputDocument;
    }
}
