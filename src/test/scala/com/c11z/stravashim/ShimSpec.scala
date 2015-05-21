package com.c11z.stravashim

import com.c11z.stravashim.api.ShimService
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import spray.http.GenericHttpCredentials
import spray.http.HttpCharsets.`UTF-8`
import spray.http.HttpHeaders.{Accept, RawHeader, `Accept-Charset`, `Authorization`}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

class ShimSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest with ShimService {
  def actorRefFactory = system

  val conf = ConfigFactory.load()
  val secret = conf.getConfig("secret")
  val test = conf.getConfig("test")

  val basicResponseHeaders = List(
    Accept(`application/json`),
    `Accept-Charset`(`UTF-8`),
    RawHeader("X-Request-ID", "0715f98e65f749aba2fc243eac1e3c09")
  )
  val testHeaders = RawHeader("IFTTT-Channel-Key", secret.getString("ifttt-channel-key")) :: basicResponseHeaders

  val badTestHeaders = RawHeader("IFTTT-Channel-Key", "INVALID") :: basicResponseHeaders

  "StravaShim for IFTTT" should "not find anything at home" in {
    Get() ~> shimRoute ~> check {
      handled should be(false)
    }
  }

  "Status endpoint" should "succeed if passed proper Channel Key" in {
    Get("/ifttt/v1/status").withHeaders(testHeaders) ~> shimRoute ~> check {
      status should equal(OK)
      mediaType should equal(`application/json`)
    }
  }

  it should "fail if the the Channel Key is invalid" in {
    Get("/ifttt/v1/status").withHeaders(badTestHeaders) ~> shimRoute ~> check {
      status should equal(Unauthorized)
    }
  }

  "test/setup endpoint" should "Allow post request and return an accessToken" in {
    Post("/ifttt/v1/test/setup").withHeaders(testHeaders) ~> shimRoute ~> check {
      status should equal(OK)
      responseAs[String] === test.getString("setup-response")
    }
  }

  it should "Fail post request if the Channel Key is invalid" in {
    Post("/ifttt/v1/test/setup").withHeaders(badTestHeaders) ~> shimRoute ~> check {
      status should equal(Unauthorized)
    }
  }

  val cred = GenericHttpCredentials("bearer", s"${test.getString("user-token")}")
  val userHeaders = `Authorization`(cred) :: basicResponseHeaders

  "user/info endpoint" should "fail if there is no Authorization Header set" ignore {
    Post("/ifttt/v1/user/info") ~> shimRoute ~> check {
      status should equal(Unauthorized)
    }
  }

  it should "mock a reasonable response for the test" ignore {}
}