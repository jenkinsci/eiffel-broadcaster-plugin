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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The supported hash algorithms when signing events.
 */
public enum HashAlgorithm {
    SHA_256("SHA-256"),
    SHA_384("SHA-384"),
    SHA_512("SHA-512");

    private final String displayName;

    HashAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    /** Constructs a {@link HashAlgorithm} object by parsing a string. */
    public static HashAlgorithm fromString(final String s) throws IllegalArgumentException {
        for (var value : HashAlgorithm.values()) {
            if (value.getDisplayName().equals(s)) {
                return value;
            }
        }
        var availableValues = Arrays.stream(HashAlgorithm.values())
                .map(HashAlgorithm::getDisplayName)
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException(String.format(
                "The string \"%s\" isn't a supported hash algorithm. Please choose one of the following: %s",
                s, availableValues));
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
