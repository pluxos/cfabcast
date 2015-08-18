package cfabcast.messages

import scala.beans.BeanProperty

import cfabcast._

/*
 * Client Messages
 */

sealed class CFABCastMessage extends Serializable 

case class Broadcast(@BeanProperty var data: Array[Byte]) extends CFABCastMessage

case class Delivery(@BeanProperty var data: Array[Byte])  extends CFABCastMessage