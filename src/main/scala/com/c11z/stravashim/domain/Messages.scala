package com.c11z.stravashim.domain

import org.json4s.JsonAST.JValue

/**
 * Created by c11z on 5/20/15.
 */

sealed trait ResponseMessage

/**
 * Typical Response message with json body
 * @param json response body as json string
 */
case class Json(json: JValue) extends ResponseMessage
case class Good() extends ResponseMessage
case class Bad(message: String) extends ResponseMessage

sealed trait RequestMessage

case class GetStatus(channelKey: String) extends RequestMessage

case class PostTestSetup(channelKey: String) extends RequestMessage

/**
 * Message object for converting Strava Athlete to IFTTT User
 * @param token OAuth token for strava api request
 */
case class GetUserInfo(token: String) extends RequestMessage

