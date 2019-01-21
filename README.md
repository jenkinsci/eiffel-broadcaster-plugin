# Eiffel Broadcaster Plugin

This jenkins plugin sends Eiffel events to a Message Queue. For now, only RabbitMQ is supported.
The plugin can be extended with more events and or more data but the required data is there.
In addition to jobs, artifacts are represented in eiffel, these are the artifacts saved in the "Archive the artifacts"
post build action. A detailed list of event representations can be found in the table below.

Read more about the Eiffel protocol on https://github.com/eiffel-community/eiffel

## Jenkins events represented in Eiffel are:
| Jenkins Event               | Eiffel Event                 |
| --------------------------- |------------------------------|
| Job Queued                  | EiffelActivityTriggeredEvent |
| Job Dequeued (canceled)     | EiffelActivityCanceledEvent  |
| Job Starts                  | EiffelActivityStartedEvent   |
| Job Finishes                | EiffelActivityFinishedEvent  |
| Job Successful              | EiffelActivityFinishedEvent  |
| Job unstable                | EiffelActivityFinishedEvent  |
| Job failed                  | EiffelActivityFinishedEvent  |
| Job Aborted                 | EiffelActivityFinishedEvent  |
| job Artifact Saved          | EiffelArtifactCreatedEvent   |

### Notes
- EiffelArtifactPublishedEvent is not sent by this plugin.
- Current versions of each event can be found in the getVersion() function in the [sourcecode.](https://github.com/Isacholm/EiffelBroadcaster/tree/master/src/main/java/com/axis/jenkins/plugins/eiffel/eiffelbroadcaster/eiffel)

## How to build and install this plugin from source
In the EiffelBroadcaster root folder, use maven to compile.
```
$ mvn compile
```
In the EiffelBroadcaster root folder, use maven to build the .hpi file.
```
$ mvn hpi:hpi
```
2. In the jenkins web interface go to: Manage Jenkins -> Plugin Manager -> Advanced
3. At "Upload Plugin", Browse to the .hpi file located in the EiffelBroadcaster/target directory and press "Upload"

The plugin should install without the need to reboot jenkins.


## Read more about the Eiffel events used in this plugin
- [EiffelActivityTriggeredEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityTriggeredEvent.md)
- [EiffelActivityStartedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityStartedEvent.md)
- [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md)
- [EiffelActivityCanceledEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityCanceledEvent.md)
- [EiffelArtifactCreatedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelArtifactCreatedEvent.md)

This plugin is part of the [Eiffel Community](https://github.com/eiffel-community/)

## Maintainers

* Isac Holm
  - isac.holm@axis.com

# License
```
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
```
==============
