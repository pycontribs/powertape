#!/usr/bin/env groovy

/*
   Runs a command inside a virtual environment, creates the virtualenv if it does not exist.

   Keeping files inside the workspace is better than using system temp for portability, security and debugging.
*/

def venv(String environment = '.venv', String script) {
    /*
        PS=1 is a workaround for https://github.com/pypa/virtualenv/issues/1029
     */
    sh """#!/bin/bash
    set -ex

    if [ ! -d "${environment}" ]; then

        pip check >/dev/null || {
            # keep the force here, we had too many cases where pip reported upgraded version
            # but when you run it it will run an older version.
            pip install --force-reinstall -U pip
        }

        # using system gives great speed and disk usage improvements
        pip check && VIRTUALENV_SYSTEM_SITE_PACKAGES=1 || \
                echo "WARNING: Conflicts found on system python packages, for safety " \
                     "we will create and use an isolated virtualenv. Slower but safer."

        virtualenv "${environment}"
        PS1= source ${environment}/bin/activate
        # default version of pip installed in virtualenv is the ancient ~1.4 and mostly broken
        pip install -q -U pip
        # we also need a recent version of setuptools to avoid installation failures
        pip install -q -U setuptools
    else
        PS1= source ${environment}/bin/activate
    fi
    # assuring that we have no conflicts inside the virtualenv
    pip check
    """ + script
}