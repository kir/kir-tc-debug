Kir debug plugin
================

  This is an internal plugin intended to collect some debugging information from TeamCity.
  See DebugAction and DataLogger classes for the actual code (so far, it writes data to TeamCity/logs/teamcity-kir.log)

  Debug logging is triggered by call

    http://teamcity-server/action.html?kir_debug=1&b1=<buildId1>&b2=<buildId2>

    where buildId1 and buildId2 should refer to two builds of the same configuration

