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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Action;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * An {@link Action} for storing the
 * <a href="https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityTriggeredEvent.md">
 * EiffelActivityTriggeredEvent</a> that was emitted when a build was enqueued.
 */
@ExportedBean
public class EiffelActivityTriggerAction implements Action {
    private String eventJSON;

    public EiffelActivityTriggerAction(EiffelActivityTriggeredEvent event) throws JsonProcessingException {
        this.eventJSON = event.toJSON();
    }

    /** Returns the build's EiffelActivityTriggeredEvent. */
    public EiffelActivityTriggeredEvent getEvent() throws JsonProcessingException {
        // It might make sense to cache this value for future use. It's a memory/CPU trade-off.
        return new ObjectMapper().readValue(eventJSON, EiffelActivityTriggeredEvent.class);
    }

    /** Returns the build's EiffelActivityTriggeredEvent expressed as a JSON string. */
    @Exported
    public String getEventJSON() {
        return eventJSON;
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
