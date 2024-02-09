node {
    def event = [
            'meta': [
                    'type': 'EiffelArtifactCreatedEvent',
                    'version': '3.3.0',
            ],
            'data': [
                    'identity': 'pkg:foo',
            ],
    ]
    sendEiffelEvent event: event, signatureCredentialsId: 'event_signing', signatureHashAlgorithm: 'SHA-512'
}
