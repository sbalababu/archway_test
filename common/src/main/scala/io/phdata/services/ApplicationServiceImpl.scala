package io.phdata.services

import cats.effect._
import cats.implicits._
import io.phdata.AppContext
import io.phdata.models.{Application, DistinguishedName}
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import io.phdata.generators.ApplicationGenerator

class ApplicationServiceImpl[F[_]](
    appContext: AppContext[F],
    provisioningService: ProvisioningService[F],
    applicationGenerator: ApplicationGenerator[F]
)(implicit F: Effect[F])
    extends ApplicationService[F] with LazyLogging {

  override def create(
      username: DistinguishedName,
      workspaceId: Long,
      applicationRrequest: ApplicationRequest
  ): F[Application] =
    for {
      workspace <- appContext.workspaceRequestRepository.find(workspaceId).value.transact(appContext.transactor)

      _ <- F.pure(logger.warn("found {}", workspace))

      app <- applicationGenerator.applicationFor(applicationRrequest, workspace.get)

      saved <- (for {
        ldap <- appContext.ldapRepository.create(app.group)
        beforeSave = app.copy(group = ldap)
        appId <- appContext.applicationRepository.create(beforeSave)
        _ <- appContext.memberRepository.create(username, ldap.id.get)
        _ <- appContext.workspaceRequestRepository.linkApplication(workspaceId, appId)
      } yield beforeSave.copy(id = Some(appId))).transact(appContext.transactor)

      _ <- provisioningService.provisionApplication(workspaceId, saved)
    } yield saved

}
