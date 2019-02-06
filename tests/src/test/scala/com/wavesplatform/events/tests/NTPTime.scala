package com.wavesplatform.events.tests

import com.wavesplatform.utils.NTP

trait NTPTime {
  protected lazy val ntpTime = {
    val ntpTime = new NTP("pool.ntp.org")
    Runtime.getRuntime.addShutdownHook(new Thread(() => ntpTime.close()))
    ntpTime
  }
}
