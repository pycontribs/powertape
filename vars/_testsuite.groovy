#!/usr/bin/env groovy


def _testsuite() {

    stage('gitClean') {
        gitClean()
    }

    stage("mkdtemp") {
       def x = mkdtemp('cd-')
       println "${x}"
    }

    stage("md5") {
       def jobname = "${env.JOB_NAME}#${env.BUILD_NUMBER}"
       def x = md5(jobname, 6)
       println "md5('${jobname}', 6) => ${x}"
    }

    stage("sh2") {
        withEnv(["MAX_LINES=2"]) {

            // should display 1,2,4,5 (missing 3) and output.log
            def result = sh2 script: "seq 5; exit 200", returnStatus: true
            print "[${result}]"
            if (result != 200) currentBuild.result = 'FAILURE'

            // should generate output-1.log
            sh2 "set"

            // this should not generate a log file or limit the output due
            // to returnStdout: true
            result = sh2 script: "seq 5", returnStdout: true
            print "[${result}]"
            // normalize output
            result = result.replaceAll('\\r\\n?', '\n').trim()
            if (result != '1\n2\n3\n4\n5') currentBuild.result = 'FAILURE'

            // currently you need to archive log manually, but next sh2()
            // version will take care of that
            archiveArtifacts artifacts: '*.log'
        }
    }
}