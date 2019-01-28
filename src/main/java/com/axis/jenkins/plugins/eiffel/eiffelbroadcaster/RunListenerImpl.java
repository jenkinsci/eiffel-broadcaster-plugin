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

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityStartedEvent;
import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelArtifactCreatedEvent;
import com.rabbitmq.client.AMQP;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityFinishedEvent;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import net.sf.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Receives notifications about builds and publish messages on configured MQ server.
 *
 * @author Isac Holm &lt;isac.holm@axis.com&gt;
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {

    private static EiffelBroadcasterConfig config;

    /**
     * Constructor for RunListenerImpl.
     */
    public RunListenerImpl() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        JSONObject json;
        EiffelActivityStartedEvent event;
        String targetEvent = EiffelJobTable.getInstance().getEventTrigger(r.getQueueId());
        event = new EiffelActivityStartedEvent(targetEvent);
        json = event.getJson();
        publish(json);
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        Result res = r.getResult();
        List<JSONObject> allEvents = new ArrayList<JSONObject>();
        String status = "INCONCLUSIVE";
        if (res != null) {
            status = Util.translateStatus(res.toString());
        }

        String targetEvent = EiffelJobTable.getInstance().getEventTrigger(r.getQueueId());
        EiffelActivityFinishedEvent actFinEvent = new EiffelActivityFinishedEvent(status, targetEvent);
        JSONObject actFinJson = actFinEvent.getJson();

        allEvents.add(actFinJson);

        if (status != "INCONCLUSIVE") {
            if (res.isBetterOrEqualTo(Result.UNSTABLE)) {
                List <Run.Artifact> artifacts = r.getArtifacts();
                for (Run.Artifact artifact : artifacts) {
                    String identifier;
                    identifier = Util.getArtifactIdentity(artifact, r.getUrl(), r.getNumber());
                    EiffelArtifactCreatedEvent artCreatedEvent = new EiffelArtifactCreatedEvent(identifier);
                    allEvents.add(artCreatedEvent.getJson());
                }
            }
        }

        for(JSONObject event:allEvents) {
            publish(event);
        }
    }

    /**
     * Publish json message on configured MQ server.
     *
     * @param json the message in json format
     */
    private void publish(JSONObject json) {
        if (config == null) {
            config = EiffelBroadcasterConfig.getInstance();
        }
        if (config != null && config.isBroadcasterEnabled()) {
            AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
            int dm = 1;
            if (config.getPersistentDelivery()) {
                dm = 2;
            }
            bob.appId(config.getAppId());
            bob.deliveryMode(dm);
            bob.contentType(Util.CONTENT_TYPE);
            bob.timestamp(Calendar.getInstance().getTime());
            MQConnection.getInstance().addMessageToQueue(config.getExchangeName(), config.getRoutingKey(),
                    bob.build(), json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
