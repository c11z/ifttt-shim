package com.c11z.ifttt.shim.channel.strava

import akka.actor.{Actor, ActorRef}
import com.c11z.ifttt.shim.domain._
import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Properties, Success}

/**
 * Describes operations with the Strava API in response to IFTTT requests.
 */
trait StravaOperations {
  val conf = ConfigFactory.load()
  val test = conf.getConfig("test")

  implicit val formats = DefaultFormats

  def validateChannelKey(channelKey: String): Boolean = {
    val fakeChannelKey = test.getString("fake-channel-key")
    val trueChannelKey = Properties.envOrElse("STRAVA_CHANNEL_KEY", fakeChannelKey)
    if(channelKey == trueChannelKey) true else false
  }

  def status(channelKey: String) = {
    validateChannelKey(channelKey) match {
      case true => Http200Empty()
      case false => Http401("Invalid IFTTT-Channel-Key")
    }
  }

  /**
   * Handles test/setup endpoint, returns a valid test token for IFTTT to use in testing the channel.
   * @param channelKey A key identifying IFTTT's access to this channel.
   * @return
   */
  def testSetup(channelKey: String) = {
    validateChannelKey(channelKey) match {
      case true =>
        val fakeToken = test.getString("fake-access-token")
        val token = Properties.envOrElse("STRAVA_ACCESS_TOKEN", fakeToken)
        val content = "data" -> ("accessToken" -> token) ~ ("samples" -> Nil)
        Http200(content)
      case false => Http401("The Channel Key is present but not valid for this channel")
    }
  }

  /**
   * Handles user/info endpoint. Calls Strava athlete and transforms to IFTTT user
   * @param token OAuth token for api call
   * @param requestor Immutable copy of the sender to resolve future
   */
  def athlete(token: String, requestor: ActorRef) = {
    val athleteF = StravaClient.getAthlete(token)
    athleteF onComplete {
      case Success(athlete) =>
        val JString(first) = athlete \ "firstname"
        val JString(last) = athlete \ "lastname"
        val JInt(id) = athlete \ "id"
        val json = "data" -> ("name" -> s"$first, $last") ~ ("id" -> id.toString)
        requestor ! Http200(json)
      case Failure(ex) => requestor ! Http401(ex.getMessage)
    }
  }

  /**
   * Handles triggers/new_personal_record endpoint. Gets the last 'athlete-activity-limit'(from application.conf)
   * of activity summaries, concurrently calls the full activities, extracts the personal records from the segment
   * efforts and returns a collection of personal record details. A very heavy operation requiring ~ 21 api calls.
   * @param token OAuth token for api call
   * @param trigger Json entity identifying trigger call and containing parameters
   * @param requestor Immutable copy of the sender to resolve future
   */
  def personalRecords(token: String, trigger: String, requestor: ActorRef) = {
    val jsonReq = parse(trigger)
    // ignore trigger identity for now
    // ignore timezone for now, not sure what all the options might be, the one string in the test cases doesn't seem
    // to be standardized.
    // no triggerFields for first pass at New Personal Record
    val limit = jsonReq \ "limit" match {
      case JInt(i) => i.toInt
      case _ => 50
    }

    val athleteActivitiesF: Future[JValue] = StravaClient.getAthleteActivities(token)
    val activityIdsF = for(JArray(summaryActivites) <- athleteActivitiesF)
      yield this.parseActivites(summaryActivites, ListBuffer())

    val activitiesF: Future[List[Future[List[JValue]]]] = for(activityIds <- activityIdsF)
      yield for(activityId <- activityIds)
        yield for (activity <- StravaClient.getActivity(token, activityId))
          yield this.parseActivity(activity)

    // Tricky bit to remove future nesting
    val effortsF: Future[List[List[JValue]]] = activitiesF.flatMap(effortF => Future.sequence(effortF))

    effortsF onComplete {
      case Success(efforts) =>
        val json = "data" -> efforts.flatten.take(limit)
        requestor ! Http200(json)
      case Failure(ex) => requestor ! Http401(ex.getMessage)
    }
  }

  /**
   * Recurses through activity list and returns a list of activity id's with achievements associated with them.
   * @param activities List of json activities
   * @param result list of activity Id's known to have achievements
   * @return
   */
  @tailrec
  private def parseActivites(activities: List[JValue], result: ListBuffer[BigInt]): List[BigInt] = {
    if(activities.isEmpty) result.toList
    else {
      val activity = activities.head
      val JInt(achievementCount) = activity \ "achievement_count"
      if(achievementCount > 0) {
        val JInt(id) = activity \ "id"
        parseActivites(activities.tail, result += id)
      } else parseActivites(activities.tail, result)
    }
  }

  /**
   * Extracts personal records from full activity responses. Uses recursive closure to loop the achievements list.
   * Returns List of personal record results.
   * @param activity Activity json to be parsed.
   * @return
   */
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
                ("id" -> id.toString()) ~ ("timestamp" -> DateTime.parse(createdAt).getMillis))
          parseEffort(efforts.tail, result += json)
        } else parseEffort(efforts.tail, result)
      }
    }

    parseEffort(efforts, ListBuffer())
  }
}


/**
 * Actor that handles Request Messages with Strava Operations.
 */
class StravaActor extends Actor with StravaOperations {
  override def receive: Receive = {
    case GetStatus(channelKey) => sender ! status(channelKey)
    case PostTestSetup(channelKey) => sender ! testSetup(channelKey)
    case GetUserInfo(token) => athlete(token, sender())
    case NewPersonalRecord(token, trigger) => personalRecords(token, trigger, sender())
  }
}

