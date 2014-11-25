package cfpaxos

import akka.actor._
import agents._
import messages._
import protocol._

trait SharedBehavior extends Actor with LoggingFSM[State, Metadata]{
  val sharedBehavior: StateFunction = {
    case Event(added: MemberAdded, m: Meta) =>
      // FIXME: use val instead of var
      var p = m.config.proposers
      var a = m.config.acceptors
      var l = m.config.learners
      added.member.roles collect {
        case "proposer" => p += added.ref
        case "acceptor" => a += added.ref
        case "learner"  => l += added.ref
      }
      //FIXME Get coordinator, cfproposers and quorum from configuration
      val initialConfig = ClusterConfiguration(p, p, p, a, l, a)
      if(added.until <= initialConfig.acceptors.size) {
        log.info("Discovered the minimum of {} acceptors, starting protocol instance.", added.until)
        goto(Running) using m.copy(config = initialConfig)
      } else {
        log.info("Up to {} acceptors, still waiting in Init until {} acceptors discovered.", initialConfig.acceptors.size, added.until)
        stay() using m.copy(config = initialConfig)
    }
    //TODO MemberRemoved
  } 
}
