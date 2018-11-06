/**
 The MIT License

 Copyright 2018 Axis Communications AB.

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EiffelEvent EiffelArtifactCreatedEvent representation.
 *
 * Schema for this event can be found in the link below.
 * https://github.com/eiffel-community/eiffel/tree/master/schemas/EiffelArtifactCreated
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
public class EiffelArtifactCreatedEvent extends EiffelEvent {
    /**
     * Event Version implementation.
     * @return Event Version
     */
    public String getVersion() {
        return "2.0.0";
    }
    /**
     * Constructor for EiffelArtifactCreatedEvent.
     * @param artifactIdentity purl specified artifact identity.
     */
    public EiffelArtifactCreatedEvent(String artifactIdentity) {
        super();
        super.setEventData(initEventData(artifactIdentity));
        super.setEventLinks(initEventLinks());

    }
    /**
     * Initialize the eventData with the attifact identity.
     * @param artifactIdentity purl specified artifact identity.
     * @return eventData object.
     */
    private Map initEventData(String artifactIdentity) {
        Map<String, Object> eventData = new HashMap<>();

        eventData.put("identity", artifactIdentity);

        return eventData;
    }
    /**
     * Set the event link list containing link type(s) and event target(s).
     * @return event links list for the event, currently empty.
     */
    private List initEventLinks() {
        List<Map> eventLinks = new ArrayList<>();

        return eventLinks;
    }

}
