#!/usr/bin/env groovy
/* Logs jenkins console message at different levels: DEBUG, INFO, WARN,
ERROR which could appear colored when console supports ANSI coloring.
*/

// def ansi_message(msg){
//   return {
//     echo msg
//   }
// }

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

    def levels = [
      'DEBUG': '\u001B[34m', // blue
      'INFO': '\u001B[32m', // green
      'WARN': '\u001B[33m', // yellow
      'ERROR': '\u001B[31m', // red
      'UNSET': '\u001B[0m' // clear/defaults
    ]

    if ((env.TERM ?: '').toLowerCase().contains('xterm')) {
      ansiColor {
        echo "${levels[level]}${level}: ${msg}${levels['UNSET']}"
      }
    }
    else {
        echo "${level}: ${msg}"
    }
}
//
// def call(def msg) {
//     // msg can be non String
//     return log (msg: msg, level: 'INFO')
// }
//
// def call(def msg, Map params) {
//     // msg can be non String
//     params.msg = msg
//     return log (params)
// }
