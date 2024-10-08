/**
 The MIT License

 Copyright 2023-2024 Axis Communications AB.

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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.routingkeys;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEventFactory;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SepiaRoutingKeyProviderTest {
    @Test
    public void testGetRoutingKey_UsesUnderscoreInsteadOfNull() {
        var rp = new SepiaRoutingKeyProvider();
        var event = EiffelEventFactory.getInstance().create(EiffelArtifactCreatedEvent.class);
        assertThat(rp.getRoutingKey(event), is("eiffel._.EiffelArtifactCreatedEvent._._"));
    }

    @Test
    public void testGetRoutingKey_UsesDomainIdIfSet() {
        var rp = new SepiaRoutingKeyProvider();
        var event = EiffelEventFactory.getInstance().create(EiffelArtifactCreatedEvent.class);
        event.getMeta().getSource().setDomainId("some-domainid");
        assertThat(rp.getRoutingKey(event), is("eiffel._.EiffelArtifactCreatedEvent._.some-domainid"));
    }

    @Test
    public void testGetRoutingKey_UsesTagIfSet() {
        var rp = new SepiaRoutingKeyProvider();
        rp.setTag("some-tag");
        var event = EiffelEventFactory.getInstance().create(EiffelArtifactCreatedEvent.class);
        assertThat(rp.getRoutingKey(event), is("eiffel._.EiffelArtifactCreatedEvent.some-tag._"));
    }
}
