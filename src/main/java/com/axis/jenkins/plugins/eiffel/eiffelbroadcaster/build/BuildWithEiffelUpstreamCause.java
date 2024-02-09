// The following code is copied from https://github.com/jenkinsci/pipeline-build-step-plugin

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.build;

import hudson.model.Cause;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.util.Objects;

public class BuildWithEiffelUpstreamCause extends Cause.UpstreamCause {
    private final String nodeId;

    public BuildWithEiffelUpstreamCause(FlowNode node, Run<?, ?> invokingRun) {
        super(invokingRun);
        this.nodeId = node.getId();
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BuildWithEiffelUpstreamCause that = (BuildWithEiffelUpstreamCause) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeId);
    }
}