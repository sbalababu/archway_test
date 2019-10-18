package io.phdata.services

import io.phdata.models.{Application, DistinguishedName}

trait ApplicationService[F[_]] {

  def create(username: DistinguishedName, workspaceId: Long, applicationRrequest: ApplicationRequest): F[Application]

}
