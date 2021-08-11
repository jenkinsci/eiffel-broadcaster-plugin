node {
    def event = [
            'meta': [
                    'type': 'EiffelCompositionDefinedEvent',
                    'version': '3.0.0',
            ],
            'data': [
                    // Leaving out mandatory 'name' key
            ],
    ]
    sendEiffelEvent event: event
}
