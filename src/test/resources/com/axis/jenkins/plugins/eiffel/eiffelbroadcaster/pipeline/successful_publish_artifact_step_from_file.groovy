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

    archiveArtifacts artifacts: '*.txt'
    writeFile file: 'events.json',
        text: events.collect { JsonOutput.toJson(it) }.join('\n')
    publishEiffelArtifacts artifactEventFiles: 'events.json'
}

