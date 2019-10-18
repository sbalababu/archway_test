package io.phdata.provisioning

import java.time.Instant

import cats._
import cats.effect._
import cats.implicits._
import doobie.implicits._
import io.phdata.models.DistinguishedName

object GroupMemberProvisioning {

  implicit val show: Show[GroupMember] =
    Show.show(am => s""""${am.distinguishedName}" as a member of "${am.groupDN}""")

  implicit object NewMemberProvisioningTask extends ProvisioningTask[GroupMember] {

    override def complete[F[_]: Sync](
        groupMember: GroupMember,
        instant: Instant,
        workspaceContext: WorkspaceContext[F]
    ): F[Unit] =
      workspaceContext.context.memberRepository
        .complete(groupMember.ldapRegistrationId, groupMember.distinguishedName)
        .transact(workspaceContext.context.transactor)
        .void

    override def run[F[_]: Sync: Clock](groupMember: GroupMember, workspaceContext: WorkspaceContext[F]): F[Unit] =
      for {
        _ <- workspaceContext.context.provisioningLDAPClient
          .addUser(groupMember.groupDN, groupMember.distinguishedName)
          .value
          .void
      } yield ()
  }

  implicit object NewMemberDeprovisioningTask extends DeprovisioningTask[GroupMember] {

    override def run[F[_]: Sync: Clock](groupMember: GroupMember, workspaceContext: WorkspaceContext[F]): F[Unit] =
      workspaceContext.context.provisioningLDAPClient
        .removeUser(groupMember.groupDN, groupMember.distinguishedName)
        .value
        .void

  }

  implicit val provisionable: Provisionable[GroupMember] = Provisionable.deriveFromTasks

}
