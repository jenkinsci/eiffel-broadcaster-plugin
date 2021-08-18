node {
    def event = [
        'meta': [
            'type': 'EiffelCompositionDefinedEvent',
            'version': '3.0.0',
        ],
        'data': [
            'name': 'foo',
        ],
    ]

    writeJSON file: 'events.json', json: event
    publishEiffelArtifacts artifactEventFiles: 'events.json'
}

