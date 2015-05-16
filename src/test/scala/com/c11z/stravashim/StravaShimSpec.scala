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
    RawHeader("IFTTT-Channel-Key", "wq3U2-THKBYt08VBJOmT_B304rhQD6MND5friCCH_KYJIvhmKGvC-MH72weSPCQ3"),
    RawHeader("X-Request-ID", "0715f98e65f749aba2fc243eac1e3c09")
  )

  val badHeaders = List(
    Accept(`application/json`),
    `Accept-Charset`(`UTF-8`),
    RawHeader("IFTTT-Channel-Key", "INVALID"),
    RawHeader("X-Request-ID", "0715f98e65f749aba2fc243eac1e3c09")
  )

  "StravaShim for IFTTT" should "not find anything at home" in {
    Get() ~> shimRoute ~> check {
      handled should be(false)
    }
  }

  "Status endpoint" should "succeed if passed proper Channel Key" in {
    Get("/ifttt/v1/status").withHeaders(defaultHeaders) ~> shimRoute ~> check {
      status should equal(OK)
      mediaType should equal(`application/json`)
    }
  }

  it should "fail if the the Channel Key is invalid" in {
    Get("/ifttt/v1/status").withHeaders(badHeaders) ~> shimRoute ~> check {
      status should equal(Unauthorized)
    }
  }
}