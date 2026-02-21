package template

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import scala.concurrent.ExecutionContext.Implicits.global

object Main:

  @main
  def mainEntry(): Unit = 
    val rootNode = dom.document.getElementById("app")

    if rootNode != null then

      val timeVar = Var("click button to load time from server")

      def loadTime(): Unit =
        dom.fetch("/api/time")
          .toFuture
          .foreach(x => x.text().toFuture.foreach(t => timeVar.set(t)))

      render(
        rootNode,
        div(
          cls := "mt-4",
          h2("Laminar mounted successfully!"),
          p(cls := "h5", "Server Time"),
          p(child.text <-- timeVar.signal),

          button(
            cls("btn", "btn-primary"),
            "Load Time",
            onClick --> { _ => loadTime() }
          )
        )
      )    