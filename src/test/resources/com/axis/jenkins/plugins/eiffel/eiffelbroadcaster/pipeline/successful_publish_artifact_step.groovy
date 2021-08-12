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
                        'name': 'b.txt',
                    ],
                ],
            ],
        ],
    ]
    events.each {
        sendEiffelEvent event: it, publishArtifact: true
        it.data.fileInformation.each {
            writeFile file: it.name, text: ''
        }
    }

    archiveArtifacts artifacts: '**'
    publishEiffelArtifacts()
}

