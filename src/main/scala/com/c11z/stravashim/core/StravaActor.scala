package com.c11z.stravashim.core

import akka.actor.{Actor, ActorRef}
import com.c11z.stravashim.domain._
import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

/**
 * Created by c11z on 5/20/15.
 */


trait StravaOptions {
  val conf = ConfigFactory.load()
  val secret = conf.getConfig("secret")
  val test = conf.getConfig("test")

  implicit val formats = DefaultFormats

  def validateChannelKey(channelKey: String): Boolean = {
    if(channelKey == secret.getString("ifttt-channel-key")) true else false
  }

  def status(channelKey: String) = {
    validateChannelKey(channelKey) match {
      case true => Http200Empty()
      case false => Http401("Invalid IFTTT-Channel-Key")
    }
  }

  def testSetup(channelKey: String) = {
    validateChannelKey(channelKey) match {
      case true =>
        val token = test.getString("access-token")
        val content = "data" -> ("accessToken" -> token) ~ ("samples" -> Nil)
        Http200(content)
      case false => Http401("The Channel Key is present but not valid for this channel")
    }
  }

  /**
   * Calls Strava and parses Athlete into IFTTT User
   * @param token OAuth token for api call
   * @param requestor Immutable copy if the sender to resolve future
   */
  def athlete(token: String, requestor: ActorRef) = {
    val data = for(res <- StravaClient.getAthlete(token)) yield for {
      JString(first) <- res \ "firstname"
      JString(last) <- res \ "lastname"
      JInt(id) <- res \ "id"
    } yield "data" -> ("name" -> s"$first $last") ~ ("id" -> id.toString)

    data onComplete {
      case Success(content) => requestor ! Http200(content)
      case Failure(ex) => requestor ! Http401(ex.getMessage)
    }
  }

  def personalRecords(token: String, trigger: String, requestor: ActorRef) = {
    val jsonReq = parse(trigger)
    // ignore trigger identity for now
    // ignore timezone for now, not sure what all the options might be
    // no triggerFields for first pass at New Personal Record
    val limit = jsonReq \ "limit" match {
      case JInt(i) => i.toInt
      case JNothing => 50
    }

    val athleteActivityRes: Future[JValue] = StravaClient.getAthleteActivities(token)
    val activityIdsF = for(JArray(summaryActivites) <- athleteActivityRes)
      yield this.parseActivites(summaryActivites, ListBuffer(), limit)

    val activitiesF: Future[List[Future[List[JValue]]]] = for(activityIds <- activityIdsF)
      yield for(activityId <- activityIds)
        yield for (activity <- StravaClient.getActivity(token, activityId))
          yield this.parseActivity(activity)

    val effortsF: Future[List[List[JValue]]] = activitiesF.flatMap(effortF => Future.sequence(effortF))

    effortsF onComplete {
      case Success(efforts) =>
        val json = "data" -> efforts.flatten.take(limit)
        requestor ! Http200(json)
      case Failure(ex) => requestor ! Http401(ex.getMessage)
    }
  }

  @tailrec
  private def parseActivites(activities: List[JValue], result: ListBuffer[BigInt], countDown: Int): List[BigInt] = {
    if(activities.isEmpty) result.toList
    else {
      val activity = activities.head
      val JInt(achievementCount) = activity \ "achievement_count"
      if(achievementCount > 0) {
        val JInt(id) = activity \ "id"
        parseActivites(activities.tail, result += id, countDown - achievementCount.toInt)
      } else parseActivites(activities.tail, result, countDown)
    }
  }

  private def parseActivity(activity: JValue): List[JValue] = {
    val JString(createdAt) = activity \ "start_date"
    val JString(activityName) = activity \ "name"
    val JString(activityType) = activity \ "type"
    val JArray(efforts) = activity \ "segment_efforts"

    @tailrec
    def parseEffort(efforts: List[JValue], result: ListBuffer[JValue]): List[JValue] = {
      if(efforts.isEmpty) result.toList
      else {
        val effort = efforts.head
        val JInt(id) = effort \ "id"
        val JString(segmentName) = effort \ "segment" \ "name"
        val rank = (effort \ "pr_rank").values match {
          case 1 => Some("1st")
          case 2 => Some("2nd")
          case 3 => Some("3rd")
          case null => None
        }
        if(rank.isDefined) {
          val json =
            ("activity_name" -> activityName) ~
              ("segment_name" -> segmentName) ~
              ("activity_type" -> activityType) ~
              ("rank" -> rank.get) ~
              ("created_at" -> createdAt) ~
              ("meta" ->
                ("id" -> id) ~ ("timestamp" -> (System.currentTimeMillis / 1000)))
          parseEffort(efforts.tail, result += json)
        } else parseEffort(efforts.tail, result)
      }
    }

    parseEffort(efforts, ListBuffer())
  }
}


/**
 * Actor to provide operations with the Strava API
 */
class StravaActor extends Actor with StravaOptions {
  override def receive: Receive = {
    case GetStatus(channelKey) => sender ! status(channelKey)
    case PostTestSetup(channelKey) => sender ! testSetup(channelKey)
    case GetUserInfo(token) => athlete(token, sender())
    case NewPersonalRecord(token, trigger) => personalRecords(token, trigger, sender())
  }
}

