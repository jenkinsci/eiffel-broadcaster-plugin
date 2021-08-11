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
    sendEiffelEvent event: event, activityLinkType: 'CAUSE'
}
