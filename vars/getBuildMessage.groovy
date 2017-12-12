#!/usr/bin/env groovy

def call() {
    info = getBuildInfo(currentBuild)
    failed_stages = []
    info.stages.each() { s ->
        if (s.status.toString() != 'SUCCESS') {
          failed_stages.add(s.name)
        }
    }
    failed_stages = failed_stages.join(',') + ' '

    log "${failed_stages}${currentBuild.absoluteUrl}", level: currentBuild.currentResult
    return "${currentBuild.currentResult}: ${failed_stages}${currentBuild.absoluteUrl}"
}
