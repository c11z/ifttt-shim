package com.c11z.stravashim.domain


/**
 * Response Messages are organized by Http Status Codes and their payloads.
 */
sealed trait ResponseMessage

case class Http200(content: Any) extends ResponseMessage
case class Http200Empty() extends ResponseMessage
case class Http401(messages: String) extends ResponseMessage


/**
 * Request messages are endpoint specific, and have unique combinations of parameters and actor handlers.
 */
sealed trait RequestMessage

/**
 * Strava Specific RequestMessage
 */
case class GetStatus(channelKey: String) extends RequestMessage
case class PostTestSetup(channelKey: String) extends RequestMessage
case class GetUserInfo(token: String) extends RequestMessage
case class NewPersonalRecord(token: String, trigger: String) extends RequestMessage

/** NEW Channel RequestMessages Here **/
