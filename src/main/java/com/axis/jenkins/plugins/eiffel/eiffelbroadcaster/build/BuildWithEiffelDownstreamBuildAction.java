// The following code is copied from https://github.com/jenkinsci/pipeline-build-step-plugin

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.build;

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
 * Tracks downstream builds triggered by the {@code BuildWithEiffel} step, as well as the {@link FlowNode#getId} of the step.
 *
 * @see BuildWithEiffelUpstreamCause
 */
public final class BuildWithEiffelDownstreamBuildAction extends InvisibleAction {
    private final List<DownstreamBuild> downstreamBuilds = new ArrayList<>();

    public static @NonNull DownstreamBuild getOrCreate(@NonNull Run<?, ?> run, @NonNull String flowNodeId, @NonNull Item job) {
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

    public synchronized @NonNull List<DownstreamBuild> getDownstreamBuilds() {
        return Collections.unmodifiableList(new ArrayList<>(downstreamBuilds));
    }

    private synchronized @NonNull DownstreamBuild getOrAddDownstreamBuild(@NonNull String flowNodeId, @NonNull Item job) {
        for (DownstreamBuild build : downstreamBuilds) {
            if (build.getFlowNodeId().equals(flowNodeId)) {
                return build;
            }
        }
        var build = new DownstreamBuild(flowNodeId, job);
        downstreamBuilds.add(build);
        return build;
    }

    public static final class DownstreamBuild {
        private final String flowNodeId;
        private final String jobFullName;
        private Integer buildNumber;

        DownstreamBuild(String flowNodeId, @NonNull Item job) {
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