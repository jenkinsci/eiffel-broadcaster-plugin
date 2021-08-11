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
    def sentEvent = sendEiffelEvent event: event
    writeJSON file: 'event.json', json: sentEvent
}
