#!/usr/bin/env groovy
/*
   Shared libraries cannot be tested with CR/PR, as Jenkins can only load
   a library from a defined branch.

   Due to this, we will merge to @master only from green branches.
*/
@Library('powertape') _

properties([
        // https://st-g.de/2016/12/parametrized-jenkins-pipelines
        parameters([
                booleanParam(
                  name: 'DEBUG',
                  defaultValue: true,
                  description: 'Enables debug mode which increase verbosity level.'),
                string(
                  name: 'GERRIT_REFSPEC',
                  defaultValue: '+refs/heads/master:refs/remotes/origin/master',
                  description: 'The REFSPEC of the component'),
                string(
                  name: 'GERRIT_BRANCH',
                  defaultValue: 'master',
                  description: 'The branch of the component')

        ]),

        // schedule job to run daily between 1am-2am
        pipelineTriggers([
                // [$class: 'GitHubPushTrigger'], // enable only if plugin is installed
                pollSCM('H/5 * * * *'),
                cron('H 01 * * *')
        ])
])


timestamps {

    // some tools could fail if no TERM is defined
    env.TERM = env.TERM ?: 'xterm-color'

    // Inspired from http://unix.stackexchange.com/questions/148/colorizing-your-terminal-and-shell-environment
    env.ANSIBLE_FORCE_COLOR = env.ANSIBLE_FORCE_COLOR ?: 'true'
    env.CLICOLOR = env.CLICOLOR ?: '1'
    env.LSCOLORS = env.LSCOLORS ?: 'ExFxCxDxBxegedabagacad'

    node('master') {

        try {

            // we don't want any leftovers to influence our execution (like previous logs)
            step([$class: 'WsCleanup'])

            checkout scm

            // start-of-unittests
            stage('gitClean') {
                gitClean()
            }

            sh "set > .envrc"

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
                    // should display 1,2,4,5 (missing 3) and sh.log
                    echo "[sh2] 001"
                    def result = sh2 script: "pwd; seq 5; exit 0", returnStatus: true

                    if (result != 0) {
                        println "ERROR: Got [${result}] status code instead of expected [0]"
                        currentBuild.result = 'FAILURE'
                    }

                    echo "[sh2] 002"
                    sh "mkdir -p foo"
                    dir("foo") {
                      // should generate and archive $WORKSPACE/.sh/ansitest.log
                      // even if the current directory is $WORKSKAPCE/foo
                      withEnv(['STAGE_NAME=foo']) {
                          sh2 "echo 'foo!'; seq 10"
                          sh2 script: "echo 'foo2!'; seq 10", compress: true
                          }
                    }

                    echo "[sh2] 003"
                    // should create "date.log"
                    sh2 script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"',
                        basename: "date"

                    echo "[sh2] 004"
                    // should generate sh-1.log.gz
                    sh2 script: """#!/bin/bash
                        for i in \$(seq 100)
                        do
                          printf "\$i\n"
                          sleep 0.5
                        done""",
                        compress: true,
                        progressSeconds: 5

                    echo "[sh2] 005"
                    // this should not generate a log file or limit the output due
                    // to returnStdout: true
                    result = sh2 script: "seq 5", returnStdout: true
                    println "result = [${result}]"
                    // normalize output
                    result = result.replaceAll('\\r\\n?', '\n').trim()
                    if (result != '1\n2\n3\n4\n5') {
                        println "FAILURE: Unexpected result: [$result]"
                        currentBuild.result = 'FAILURE'
                    }

                    echo "[sh2] 006"
                    sh2 basename: "sh-006", script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"'

                    echo "[sh2] tox"
                    sh2 basename: "tox", 'tox'

                    echo "[niceprefix] ${niceprefix()}"
                }
            }
            // end-of-unittests
        } // end-try
        catch (error) {
            println "Exception ${error.getClass()} received: ${error}"

            notifyBuild(currentBuild.result, error=errror)
            throw error
        }
        finally {
            stage('archive') {

                archiveArtifacts artifacts: '.envrc',
                                 fingerprint: false,
                                 allowEmptyArchive: true
                // defaultExcludes: true
                // caseSensitive: false
                // onlyIfSuccessful: false

                notifyBuild(currentBuild.result)

            } // end-clean
        } // finally
    } // node
} // timestamps
