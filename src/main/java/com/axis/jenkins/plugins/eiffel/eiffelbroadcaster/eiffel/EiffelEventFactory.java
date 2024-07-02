/**
 The MIT License

 Copyright 2024 Axis Communications AB.

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
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Factory for all event classes that descend from {@link EiffelEvent}.
 * Makes sure event instances are properly initialized, including
 * the event version.
 */
public class EiffelEventFactory {
    /**
     * A mapping from Eiffel event types to the version that should be used when creating
     * an event of that type. These versions correspond to the Paris edition of the protocol.
     * This hardcoded mapping will eventually be replaced by a dynamic mapping that reads
     * <a href="https://github.com/eiffel-community/eiffel/blob/master/event_manifest.yml">
     *     event_manifest.yml
     * </a> from the protocol repository.
     */
    private static final Map<Class, String> EVENT_TYPE_VERSIONS = Map.of(
            EiffelActivityCanceledEvent.class, "3.0.0",
            EiffelActivityFinishedEvent.class, "3.0.0",
            EiffelActivityStartedEvent.class, "4.0.0",
            EiffelActivityTriggeredEvent.class, "4.0.0",
            EiffelArtifactCreatedEvent.class, "3.0.0",
            EiffelArtifactPublishedEvent.class, "3.1.0"
    );

    private SourceProvider sourceProvider;

    /**
     * Creates an instance of the given class and initializes its standard fields
     * (e.g. meta.id, meta.source, and meta.type). The caller is responsible for
     * populating any remaining mandatory fields required by the schema.
     *
     * @param clazz the event class to instantiate
     * @return an object of the given type
     */
    @NonNull
    public <T extends EiffelEvent> T create(final Class<T> clazz) {
        try {
            var event = clazz.getConstructor(String.class).newInstance(EVENT_TYPE_VERSIONS.get(clazz));
            populateSource(event);
            return event;
        } catch (IllegalAccessException | InstantiationException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Event factory unexpectedly failed (this is a bug)", e);
        }
    }

    /** Populates meta.source for the given event. */
    public void populateSource(@NonNull final EiffelEvent event) {
        if (sourceProvider != null) {
            sourceProvider.populateSource(event.getMeta().getSource());
        }
    }

    /**
     * Provide a {@link SourceProvider} instance that will be requested to provide a {@link EiffelEvent.Meta.Source}
     * object for each event created after that point.
     */
    public void setSourceProvider(final SourceProvider provider) {
        sourceProvider = provider;
    }

    /** Returns the singleton object of this class. */
    public static EiffelEventFactory getInstance() {
        return EiffelEventFactory.LazyEiffelEventFactorySingleton.INSTANCE;
    }

    private static class LazyEiffelEventFactorySingleton {
        private static final EiffelEventFactory INSTANCE = new EiffelEventFactory();
    }
}
