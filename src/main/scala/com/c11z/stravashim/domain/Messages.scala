package com.c11z.stravashim.domain


/**
 * Created by c11z on 5/20/15.
 */

sealed trait ResponseMessage

/**
 * Typical Response message with status code 200 OK
 * @param content Anything that can be processed with json4s Extraction.decompose(a: Any)
 */
case class Http200(content: Any) extends ResponseMessage
case class Http200Empty() extends ResponseMessage
case class Http401(messages: List[Map[String, String]]) extends ResponseMessage

sealed trait RequestMessage

case class GetStatus(channelKey: String) extends RequestMessage

case class PostTestSetup(channelKey: String) extends RequestMessage

/**
 * Message object for converting Strava Athlete to IFTTT User
 * @param token OAuth token for strava api request
 */
case class GetUserInfo(token: String) extends RequestMessage

