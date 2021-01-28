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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityStartedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Action;
import hudson.model.Run;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * An {@link Action} for storing the Eiffel activity events ({@link EiffelActivityTriggeredEvent} (ActT),
 * {@link EiffelActivityStartedEvent} (ActS), and {@link EiffelActivityFinishedEvent} (ActF)) that have been
 * emitted for a {@link Run}. This allows other plugins and clients in a CI/CD pipeline to map a Run into
 * the Eiffel domain.
 *
 * This action is instantiated when ActT is sent and always contains that event. At that point the contents
 * of ActS and ActF are unknown so the methods for returning those events return null.
 */
@ExportedBean
public class EiffelActivityAction implements Action {
    private String finishedEventJSON;
    private String startedEventJSON;
    private String triggerEventJSON;

    public EiffelActivityAction(@Nonnull EiffelActivityTriggeredEvent triggerEvent) throws JsonProcessingException {
        this.triggerEventJSON = triggerEvent.toJSON();
    }

    /**
     * Returns the Run's {@link EiffelActivityFinishedEvent}, or null if the Run hasn't completed
     * and no event has been sent.
     */
    @CheckForNull
    public EiffelActivityFinishedEvent getFinishedEvent() throws JsonProcessingException {
        if (finishedEventJSON == null) {
            return null;
        }
        // It might make sense to cache this value for future use. It's a memory/CPU trade-off.
        return new ObjectMapper().readValue(finishedEventJSON, EiffelActivityFinishedEvent.class);
    }

    /**
     * Returns the Run's {@link EiffelActivityFinishedEvent} expressed as a JSON string, or null if the Run hasn't
     * completed and no event has been sent.
     */
    @CheckForNull
    @Exported
    public String getFinishedEventJSON() {
        return finishedEventJSON;
    }

    /** Stores the {@link EiffelActivityFinishedEvent} that was sent when this Run completed. */
    void setFinishedEvent(@Nonnull EiffelActivityFinishedEvent finishedEvent) throws JsonProcessingException {
        finishedEventJSON = finishedEvent.toJSON();
    }

    /**
     * Returns the Run's {@link EiffelActivityStartedEvent}, or null if the Run hasn't completed
     * and no event has been sent.
     */
    @CheckForNull
    public EiffelActivityStartedEvent getStartedEvent() throws JsonProcessingException {
        if (startedEventJSON == null) {
            return null;
        }
        // It might make sense to cache this value for future use. It's a memory/CPU trade-off.
        return new ObjectMapper().readValue(startedEventJSON, EiffelActivityStartedEvent.class);
    }

    /**
     * Returns the Run's {@link EiffelActivityStartedEvent} expressed as a JSON string, or null if the Run hasn't
     * completed and no event has been sent.
     */
    @CheckForNull
    @Exported
    public String getStartedEventJSON() {
        return startedEventJSON;
    }

    /** Stores the {@link EiffelActivityStartedEvent} that was sent when this Run completed. */
    void setStartedEvent(@Nonnull EiffelActivityStartedEvent startedEvent) throws JsonProcessingException {
        startedEventJSON = startedEvent.toJSON();
    }

    /** Returns the Run's EiffelActivityTriggeredEvent. */
    @Nonnull
    public EiffelActivityTriggeredEvent getTriggerEvent() throws JsonProcessingException {
        // It might make sense to cache this value for future use. It's a memory/CPU trade-off.
        return new ObjectMapper().readValue(triggerEventJSON, EiffelActivityTriggeredEvent.class);
    }

    /** Returns the Run's {@link EiffelActivityTriggeredEvent} expressed as a JSON string. */
    @Exported
    public String getTriggerEventJSON() {
        return triggerEventJSON;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return null;
    }
}
