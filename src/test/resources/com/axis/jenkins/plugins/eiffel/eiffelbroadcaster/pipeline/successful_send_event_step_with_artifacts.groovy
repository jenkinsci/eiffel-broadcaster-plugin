node {
    def event = [
            'meta': [
                    'type': 'EiffelArtifactCreatedEvent',
                    'version': '3.0.0',
            ],
            'data': [
                    'identity': 'pkg:generic/foo',
            ],
    ]
    sendEiffelEvent event: event, publishArtifact: true
    sendEiffelEvent event: event, publishArtifact: true
}
