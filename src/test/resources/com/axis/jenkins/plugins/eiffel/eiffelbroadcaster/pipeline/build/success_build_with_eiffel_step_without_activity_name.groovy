package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline.build

node {
    buildWithEiffel job: "downstream"
}
