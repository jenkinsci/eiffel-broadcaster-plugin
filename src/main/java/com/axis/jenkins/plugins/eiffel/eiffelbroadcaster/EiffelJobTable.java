/**
 The MIT License

 Copyright 2018-2021 Axis Communications AB.

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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Contains eiffel event map for jobs {@link EiffelJobTable}.
 * This singleton table is used to store information about eiffel events that
 * are linked within the same job.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 * @version 1.0
 * @since 2018-09-24
 */
public final class EiffelJobTable {
    private static EiffelJobTable instance = null;
    private final ConcurrentHashMap<Long, UUID> table;

    /**
     * EiffelJobTable Constructor.
     *
     */
     private EiffelJobTable() {
        this.table = new ConcurrentHashMap<Long, UUID>();
    }

    /**
     * Get singleton instance.
     * @return class instance
     */
    public static synchronized EiffelJobTable getInstance() {
        if (instance == null) {
            instance = new EiffelJobTable();
        }
        return instance;
    }

    /**
     * Get last event on a given jenkinsID.
     * @return eventId
     * @param jenkinsID
     * uniqe id of a jenkins job.
     */
    public UUID getEventTrigger(@Nonnull Long jenkinsID) {
        return table.get(jenkinsID);
    }

    /**
     * Update table with a new event for the given jenkinsId.
     * @param jenkinsID
     * uniqe id of a jenkins job.
     * @param eiffelEventID
     * uniqe id of an eiffel event.
     */
    public void setEventTrigger(@Nonnull Long jenkinsID, @Nonnull UUID eiffelEventID) {
        table.put(jenkinsID, eiffelEventID);
    }

}
