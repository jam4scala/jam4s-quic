package org.jam4s.quic4cats

import net.luminis.quic.log.{ Logger as QLogger, SysOutLogger }

object QDefaults:

  def logger(): QLogger =
    val l = SysOutLogger()
    l.timeFormat(QLogger.TimeFormat.Long)
    l.logInfo(true)
    l.logWarning(true)
    l
