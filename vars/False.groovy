#!/usr/bin/env groovy

/* groovy.lang.MissingPropertyException: No such property: False for class: WorkflowScript */
def call() {
  return false
}

public boolean equals(obj) {
    return obj == false;
}
