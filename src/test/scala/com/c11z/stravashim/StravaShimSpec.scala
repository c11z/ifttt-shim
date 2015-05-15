package com.c11z.stravashim

import org.scalatest.{ FreeSpec, Matchers }
import spray.http.StatusCodes._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

class StravaShimSpec extends FreeSpec with Matchers with Directives with ScalatestRouteTest with StravaShimService {
  def actorRefFactory = system

  "Strava shim for IFTTT should" - {

    "return a greeting for GET requests to the root path" in {
      Get() ~> shimRoute ~> check {
        status should equal(OK)
        entity.toString should include("IFTTT")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> shimRoute ~> check {
        handled should be(false)
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(shimRoute) ~> check {
        status == MethodNotAllowed
        entity.toString === "HTTP method not allowed, supported methods: GET"
      }
    }
  }

}