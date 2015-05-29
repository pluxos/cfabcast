package cfpaxos.agents

import akka.actor._
import cfpaxos._
import cfpaxos.messages._
import cfpaxos.protocol._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import concurrent.Promise
import scala.util.{Success, Failure}

trait Learner extends ActorLogging {
  this: LearnerActor =>
  
  override def preStart(): Unit = {
    log.info("Learner ID: {} UP on {}\n", self.hashCode, self.path)
  }

  def learn(msg: Message, state: Future[LearnerMeta], config: ClusterConfiguration): Future[LearnerMeta] = {
    val newState = Promise[LearnerMeta]()
    val actorSender = sender
    state onComplete {
      case Success(s) =>
                // TODO: verify learner round!?
                msg match {
                    case m: Msg2A =>
                      if (m.value.get(actorSender) == Nil && m.rnd.cfproposers(actorSender)){
                        newState.success(s.copy(P = s.P + actorSender))
                        println(s"FAST for ${actorSender.hashCode}\n")
                      } else newState.success(s)
                    case m: Msg2B =>
                      println(s"LEARNER QUORUM: ${s.quorum}\n")
                      if (s.quorum.size >= config.quorumSize) {
                        var msgs = s.quorum.values.asInstanceOf[Iterable[Msg2B]]
                        //.toSet ++ Set(m)
                        println(s"MSGS 2B RECEIVED: ${msgs}")
                        val Q2bVals = msgs.map(a => a.value).toSet.flatMap( (e: Option[VMap[Values]]) => e)
                        //Q2bVals += m.value.get
                        println(s"Q2bVals: ${Q2bVals}\n P ====> ${s.P}\n")
                        var value = VMap[Values]()
                        for (p <- s.P) value += (p -> Nil)
                        println(s"GLB: ${VMap.glb(Q2bVals)} VALUE: ${value}\n")
                        val w: Option[VMap[Values]] = Some(VMap.glb(Q2bVals) ++ value)
                        // TODO: Speculative execution
                        val Slub: Set[VMap[Values]] = Set(s.learned.get, w.get)
                        val lubVals = VMap.lub(Slub)
                        log.info("LEARNER {} --- LEARNED: {}\n", self, lubVals)
                        newState.success(s.copy(learned = Some(lubVals)))
                        instancesLearned.insert(msg.instance)
                      } 
                      else newState.success(s)
                    case _ => 
                      println("Unknown message\n")
                      newState.success(s)
                }
      case Failure(ex) => println(s"Learn Promise fail, not update State. Because of a ${ex.getMessage}\n")
    }
    newState.future
  }

  def learnerBehavior(config: ClusterConfiguration, instances: Map[Int, Future[LearnerMeta]]): Receive = {
    case msg: Msg2A =>
      log.info("Received MSG2A from {}\n", sender.hashCode)
      val state = instances(msg.instance)
      context.become(learnerBehavior(config, instances + (msg.instance -> learn(msg, state, config))))

    case msg: Msg2B =>
      log.info("Received MSG2B from {}\n", sender.hashCode)
      val actorSender = sender
      val state: Future[LearnerMeta] = instances(msg.instance)
      state onSuccess {
          case s =>
            println(s"SENDER of 2B: ${actorSender}\n")
            context.become(learnerBehavior(config, instances + (msg.instance -> learn(msg, Future.successful(s.copy(quorum =  s.quorum + (actorSender -> msg))), config))))
      }
    
    case msg: WhatULearn =>
      // TODO: Send interval of learned instances
      instancesLearned pipeTo sender

    case msg: UpdateConfig =>
      context.become(learnerBehavior(msg.config, instances))
    // TODO MemberRemoved
  }
}

class LearnerActor extends Actor with Learner {
  
  var instancesLearned: IRange = IRange()

  def receive = learnerBehavior(ClusterConfiguration(), Map(0 -> Future.successful(LearnerMeta(Some(VMap[Values]()), Map(), Set()))))
}
