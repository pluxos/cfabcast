package cfabcast.serialization

import akka.serialization.Serializer
import akka.actor.ExtendedActorSystem
import akka.serialization.Serialization
import akka.serialization.SerializationExtension

class CFABCastSerializer(val system: ExtendedActorSystem) extends Serializer {

  override def includeManifest: Boolean = true

  //FIXME: Randomly generate identifier
  override def identifier = 32974

  lazy val javaSerializer = SerializationExtension(system).findSerializerFor(classOf[java.io.Serializable])

  def toBinary(obj: AnyRef): Array[Byte] = {
    javaSerializer.toBinary(obj)
  }

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    javaSerializer.fromBinary(bytes, clazz)
  }

}
