#!/usr/bin/env groovy
/* Logs jenkins console message at different levels: DEBUG, INFO, WARN,
ERROR which could appear colored when console supports ANSI coloring.
*/


def call(Map params = [:], def msg = null) {
/* This signature allow all sorts of calling types, including:

  log "foo", level: 'WARN'
  log msg: "foo", level: 'WARN'
  log "foo"
  log Exception("xxx") (or anything that has toString()

*/
    if (!msg) {
      msg = params.msg
    }
    level = params.level ?: 'INFO'

    echo "${getANSI(level)}${level}: ${msg}${getANSI()}"
}

def call(def msg) {
    // msg can be non String
    return log (msg: msg, level: 'INFO')
}
