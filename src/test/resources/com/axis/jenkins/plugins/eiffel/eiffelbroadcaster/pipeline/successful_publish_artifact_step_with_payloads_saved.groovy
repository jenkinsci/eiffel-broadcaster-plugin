import groovy.json.JsonOutput

node {
    def events = [
            [
                    'meta': [
                            'type': 'EiffelArtifactCreatedEvent',
                            'version': '3.0.0',
                    ],
                    'data': [
                            'identity': 'pkg:generic/foo',
                            'fileInformation': [
                                    [
                                            'name': 'a.txt',
                                    ],
                                    [
                                            'name': 'b.txt',
                                    ],
                            ],
                    ],
            ],
            [
                    'meta': [
                            'type': 'EiffelArtifactCreatedEvent',
                            'version': '3.0.0',
                    ],
                    'data': [
                            'identity': 'pkg:generic/foo',
                            'fileInformation': [
                                    [
                                            'name': 'c.txt',
                                    ],
                                    [
                                            'name': 'd.txt',
                                    ],
                            ],
                    ],
            ],
    ]
    events.collect { it.data.fileInformation }.flatten()*.name.each {
        writeFile file: it, text: ''
    }

    sendEiffelEvent event: events[0], publishArtifact: true

    archiveArtifacts artifacts: '*.txt'
    writeFile file: 'artifacts.json', text: JsonOutput.toJson(events[1])
    def publishedEvents = publishEiffelArtifacts artifactEventFiles: 'artifacts.json'
    writeFile file: 'events.json', text: publishedEvents.collect { JsonOutput.toJson(it) }.join('\n')
}
