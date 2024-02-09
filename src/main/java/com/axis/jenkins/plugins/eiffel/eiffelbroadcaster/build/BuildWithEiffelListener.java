// The following code is copied from https://github.com/jenkinsci/pipeline-build-step-plugin

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.build;

import hudson.AbortException;
import hudson.Extension;
import hudson.console.ModelHyperlinkNote;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.util.Timer;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class BuildWithEiffelListener extends RunListener<Run<?,?>>{

    private static final Logger LOGGER = Logger.getLogger(BuildWithEiffelListener.class.getName());

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        for (BuildWithEiffelAction.Trigger trigger : BuildWithEiffelAction.triggersFor(run)) {
            StepContext stepContext = trigger.context;
            if (stepContext != null && stepContext.isReady()) {
                LOGGER.log(Level.FINE, "started building {0} from #{1} in {2}", new Object[] {run, run.getQueueId(), stepContext});
                try {
                    TaskListener taskListener = stepContext.get(TaskListener.class);
                    // encodeTo(Run) calls getDisplayName, which does not include the project name.
                    taskListener.getLogger().println("Starting building: " + ModelHyperlinkNote.encodeTo("/" + run.getUrl(), run.getFullDisplayName()));
                    if (trigger.waitForStart) {
                        stepContext.onSuccess(new RunWrapper(run, false));
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, null, e);
                }
            } else {
                LOGGER.log(Level.FINE, "{0} unavailable in {1}", new Object[] {stepContext, run});
            }
        }
        Timer.get().submit(() -> updateDownstreamBuildAction(run));
    }

    @Override
    public void onFinalized(Run<?,?> run) {
        for (BuildWithEiffelAction.Trigger trigger : BuildWithEiffelAction.triggersFor(run)) {
            if (!trigger.waitForStart) {
                StepContext stepContext = trigger.context;
                LOGGER.log(Level.FINE, "completing {0} for {1}", new Object[] {run, stepContext});
                Result result = run.getResult();
                if (result == null) { /* probably impossible */
                    result = Result.FAILURE;
                }

                try {
                    stepContext.get(TaskListener.class).getLogger().println("Build " + ModelHyperlinkNote.encodeTo("/" + run.getUrl(), run.getFullDisplayName()) + " completed: " + result.toString());
                    if (trigger.propagate && result != Result.SUCCESS) {
                        stepContext.get(FlowNode.class).addOrReplaceAction(new WarningAction(result));
                    }
                }  catch (Exception e) {
                    LOGGER.log(Level.WARNING, null, e);
                }

                if (!trigger.propagate || result == Result.SUCCESS) {
                    if (trigger.interruption == null) {
                        stepContext.onSuccess(new RunWrapper(run, false));
                    } else {
                        stepContext.onFailure(trigger.interruption);
                    }
                } else {
                    stepContext.onFailure(new FlowInterruptedException(result, false, new BuildWithEiffelDownstreamFailureCause(run)));
                }
            }
        }
        run.removeActions(BuildWithEiffelAction.class);
    }

    @Override
    public void onDeleted(final Run<?,?> run) {
        for (final BuildWithEiffelAction.Trigger trigger : BuildWithEiffelAction.triggersFor(run)) {
            Timer.get().submit(() -> trigger.context.onFailure(new AbortException(run.getFullDisplayName() + " was deleted")));
        }
    }

    private void updateDownstreamBuildAction(Run<?, ?> downstream) {
        for (Cause cause : downstream.getCauses()) {
            if (cause instanceof BuildWithEiffelUpstreamCause) {
                BuildWithEiffelUpstreamCause buildUpstreamCause = (BuildWithEiffelUpstreamCause) cause;
                Run<?, ?> upstream = buildUpstreamCause.getUpstreamRun();
                if (upstream instanceof FlowExecutionOwner.Executable) {
                    String flowNodeId = buildUpstreamCause.getNodeId();
                    BuildWithEiffelDownstreamBuildAction.getOrCreate(upstream, flowNodeId, downstream.getParent()).setBuild(downstream);
                    try {
                        upstream.save();
                    } catch (IOException e) {
                        LOGGER.log(Level.FINE, e, () -> "Unable to update BuildWithEiffelDownstreamBuildAction for " + upstream + " node " + buildUpstreamCause.getNodeId());
                    }
                }
            }
        }
    }
}