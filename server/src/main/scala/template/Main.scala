package template

import zio.*
import zio.http.*
import zio.http.codec.PathCodec.trailing
import zio.http.template.*

import java.io.File
import java.time.ZonedDateTime

object Main extends ZIOAppDefault:

  private val staticRoot = new File("./server/src/main/resources/static")

  private def pageFile(name: String): File =
    new File(staticRoot, s"page/$name.html")

  private val app = Routes(
    Method.GET / Root -> Handler.fromResource("static/index.html"),

    Method.GET / "api" / "health" -> handler(Response.text("OK")),

    Method.GET / "api" / "time" -> handler {
      ZIO.succeed(ZonedDateTime.now().toString()).map(Response.text)
    },

    Method.GET / "page" / string("name") -> handler { (name: String, _: Request) =>
      for
        file <- ZIO.succeed(pageFile(name))
        body <- if file.exists() then
          Body.fromFile(file)
        else
          ZIO.succeed(Body.empty)

        resp <- ZIO.succeed(
          Response(
            status = Status.Ok,
            headers = Headers(Header.ContentType(MediaType.text.html)),
            body = body
          )
        )
      yield resp
    }
  ).sandbox
    @@ Middleware.serveDirectory(Path("assets"), new File("./server/src/main/resources/static/assets"))
    @@ Middleware.serveDirectory(Path("static"), new File("./server/src/main/resources/static"))

  def run = Server.serve(app).provide(Server.default)

