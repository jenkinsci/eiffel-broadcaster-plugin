/*
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
package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.EiffelBroadcasterConfig;

def f = namespace("/lib/form")
def l = "/plugin/eiffel-broadcaster/"

f.section(title: "Eiffel Broadcaster Plugin") {

    f.entry(title: "Enable Eiffel Broadcaster plugin", help: l+"help-enable-broadcaster.html") {
        f.checkbox(field: "enableBroadcaster", checked: my.enableBroadcaster)
    }
    f.entry(title: "MQ URI", field: "serverUri", help: l+"help-amqp-uri.html") {
        f.textbox("value":my.serverUri)
    }
    f.entry(title: "User name", field: "userName", help: l+"help-user-name.html") {
        f.textbox("value":my.userName)
    }
    f.entry(title: "Password", field: "userPassword", help: l+"help-user-password.html") {
        f.password("value":my.userPassword)
    }
    descriptor = my.descriptor
    f.validateButton(title: "Test Connection", progress: "Trying to connect...", method: "testConnection",
            with: "serverUri,userName,userPassword")

    f.entry(title: "Exchange Name", field: "exchangeName", help: l+"help-exchange-name.html") {
        f.textbox("value":my.exchangeName)
    }
    f.entry(title: "Virtual host", field: "virtualHost", help: l+"help-virtual-host.html") {
        f.textbox("value":my.virtualHost)
    }
    f.entry(title: "Routing Key", field: "routingKey", help: l+"help-routing-key.html") {
        f.textbox("value":my.routingKey)
    }
    f.entry(title: "Application Id", field: "appId", help: l+"help-application-id.html") {
        f.textbox("value":my.appId)
    }
    f.entry(title: "Persistent Delivery mode", help: l+"help-persistent-delivery.html") {
        f.checkbox(field: "persistentDelivery", checked: my.persistentDelivery)
    }
    f.entry(title: "Activity Categories", field: "activityCategories", help: l+"help-activity-categories.html") {
        f.textarea(value: my.activityCategories)
    }
    f.entry(title: "Hostname source", field: "hostnameSource", help: l+"help-hostname-source.html") {
        f.enum {
            raw(my.description)
        }
    }
}
