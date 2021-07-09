node {
    def event = [
            'meta': [
                    // Leaving out the mandatory 'type' key
                    'version': '3.0.0',
            ],
            'data': [
                    'name': 'foo',
            ],
    ]
    sendEiffelEvent event: event
}
