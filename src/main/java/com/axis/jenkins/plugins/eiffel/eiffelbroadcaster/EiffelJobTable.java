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
 * Maintains a table that maps a Jenkins queue ids to the id of the
 * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}
 * that was sent when the build was enqueued. This is needed to link the resulting
 * {@link hudson.model.Run} to the right trigger event.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 * @version 1.0
 * @since 2018-09-24
 */
public final class EiffelJobTable {
    private static EiffelJobTable instance = null;
    private final ConcurrentHashMap<Long, UUID> table;

    /** Private constructor. Use {@link #getInstance()} to obtain an object instance of this class. */
    private EiffelJobTable() {
        this.table = new ConcurrentHashMap<Long, UUID>();
    }

    /** Gets the singleton instance. */
    public static synchronized EiffelJobTable getInstance() {
        if (instance == null) {
            instance = new EiffelJobTable();
        }
        return instance;
    }

    /**
     * Gets the id of the {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}
     * for a given queue id, or null of no such mapping is known.
     */
    public UUID getEventTrigger(@Nonnull Long queueId) {
        return table.get(queueId);
    }

    /**
     * Gets the id of the {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}
     * for a given queue id, or null if no such mapping is known, and immediately clears that entry from the table.
     */
     public UUID getAndClearEventTrigger(@Nonnull Long queueId) {
         return table.remove(queueId);
     }

    /**
     * Update the table with a new mapping from a queue id to the id of a
     * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}.
     * */
    public void setEventTrigger(@Nonnull Long queueId, @Nonnull UUID eiffelEventId) {
        table.put(queueId, eiffelEventId);
    }

}
