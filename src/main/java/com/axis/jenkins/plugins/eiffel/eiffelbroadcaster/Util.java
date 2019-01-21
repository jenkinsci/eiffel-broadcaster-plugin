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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster;

import hudson.model.AbstractItem;
import hudson.model.Queue;
import hudson.model.Run;

import java.util.HashMap;
import java.util.UUID;
import java.time.Instant;
import java.net.InetAddress;


/**
 * Constants and helper functions.
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
public final class Util {
    /** Translate jenkins exit status to eiffel status */
    private static final HashMap<String, String> STATUS_TRANSLATION = new HashMap<>();
    static {
        STATUS_TRANSLATION.put("SUCCESS", "SUCCESSFUL");
        STATUS_TRANSLATION.put("UNSTABLE", "UNSUCCESSFUL");
        STATUS_TRANSLATION.put("FAILURE", "FAILED");
        STATUS_TRANSLATION.put("ABORTED", "ABORTED");
        STATUS_TRANSLATION.put("x", "TIMED_OUT");
        STATUS_TRANSLATION.put("INCONCLUSIVE", "INCONCLUSIVE");
    }

    /**Content Type. */
    public static final String CONTENT_TYPE = "application/json";

    /**
     * Utility classes should not have a public or default constructor.
     */
    private Util() {
    }

    /**
     * Fetches the full name task name if available.
     *
     * @param t
     * Queue.task
     * @return full name if available, else the short name
     */
    public static String getFullName(Queue.Task t) {
        if (t instanceof AbstractItem) {
            return ((AbstractItem)t).getFullName();
        } else {
            return t.getName();
        }
    }
    /**
     * Generate a UIID.
     *
     * @return UUID as a string.
     */
    public static String getUUID() {
        UUID uuid = java.util.UUID.randomUUID();
        return uuid.toString();
    }

    /**
     * Get identity of an Artifact.
     * @param artifact artifact.
     * @param buildUrl Url to the build.
     * @param buildNumber jenkins run build number.
     * @return purl formated identity as a string.
     */
    public static String getArtifactIdentity(Run.Artifact artifact, String buildUrl, Integer buildNumber) {
        String identity;
        identity = "pkg:";
        identity += buildUrl;
        identity += resolveArtifactPath(artifact.toString());
        identity += "@";
        identity += String.valueOf(buildNumber);

        return identity;
    }

    /**
     * Resolve a path so that purl identity is correct.
     * Because the path is relative to the workspace root, artifacts that are archived directly from the root
     * gets a path of "/<artifact>" but if the artifacts are in a directory, the artifact gets a path of
     * "archive/<artifact>", this means we need to remove the initial "/" if there is one.
     * @param path Artifact path from Run.Artifact
     * @return resolevedPath
     */
    private static String resolveArtifactPath(String path) {
        String resolvedPath;
        if (path.charAt(1) == '/'){
            resolvedPath = path.substring(1);
        }
        else {
            resolvedPath = path;
        }

        return resolvedPath;
    }

    /**
     * Fetches time as Epoch in miliseconds.
     *
     * @return Epoch time in milliseconds
     */
    public static long getTime() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Translate Jenkins job status into corresponding eiffel status.
     *
     * @param status
     * jenkins job status to translate.
     * @return translated status.
     */
    public static String translateStatus(String status) {
        String statusTranslated;
        if (STATUS_TRANSLATION.get(status) == null) {
            statusTranslated = STATUS_TRANSLATION.get("inconclusive");
        } else {
            statusTranslated = STATUS_TRANSLATION.get(status);
        }
        return statusTranslated;
    }
}
