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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.packageurl.PackageURL;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JenkinsSourceProviderTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new Mocks.RabbitMQConnectionMock();
    }

    @Test
    public void testEventHasCorrectSource() throws Exception {
        EiffelActivityTriggeredEvent event = new EiffelActivityTriggeredEvent("activity name");
        assertThat(event.getMeta().getSource(), is(notNullValue()));
        assertThat(event.getMeta().getSource().getName(), is("Eiffel Broadcaster Plugin"));
        assertThat(event.getMeta().getSource().getSerializer(), is(notNullValue()));
        PackageURL purl = new PackageURL(event.getMeta().getSource().getSerializer());
        assertThat(purl.getType(), is("maven"));
        assertThat(purl.getNamespace(), is("com.axis.jenkins.plugins.eiffel"));
        assertThat(purl.getName(), is("eiffel-broadcaster"));
        // Not testing meta.source.uri since it isn't available during tests.
        // Not testing meta.source.host to avoid test flakiness.
    }

    @Test
    public void testSourceNameIsNotOverwritten() throws Exception {
        EiffelEvent event = new ObjectMapper().readValue(
                getClass().getResourceAsStream("EiffelActivityTriggeredEvent.json"), EiffelEvent.class);
        // We get the meta.source.name value from the JSON file
        assertThat(event.getMeta().getSource().getName(), is("Custom meta.source.name"));
        // We get the meta.source.serializer value from JenkinsSourceProvider
        assertThat(event.getMeta().getSource().getSerializer(), is(notNullValue()));
    }
}
