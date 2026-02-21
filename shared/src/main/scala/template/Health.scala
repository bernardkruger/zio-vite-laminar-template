package template


import zio.json.*

case class Health(status: String)

object Health:
  given jsonCodec: JsonCodec[Health] = DeriveJsonCodec.gen
