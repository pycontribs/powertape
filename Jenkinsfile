#!/usr/bin/env groovy

/*
   Runs a command inside a virtual environment, creates the virtualenv if it does not exist.

   Keeping files inside the workspace is better than using system temp for portability, security and debugging.
*/
def venv(String environment='.venv', String script) {

    sh """#!/bin/bash
    set -ex
    [ ! -d "${environment}" ] && virtualenv --system-site-packages --relocatable "${environment}"
    source ${environment}/bin/activate
    """ + script
}

timestamps {
  
  node('master') {

      stage('prep') {
        sh """set
        which python
        """

        venv '''
        pip install -U pip wheel setuptools
        pip freeze | tee pip-freeze.txt
        pip check || "echo WARNING: pip checked returned $? error."
        which python
        '''
      }

      stage('lint') {
      }

      stage('unit') {
      }

      stage('integration') {
      }

      stage('clean') {
      }

    }
}
