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
case class Http401(messages: String) extends ResponseMessage

sealed trait RequestMessage

case class GetStatus(channelKey: String) extends RequestMessage

case class PostTestSetup(channelKey: String) extends RequestMessage

/**
 * Message object for converting Strava Athlete to IFTTT User
 * @param token OAuth token for strava api request
 */
case class GetUserInfo(token: String) extends RequestMessage

/**
 * Message for requesting trigger for new personal records
 * @param token OAuth token for strava api request
 * @param trigger Json containing fields specific to the request
 */
case class NewPersonalRecord(token: String, trigger: String) extends RequestMessage

