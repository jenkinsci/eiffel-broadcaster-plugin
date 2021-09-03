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
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A collection of Hamcrest {@link Matcher} functions that e.g. inspect the state of Eiffel events.
 */
public class Matchers {
    /**
     * Returns a matcher that checks whether the subject {@link EiffelEvent} contains
     * a link of the specified type to another specified EiffelEvent.
     *
     * @param target the target event that the link is expected to point to
     * @param type the link type
     */
    public static Matcher<EiffelEvent> linksTo(EiffelEvent target, EiffelEvent.Link.Type type) {
        EiffelEvent.Link expectedLink = new EiffelEvent.Link(type, target.getMeta().getId());
        return new TypeSafeMatcher<EiffelEvent>() {
            @Override
            protected boolean matchesSafely(EiffelEvent eiffelEvent) {
                return eiffelEvent.getLinks().contains(expectedLink);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("event containing link: ").appendValue(expectedLink);
            }
        };
    }

    /**
     * Returns a matcher that checks whether the subject {@link EiffelActivityTriggeredEvent}
     * has a trigger with the specified type.
     *
     * @param type the desired type of the trigger
     */
    public static Matcher<EiffelActivityTriggeredEvent> hasTrigger(
            EiffelActivityTriggeredEvent.Data.Trigger.Type type) {
        return hasTrigger(type, "");
    }

    /**
     * Returns a matcher that checks whether the subject {@link Run} has a parameter
     * with the given name and value.
     *
     * @param name the expected name of the parameter
     * @param value the expected value of the parameter
     */
    public static Matcher<Run> hasBuildParameter(String name, Object value) {
        return new TypeSafeMatcher<Run>() {
            @Override
            protected boolean matchesSafely(Run run) {
                ParametersAction paramAction = run.getAction(ParametersAction.class);
                if (paramAction == null) {
                    return false;
                }
                ParameterValue actualParam = paramAction.getParameter(name);
                if (actualParam == null) {
                    return false;
                }
                return value.equals(actualParam.getValue());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("run having a %s parameter with the value '%s'", name, value));
            }
        };
    }

    /**
     * Returns a matcher that checks whether the subject {@link EiffelActivityTriggeredEvent}
     * has a trigger with the specified type.
     *
     * @param type the type that the trigger is expected to have
     * @param descriptionSubstring a string that should appear in the trigger's description
     */
    public static Matcher<EiffelActivityTriggeredEvent> hasTrigger(
            EiffelActivityTriggeredEvent.Data.Trigger.Type type, String descriptionSubstring) {
        return new TypeSafeMatcher<EiffelActivityTriggeredEvent>() {
            @Override
            protected boolean matchesSafely(EiffelActivityTriggeredEvent eiffelEvent) {
                return eiffelEvent.getData().getTriggers().stream()
                        .anyMatch(t -> t.getDescription() != null
                                && t.getDescription().contains(descriptionSubstring)
                                && t.getType() == type);
            }

            @Override
            public void describeTo(Description description) {
                if (descriptionSubstring.isEmpty()) {
                    description.appendText(String.format("event containing a %s trigger", type));
                } else {
                    description.appendText(String.format(
                            "event containing a %s trigger and the substring \"%s\" in the description",
                            type, descriptionSubstring));
                }
            }
        };
    }
}
