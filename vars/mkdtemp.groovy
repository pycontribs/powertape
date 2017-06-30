#!/usr/bin/env groovy

import java.nio.file.Files

String call(String prefix=null) {

    def mydir = Files.createTempDirectory(prefix).toString()
          addShutdownHook {
              def x = new File(mydir)
              x.deleteDir()
              println "cleaned ${mydir}"
          }
    mydir
}
