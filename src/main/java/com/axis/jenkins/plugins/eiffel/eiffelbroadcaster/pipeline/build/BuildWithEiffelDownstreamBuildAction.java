/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

// The following code is copied from https://github.com/jenkinsci/pipeline-build-step-plugin

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline.build;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.InvisibleAction;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks downstream builds triggered by the {@code build} step, as well as the {@link FlowNode#getId} of the step.
 *
 * @see BuildWithEiffelUpstreamCause
 */
public final class BuildWithEiffelDownstreamBuildAction extends InvisibleAction {
    private final List<BuildWithEiffelDownstreamBuild> downstreamBuilds = new ArrayList<>();

    public static @NonNull BuildWithEiffelDownstreamBuild getOrCreate(@NonNull Run<?, ?> run, @NonNull String flowNodeId, @NonNull Item job) {
        BuildWithEiffelDownstreamBuildAction downstreamBuildAction;
        synchronized (BuildWithEiffelDownstreamBuildAction.class) {
            downstreamBuildAction = run.getAction(BuildWithEiffelDownstreamBuildAction.class);
            if (downstreamBuildAction == null) {
                downstreamBuildAction = new BuildWithEiffelDownstreamBuildAction();
                run.addAction(downstreamBuildAction);
            }
        }
        return downstreamBuildAction.getOrAddDownstreamBuild(flowNodeId, job);
    }

    public synchronized @NonNull List<BuildWithEiffelDownstreamBuild> getDownstreamBuilds() {
        return Collections.unmodifiableList(new ArrayList<>(downstreamBuilds));
    }

    private synchronized @NonNull BuildWithEiffelDownstreamBuild getOrAddDownstreamBuild(@NonNull String flowNodeId, @NonNull Item job) {
        for (BuildWithEiffelDownstreamBuild build : downstreamBuilds) {
            if (build.getFlowNodeId().equals(flowNodeId)) {
                return build;
            }
        }
        var build = new BuildWithEiffelDownstreamBuild(flowNodeId, job);
        downstreamBuilds.add(build);
        return build;
    }

    public static final class BuildWithEiffelDownstreamBuild {
        private final String flowNodeId;
        private final String jobFullName;
        private Integer buildNumber;

        BuildWithEiffelDownstreamBuild(String flowNodeId, @NonNull Item job) {
            this.flowNodeId = flowNodeId;
            this.jobFullName = job.getFullName();
        }

        public @NonNull String getFlowNodeId() {
            return flowNodeId;
        }

        public @NonNull String getJobFullName() {
            return jobFullName;
        }

        /**
         * Get the build number of the downstream build, or {@code null} if the downstream build has not yet started or the queue item was cancelled.
         */
        public @CheckForNull Integer getBuildNumber() {
            return buildNumber;
        }

        /**
         * Load the downstream build, if it has started and still exists.
         * <p>Loading builds indiscriminately will affect controller performance, so use this carefully. If you only need
         * to know whether the build started at one point, use {@link #getBuildNumber}.
         * @throws AccessDeniedException as per {@link ItemGroup#getItem}
         */
        public @CheckForNull Run<?, ?> getBuild() throws AccessDeniedException {
            if (buildNumber == null) {
                return null;
            }
            return Run.fromExternalizableId(jobFullName + '#' + buildNumber);
        }

        void setBuild(@NonNull Run<?, ?> run) {
            this.buildNumber = run.getNumber();
        }
    }
}
