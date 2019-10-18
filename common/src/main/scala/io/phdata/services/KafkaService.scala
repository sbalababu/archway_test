package io.phdata.services

import cats.data._
import cats.effect.Effect
import cats.implicits._
import io.phdata.AppContext
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import io.phdata.generators.TopicGenerator
import io.phdata.models.{DistinguishedName, TopicRequest}

trait KafkaService[F[_]] {

  def create(username: DistinguishedName, workspaceId: Long, kafkaTopic: TopicRequest): F[NonEmptyList[String]]

}

class KafkaServiceImpl[F[_]: Effect](
    appContext: AppContext[F],
    provisioningService: ProvisioningService[F],
    topicGenerator: TopicGenerator[F]
) extends KafkaService[F] with LazyLogging {

  override def create(
      username: DistinguishedName,
      workspaceId: Long,
      topicRequest: TopicRequest
  ): F[NonEmptyList[String]] = {
    val MIN_REPLICATION_FACTOR = 3
    val replicationFactor = Math.max(topicRequest.replicationFactor, MIN_REPLICATION_FACTOR)
    (for {
      workspace <- OptionT(
        appContext.workspaceRequestRepository.find(workspaceId).value.transact(appContext.transactor)
      )

      kafkaTopic <- OptionT.liftF(
        topicGenerator.topicFor(topicRequest.name, topicRequest.partitions, replicationFactor, workspace)
      )

      result <- OptionT.liftF {
        (for {
          managerLDAP <- appContext.ldapRepository.create(kafkaTopic.managingRole.ldapRegistration)
          readonlyLDAP <- appContext.ldapRepository.create(kafkaTopic.readonlyRole.ldapRegistration)

          beforeRoles = kafkaTopic.copy(
            managingRole = kafkaTopic.managingRole.copy(ldapRegistration = managerLDAP),
            readonlyRole = kafkaTopic.readonlyRole.copy(ldapRegistration = readonlyLDAP)
          )
          managerRoleId <- appContext.topicGrantRepository.create(beforeRoles.managingRole)
          readonlyRoleId <- appContext.topicGrantRepository.create(beforeRoles.readonlyRole)

          afterRoles = beforeRoles.copy(
            managingRole = beforeRoles.managingRole.copy(id = Some(managerRoleId)),
            readonlyRole = beforeRoles.readonlyRole.copy(id = Some(readonlyRoleId))
          )

          id <- appContext.kafkaRepository.create(afterRoles)

          _ <- appContext.memberRepository.create(username, managerLDAP.id.get)

          _ <- appContext.workspaceRequestRepository.linkTopic(workspaceId, id)
        } yield afterRoles.copy(id = Some(id))).transact(appContext.transactor)
      }

      _ <- OptionT.liftF(provisioningService.provisionTopic(workspaceId, result))

    } yield NonEmptyList.one("")).value.map(_.get)
  }
}
