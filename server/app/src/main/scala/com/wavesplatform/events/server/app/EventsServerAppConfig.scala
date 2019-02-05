package com.wavesplatform.events.server.app

import com.wavesplatform.events.config.{EventsClientConfig, EventsServerConfig}

final case class EventsServerAppConfig(client: EventsClientConfig, server: EventsServerConfig)