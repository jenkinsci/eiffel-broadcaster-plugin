# Eiffel Broadcaster Plugin

This Jenkins plugin sends Eiffel events to a Message Queue. For now, only RabbitMQ is supported.
The plugin can be extended with more events and or more data but the required data is there.
A detailed list of event representations can be found in the table below.

Read more about the Eiffel protocol on https://github.com/eiffel-community/eiffel

## Jenkins events represented in Eiffel are:
| Jenkins Event               | Eiffel Event                 |
| --------------------------- |------------------------------|
| Job Queued                  | EiffelActivityTriggeredEvent |
| Job Dequeued (canceled)     | EiffelActivityCanceledEvent  |
| Job Starts                  | EiffelActivityStartedEvent   |
| Job Finishes                | EiffelActivityFinishedEvent  |
| Job Successful              | EiffelActivityFinishedEvent  |
| Job Unstable                | EiffelActivityFinishedEvent  |
| Job Failed                  | EiffelActivityFinishedEvent  |
| Job Aborted                 | EiffelActivityFinishedEvent  |

### Notes
- Current versions of each event can be found in the getVersion() function in the [sourcecode.](https://github.com/Isacholm/EiffelBroadcaster/tree/master/src/main/java/com/axis/jenkins/plugins/eiffel/eiffelbroadcaster/eiffel)

## Accessing emitted Eiffel events in builds
If a build needs to emit Eiffel events of its own they should probably have
a CONTEXT or CAUSE link to the build's EiffelActivityTriggeredEvent. The
plugins injects the following environment variables with the contents of
the previously sent events:
* `EIFFEL_ACTIVITY_TRIGGERED`: The build's EiffelActivityTriggeredEvent event.
* `EIFFEL_ACTIVITY_STARTED`: The build's EiffelActivityStartedEvent event.

## API
The plugin will do its best to populate the emitted
EiffelActivityTriggeredEvent with information taken from the causes of
the build, but in many cases you'll want to inject additional Eiffel links
that the plugin can't figure out on its own. This can be done with the
$JOB_URL/eiffel/build API endpoint which works nearly identially to the
standard $JOB_URL/build endpoint except that it requires an additional
`eiffellinks` form parameter that contains the Eiffel links to include in
the EiffelActivityTriggeredEvent. `eiffellinks` follows the same schema
as the `links` value in Eiffel events. Example (payload lacking URL
encoding to improve readability):
```
POST $JOB_URL/eiffel/build
Content-Type: application/x-www-form-urlencoded

json={"eiffellinks": [{"target": "662b3813-bef4-4588-bf75-ffaead24a6d5", "type": "CAUSE"}], "parameter": [{"name": "PARAM_NAME", "value": "param value"}]}
```
The `eiffellinks` key is mandatory for this endpoint but `parameter` is
optional. If the latter is omitted and the job is parameterized the default
values for all parameters will be used.

If at least one CAUSE link is included in `eiffellinks`, an `EIFFEL_EVENT`
trigger will be included in the EiffelActivityTriggeredEvent.

## How to build and install this plugin from source
In the EiffelBroadcaster root folder, use maven to compile.
```
$ mvn compile
```
In the EiffelBroadcaster root folder, use maven to build the .hpi file.
```
$ mvn hpi:hpi
```
2. In the Jenkins web interface go to: Manage Jenkins -> Plugin Manager -> Advanced
3. At "Upload Plugin", Browse to the .hpi file located in the EiffelBroadcaster/target directory and press "Upload"

The plugin should install without the need to reboot Jenkins.


## Read more about the Eiffel events used in this plugin
- [EiffelActivityTriggeredEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityTriggeredEvent.md)
- [EiffelActivityStartedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityStartedEvent.md)
- [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md)
- [EiffelActivityCanceledEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityCanceledEvent.md)

This plugin is part of the [Eiffel Community](https://github.com/eiffel-community/)

## Maintainers

* Isac Holm \<isac.holm@axis.com\>
* Magnus Bäck \<magnus.back@axis.com\>

# License
```
The MIT License

Copyright 2018-2021 Axis Communications AB.

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
