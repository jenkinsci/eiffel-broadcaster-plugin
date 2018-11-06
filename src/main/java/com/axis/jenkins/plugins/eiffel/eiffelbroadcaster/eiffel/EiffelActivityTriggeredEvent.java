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
 * EiffelEvent EiffelActivityTriggeredEvent representation.
 *
 * Schema for this event can be found in the link below.
 * https://github.com/eiffel-community/eiffel/tree/master/schemas/EiffelActivityFinished
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
public class EiffelActivityTriggeredEvent extends EiffelEvent {
    /**
     * Event Version implementation.
     * @return Event Version
     */
    public String getVersion() {
        return "1.1.0";
    }
    /**
     * Constructor for EiffelActivityTriggeredEvent.
     * @param eventName full name of the triggered job.
     */
    public EiffelActivityTriggeredEvent(String eventName) {
        super();
        super.setEventData(initEventData(eventName));
        super.setEventLinks(initEventLinks());
    }

    /**
     * Initialize the eventData.
     * @param eventName full name of the triggered job.
     * @return eventData object
     */
    private Map initEventData(String eventName) {
        Map<String, String> eventData = new HashMap<String, String>();
        eventData.put("name", eventName);

        return eventData;
    }

    /**
     * initialize the eventLinks.
     * @return eventLinks, currently empty.
     */
    private List initEventLinks() {
        List<Map> eventLinks = new ArrayList<Map>();

        return eventLinks;
    }
}
