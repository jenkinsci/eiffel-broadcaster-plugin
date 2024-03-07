// The following code is copied from https://github.com/jenkinsci/pipeline-build-step-plugin

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline.build;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;

/**
 * @author Vivek Pandey
 */
@Extension
public class BuildWithEiffelQueueListener extends QueueListener {
    @Override
    public void onLeft(Queue.LeftItem li) {
        if(li.isCancelled()){
            for (BuildWithEiffelAction.Trigger trigger : BuildWithEiffelAction.triggersFor(li)) {
                trigger.context.onFailure(new AbortException("Build of " + li.task.getFullDisplayName() + " was cancelled"));
            }
        }
    }


}
