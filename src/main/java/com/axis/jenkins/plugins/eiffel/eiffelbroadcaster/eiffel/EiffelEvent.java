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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;
/**
 * EiffelEvent base containing object requiered in all events.
 *
 * Schema for all events can be found in the link below.
 * https://github.com/eiffel-community/eiffel/tree/master/schemas
 *
 * Protocol specification for all events can be found in the link below
 * https://github.com/eiffel-community/eiffel/tree/master/eiffel-vocabulary
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
abstract class EiffelEvent {

    /**
    * EiffelEvent Constants.
    */
    public static final String ACTIVITY_EXECUTION = "ACTIVITY_EXECUTION";

    /**
    * getter for eventMeta.
    * @return event metadata
    */
    public Map<String, Object> getEventMeta() {
        return eventMeta;
    }

    /**
    * getter for eventLinks.
    * @return event metadata
    */
    public List<List> getEventLinks() {
        return eventLinks;
    }
    /**
    * getter for eventLinks.
    * @return event metadata
    */
    public Map<String, Map> getEventData() {
        return eventData;
    }

    private Map<String, Object> eventMeta = new HashMap<String, Object>();
    private List<List> eventLinks = new ArrayList<List>();
    private Map<String, Map> eventData = new HashMap<String, Map>();

    public abstract String getVersion();

    public void setEventLinks(List links) {
        this.eventLinks = links;
    }

    public void setEventData(Map data) {
        this.eventData = data;
    }
    /**
    * Constructor for EiffelEvent.
    */
    public EiffelEvent() {
        eventMeta.put("id", Util.getUUID());
        eventMeta.put("time", Util.getTime());
        eventMeta.put("type", this.getClass().getSimpleName());
        eventMeta.put("version", getVersion());
    }
    /**
    * Get this event as Json.
    * @return eiffel event in json format.
    */
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("meta", eventMeta);
        if (eventData != null) {
            json.put("data", eventData);
        }
        if (eventLinks != null) {
            json.put("links", eventLinks);
        }
        return json;
    }
}

