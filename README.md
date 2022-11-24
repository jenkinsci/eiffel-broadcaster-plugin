# Eiffel Broadcaster Plugin

This Jenkins plugin sends Eiffel events to a Message Queue. For now, only RabbitMQ is supported.
The plugin can be extended with more events and or more data but the required data is there.
A detailed list of event representations can be found in the table below.

Read more about the Eiffel protocol at https://eiffel-community.github.io/.
The plugin and its features were presented at the 2021 Eiffel Summit;
the [presentation slides](https://github.com/eiffel-community/community/blob/master/presentations/eiffel_summit_2021.1/The%20eiffel-broadcaster%20Jenkins%20plugin.pdf)
and a [video](https://youtu.be/fRCq9VosRJA) are available.

## Jenkins events represented in Eiffel

A number of Jenkins events automatically result in Eiffel events.
Additionally, builds have various options of emitting events of their own.

| Jenkins Event           | Eiffel Event                 |
| ------------------------|------------------------------|
| Job Queued              | [EiffelActivityTriggeredEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityTriggeredEvent.md) |
| Job Dequeued (canceled) | [EiffelActivityCanceledEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityCanceledEvent.md) |
| Job Starts              | [EiffelActivityStartedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityStartedEvent.md) |
| Job Finishes            | [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md) |
| Job Successful          | [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md) |
| Job Unstable            | [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md) |
| Job Failed              | [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md) |
| Job Aborted             | [EiffelActivityFinishedEvent](https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelActivityFinishedEvent.md) |

The plugin conforms to the
[Paris edition](https://github.com/eiffel-community/eiffel/releases/tag/edition-paris)
of the Eiffel protocol for the events it emits. Users of the provided
sendEiffelEvent pipeline step may choose to emit events from any Eiffel
edition up to and including the
[Arica edition](https://github.com/eiffel-community/eiffel/releases/tag/edition-arica).
See the documentation of each event for details of the corresponding event version used.

## Accessing emitted Eiffel events in builds
If a build needs to emit Eiffel events of its own they should probably have
a CONTEXT or CAUSE link to the build's EiffelActivityTriggeredEvent. The
plugins injects the following environment variables with the contents of
the previously sent events:
* `EIFFEL_ACTIVITY_TRIGGERED`: The build's EiffelActivityTriggeredEvent event.
* `EIFFEL_ACTIVITY_STARTED`: The build's EiffelActivityStartedEvent event.

## Category configuration

The contents of the `data.categories` member in EiffelActivityTriggeredEvent
is configurable in two ways:

* A global plugin setting.
* A setting in each job, configured via the UI or the `eiffelActivity` pipeline
  property:
```
properties([
    eiffelActivity(categories: ['jenkins', 'maven'])
])
```

Categories can e.g. be used to distinguish Eiffel activities that represent
Jenkins builds from other kinds of activities, or distinguish between
different kinds of Jenkins builds.

Globally configured categories will be merged with the categories specified
in each job. Duplicate entries will be eliminated.

## Pipeline steps

### createPackageURL

The createPackageURL pipeline step accepts individual components of a
[Package URL](https://github.com/package-url/purl-spec) and returns a properly
formatted and quoted string that e.g. can be used when piecing together an
EiffelArtifactedCreatedEvent. See the Package URL specification for details on
what the components mean.

| Argument    | Required | Description               |
| ------------|----------|---------------------------|
| type        | ✔        | The package "type" or package "protocol" such as maven, npm, nuget, gem, pypi, etc. |
| namespace   |          | A name prefix such as a Maven groupid, a Docker image owner, a GitHub user or organization. |
| name        | ✔        | The name of the package. |
| version     |          | The version of the package. |
| qualifiers  |          | A Groovy map with extra qualifying data for a package such as an OS, architecture, a distro, etc. |
| subpath     |          | Extra subpath within a package, relative to the package root. |

Example:
```
def purl = createPackageURL type: 'generic', namespace: 'name/space',
    name: 'pkgname', version: '1.0', subpath: 'some/path', qualifiers: [a: 'b']
echo "Here's the resulting purl: ${purl}"
```

### publishEiffelArtifacts

The publishEiffelArtifacts pipeline step sends an EiffelArtifactPublishedEvent
for each EiffelArtifactCreatedEvent that has been recorded in the build using
a sendEiffelEvent step with the `publishArtifact` argument enabled and for
each artifact recorded in the specified JSON files in the workspace.

This requires that each EiffelArtifactPublishedEvent has at least one file
defined in its `data.fileInformation` array and that each relative file path
in `data.fileInformation.name` matches a Jenkins artifact in the build.
Because of the latter requirement it's normally used after an
[archiveArtifacts](https://www.jenkins.io/doc/pipeline/steps/core/#code-archiveartifacts-code-archive-the-artifacts)
step.

The EiffelArtifactPublishedEvent will have two links; one ARTIFACT link to
the EiffelArtifactCreatedEvent and one CONTEXT link to the parent build's
EiffelActivityTriggeredEvent.

| Argument            | Required | Description               |
| --------------------|----------|---------------------------|
| artifactEventFiles  |          | An Ant-style glob expression that selects files containing JSON representations (one per line) of EiffelArtifactCreatedEvent to publish. |

Example of publishing artifacts connected to the build:
```
def event = [
    'meta': [
        'type': 'EiffelArtifactCreatedEvent',
        'version': '3.0.0',
    ],
    'data': [
        'identity': 'pkg:generic/myprogram@1.0',
        'fileInformation': [
            [
                'name': 'myprogram-1.0.tar.gz',
            ],
        ],
    ],
]
sendEiffelEvent event: event, publishArtifact: true

archiveArtifacts artifacts: 'myprogram-1.0.tar.gz'
publishEiffelArtifacts()
```

Example of publishing artifacts from a file:
```
def event = [
    'meta': [
        'type': 'EiffelArtifactCreatedEvent',
        'version': '3.0.0',
    ],
    'data': [
        'identity': 'pkg:generic/myprogram@1.0',
        'fileInformation': [
            [
                'name': 'myprogram-1.0.tar.gz',
            ],
        ],
    ],
]
writeJSON file: 'events.json', json: event

archiveArtifacts artifacts: 'myprogram-1.0.tar.gz'
publishEiffelArtifacts artifactEventFiles: '*.json'
```

### sendEiffelEvent

The sendEiffelEvent pipeline step sends an Eiffel event from that's built in
the Groovy code or read into a Groovy map from another location. It accepts
the following parameters:

| Argument          | Required | Description               |
| ------------------|----------|---------------------------|
| event             | ✔        | A map with the event payload. The `meta.id` and `meta.time` members will be populated automatically. |
| linkToActivity    |          | If true (default) the event sent will automatically include link to the current build's EiffelActivityTriggeredEvent. |
| activityLinkType  |          | The link type to use when linking to the EiffelActivityTriggeredEvent. Defaults to CONTEXT but can be set to CAUSE. |
| publishArtifact   |          | If true and the event being sent is EiffelArtifactCreatedEvent it will be recorded for possible later use by the publishEiffelArtifacts step. |

Example:
```
def event = [
    "meta": [
        "type": "EiffelCompositionDefinedEvent",
        "version": "3.0.0",
    ],
    "data": [
        "name": "my-composition",
    ],
]
def sent = sendEiffelEvent event: event
echo "This event was sent: ${sent}"

// Make the activity link a CAUSE link
sendEiffelEvent event: event, activityLinkType: "CAUSE"

// Skip the activity link altogether
sendEiffelEvent event: event, linkToActivity: false
```

This step returns immediately as soon as the event has been validated and put
in the internal outbound queue. The actual delivery of the event to the broker
might not have happened at the time of the return. The validation supports all
events and event versions up to and including the
[Arica edition](https://github.com/eiffel-community/eiffel/releases/tag/edition-arica).

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

## Maintainers

* Isac Holm \<isac.holm@axis.com\>
* Magnus Bäck \<magnus.back@axis.com\>

## License
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
