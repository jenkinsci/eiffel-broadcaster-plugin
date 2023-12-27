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

import java.util.Arrays;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class UtilTest {
    @Test
    public void testLinesToCollection_DuplicateLinesKept() {
        assertThat(Util.getLinesInString("foo\nfoo"), is(Arrays.asList("foo", "foo")));
    }

    @Test
    public void testLinesToCollection_EmptyString() {
        assertThat(Util.getLinesInString(""), is(empty()));
    }

    @Test
    public void testLinesToCollection_EmptyLinesIgnored() {
        assertThat(Util.getLinesInString("foo\n\nbar\n\n"), is(Arrays.asList("foo", "bar")));
    }

    @Test
    public void testLinesToCollection_MultipleLinesKept() {
        assertThat(Util.getLinesInString("foo\nbar"), is(Arrays.asList("foo", "bar")));
    }

    @Test
    public void testLinesToCollection_PrefixPostfixWhitespaceTrimmed() {
        assertThat(Util.getLinesInString("  foo  "), is(Arrays.asList("foo")));
    }
}
