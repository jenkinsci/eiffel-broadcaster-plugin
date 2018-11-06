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
 * EiffelEvent EiffelActivityFinishedEvent representation.
 *
 * Schema for this event can be found in the link below.
 * https://github.com/eiffel-community/eiffel/tree/master/schemas/EiffelActivityFinished
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
public class EiffelActivityFinishedEvent extends EiffelEvent {
    /**
     * Event Version implementation.
     * @return Event Version
     */
    public String getVersion() {
        return "1.1.0";
    }

    /**
     * constructor for EiffelActivityFinishedEvent.
     * @param status status of jenkins-job translated into eiffel.
     * @param activityExecutionId event id of the link target event.
     */
    public EiffelActivityFinishedEvent(String status, String activityExecutionId) {
        super();
        super.setEventData(initEventData(status));
        super.setEventLinks(initEventLinks(activityExecutionId));

    }
    /**
     * Initialize the eventData with job conclusion.
     * @param status status of jenkins-job translated into eiffel.
     * @return eventData data object for the event
     */
    private Map initEventData(String status) {
        Map<String, Object> eventData = new HashMap<String, Object>();
        Map<String, Object> outcome = new HashMap<String, Object>();

        outcome.put("conclusion", status);
        eventData.put("outcome", outcome);
        return eventData;
    }
    /**
     * Set the event link list containing link type(s) and event target(s).
     * @param activityExecutionId event id of the event to link to.
     * @return event link list for the event
     */
    private List initEventLinks(String activityExecutionId) {
        List<Map> eventLinks = new ArrayList<Map>();

        Map<String, String> eventLink = new HashMap<String, String>();
        eventLink.put("type", super.ACTIVITY_EXECUTION);
        eventLink.put("target", activityExecutionId);

        eventLinks.add(eventLink);

        return eventLinks;
    }

}
