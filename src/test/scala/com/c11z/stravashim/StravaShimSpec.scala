package com.c11z.stravashim

import org.scalatest.{ FlatSpec, Matchers }
import spray.http.{HttpRequest, HttpHeader}
import spray.http.HttpHeaders.{RawHeader, `Accept-Charset`, Accept}
import spray.http.HttpCharsets.`UTF-8`
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

class StravaShimSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest with StravaShimService {
  def actorRefFactory = system

  val defaultHeaders: List[HttpHeader] = List(
    Accept(`application/json`),
    `Accept-Charset`(`UTF-8`),
    RawHeader("IFTTT-Channel-Key", "vFRqPGZBmZjB8JPp3mBFqOdt"),
    RawHeader("X-Request-ID", "0715f98e65f749aba2fc243eac1e3c09")
  )

  val req = HttpRequest()

  "StravaShim for IFTTT" should "have status endpoint" in {
    Get("/ifttt/v1/status").withHeaders(defaultHeaders) ~> shimRoute ~> check {
      status should equal(OK)
      mediaType should equal(`application/json`)
    }
  }

  it should "not find anything at home" in {
    Get() ~> shimRoute ~> check {
      handled should be(false)
    }
  }

  // "Strava shim for IFTTT should" - {

  //   "return a greeting for GET requests to the root path" in {
  //     Get() ~> shimRoute ~> check {
  //       status should equal(OK)
  //       entity.toString should include("IFTTT")
  //     }
  //   }

  //   "leave GET requests to other paths unhandled" in {
  //     Get("/kermit") ~> shimRoute ~> check {
  //       handled should be(false)
  //     }
  //   }

  //   "return a MethodNotAllowed error for PUT requests to the root path" in {
  //     Put() ~> sealRoute(shimRoute) ~> check {
  //       status == MethodNotAllowed
  //       entity.toString === "HTTP method not allowed, supported methods: GET"
  //     }
  //   }
  // }

}