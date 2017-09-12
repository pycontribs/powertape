#!/usr/bin/env groovy

/*
  niceprefix() would generate a safe prefix (less than 40 chars) that can be
  used for naming resources (hostnames, os_networks, os_instances,...)

  The 40 chars limit is based on the fact that hostnames are limtied to 62 chars
  by RFC . The rest of the chars are reserved for IR suffixes (-controller0)

  OpenStack resource names are limited to 255 chars so they are not affected by
  the same limitation.

  Current implementation returns a string with this format:

  DDHH-<shortened-job-name>-<hash>

  DDHH - current day and hour when build was triggers, helps determine *age*
  and allows us to sort resources by their age (both horizon or cli)

  hash - is a short key that assures us that we don't generate duplicate prefixes
  by mistake. Is generated using md5(BUILD_URL)[:4]

  shortened-job-name is an an ugly attempt to use acronyms to shorten job names
  by minizing the lost information. It does not guarantee that two different
  jobs would not endup having the same shortened name. The only scope of the
  shortened name is to allow us to get an idea about which kind of job
  allocated that resource in case we have to investigat resource leaks.
  */
import java.security.MessageDigest


String call() {

  String build_name = env.BUILD_TAG
  // 4 chars for '-XXX' hash suffix
  // 12 chars for infrared suffixes like '-controller-0'
  // 5 chars for allowing us to add 'DDHH-' time prefix
  // def MAX_SIZE = 63 - 4 - 14 - 5

  // https://wiki.jenkins.io/display/JENKINS/Building+a+software+project
  String shortened = build_name.toLowerCase().replaceAll('_','-')
  .replaceAll(/\./,'')
  .replaceAll( /jenkins-|dfg-|-rhos|rhos-|-rhel|python-|-\d(cont|ceph|db|msg|net|comp)|virt|-(from|with)/, '')
  .replaceAll('-ipv','')
  .replaceAll('all-in-one','aio')
  .replaceAll('baremetal','bm')
  .replaceAll('bonding','bnd')
  .replaceAll('compat','cpt')
  .replaceAll('composable','cp')
  .replaceAll('containers','cnt')
  .replaceAll('ctlplane','cpl')
  .replaceAll('custom(ized)?','cst')
  .replaceAll('deploy(ment)?','dpl')
  .replaceAll('fullstack','fs')
  .replaceAll('gate','gt')
  .replaceAll('guest','g')
  .replaceAll('image','img')
  .replaceAll('infrared','ir')
  .replaceAll('manila','man')
  .replaceAll('mixed-versions','mxv')
  .replaceAll('monolithic','monolit')
  .replaceAll('multijob','mj')
  .replaceAll('network','net')
  .replaceAll('neutron','neu')
  .replaceAll('nightly','n')
  .replaceAll('opendaylight','old')
  .replaceAll('openstack','os')
  .replaceAll('packstack','ps')
  .replaceAll('performance','perf')
  .replaceAll('phase','p')
  .replaceAll('rabbitmq','rmq')
  .replaceAll('risk','r')
  .replaceAll('sahara','sah')
  .replaceAll('secgroups','sg')
  .replaceAll('single-port','1p')
  .replaceAll('stage','st')
  .replaceAll('storage','sto')
  .replaceAll('two-ports','2p')
  .replaceAll('virthost','vh')
  .replaceAll('workflow','wf')
  .replaceAll(/(\d+)-director/,'$1d')
  .replaceAll(/(upgrade|update)[s]?/,'u')
  .replaceAll(/multi-?node/,'mn')
  .replaceAll('-external','-ext')
  .replaceAll('-minimal','-min')
  .replaceAll('-tempest','-tpst')
  .replaceAll('-workarounds','-wkr')
  .replaceAll('--','-')

  String result = new Date().format('ddHH-') + shortened.take(40) + '-' + md5(build_name, 4)

  log "[niceprefix] ${build_name} shortened as \u001B[34m${result}", level='DEBUG'
  if (result.size() > 63) {
    throw new RuntimeException("niceprefix failed to generate safe prefix.")
  }
  return result
}
