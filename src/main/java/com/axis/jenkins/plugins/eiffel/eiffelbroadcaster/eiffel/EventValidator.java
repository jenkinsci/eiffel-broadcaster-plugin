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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;

/** Validates an Eiffel event against the available schemas. */
public class EventValidator {
    /** A cache of already loaded {@link JsonSchema} instances to allow reuse. */
    private ConcurrentMap<EventVersionKey, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    private ObjectMapper jsonMapper = new ObjectMapper();
    private JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    private SchemaProvider schemaProvider = new BundledSchemaProvider();

    public EventValidator() { }

    /**
     * Validates an Eiffel event and raises am exception if unsuccessful.
     * Will use {@link BundledSchemaProvider} to locate a schema that fits the event.
     *
     * @param eventName the name of the payload's event type
     * @param eventVersion the version of the event type
     * @param eventJson the event payload
     * @throws EventValidationFailedException if the event fails validation
     * @throws SchemaUnavailableException if a schema can't be located
     */
    public void validate(String eventName, String eventVersion, @Nonnull final JsonNode eventJson)
            throws EventValidationFailedException, SchemaUnavailableException {
        EventVersionKey key = new EventVersionKey(eventName, eventVersion);
        JsonSchema schema = schemaCache.get(key);
        if (schema == null) {
            try (InputStream schemaStream = schemaProvider.getSchema(eventName, eventVersion)) {
                if (schemaStream == null) {
                    throw new SchemaUnavailableException(
                            String.format("Unable to locate a schema for %s@%s", eventName, eventVersion));
                }
                schema = schemaFactory.getSchema(jsonMapper.readTree(schemaStream));
            } catch (IOException e) {
                throw new SchemaUnavailableException(
                        String.format("Error reading schema for %s@%s", eventName, eventVersion), e);
            }
            schemaCache.put(key, schema);
        }
        Set<ValidationMessage> result = schema.validate(eventJson);
        if (!result.isEmpty()) {
            throw new EventValidationFailedException(result, eventJson);
        }
    }
}
