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

import hudson.model.Cause;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import java.util.Objects;

/**
 * Points back to the triggering {@link FlowNode}.
 *
 * @see BuildWithEiffelDownstreamBuildAction
 */
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
