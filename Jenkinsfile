#!/usr/bin/env groovy

/*
   Runs a command inside a virtual environment, creates the virtualenv if it does not exist.

   Keeping files inside the workspace is better than using system temp for portability, security and debugging.
*/
def venv(String environment='.venv', String script) {

    sh """#!/bin/bash
    set -ex
    [ ! -d "${environment}" ] && virtualenv --system-site-packages "${environment}"
    source ${environment}/bin/activate
    """ + script
}

timestamps {

  ansiColor('xterm') {

    node('master') {

        stage('prep') {

          // https://support.cloudbees.com/hc/en-us/articles/226122247-How-to-Customize-Checkout-for-Pipeline-Multibranch
          // clean needed to avoid potential leftovers from a reused workspace
          checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            extensions: scm.extensions + [[$class: 'CleanCheckout']],
            userRemoteConfigs: scm.userRemoteConfigs
            ])

          sh """set
          which python
          """

          venv '''
          pip install -U pip wheel setuptools
          pip freeze | tee pip-freeze.log
          pip check || "echo WARNING: pip checked returned $? error."
          which python
          '''
        }

        stage('lint') {
        }

        stage('unit') {
          venv '''
          pwd
          ls -la
          ./bin/sample.sh
          tox
          '''
        }

        stage('integration') {
        }

        stage('clean') {
        }

    }
  }
}
