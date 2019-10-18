package io.phdata.clients

import cats.effect.{Effect, IO, Sync}
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging
import doobie._
import doobie.implicits._
import org.apache.sentry.provider.db.generic.service.thrift.{SentryGenericServiceClient, TSentryPrivilege}
import org.apache.sentry.provider.db.generic.tools.KafkaTSentryPrivilegeConverter
import io.phdata.repositories.CustomLogHandler
import io.phdata.models.{DatabaseRole, Manager, ReadOnly, ReadWrite}
import io.phdata.services.LoginContextProvider

sealed trait Component {

  def name: String

  def privilege(grantString: String): TSentryPrivilege

}

case object Hive extends Component {
  val name: String = "hive"

  def privilege(grantString: String): TSentryPrivilege = ???
}

case object Kafka extends Component {
  val name: String = "kafka"

  def privilege(grantString: String): TSentryPrivilege =
    new KafkaTSentryPrivilegeConverter("kafka", "kafka").fromString(grantString)
}

trait SentryClient[F[_]] {
  def createRole(name: String): F[Unit]

  def grantGroup(group: String, role: String): F[Unit]

  def enableAccessToDB(database: String, role: String, databaseRole: DatabaseRole): F[Unit]

  def enableAccessToLocation(location: String, role: String): F[Unit]

  def grantPrivilege(role: String, component: Component, grantString: String): F[Unit]

  def dropRole(role: String): F[Unit]

  def removeAccessToDB(database: String, role: String, databaseRole: DatabaseRole): F[Unit]

  def revokeGroup(group: String, role: String): F[Unit]

  def removeAccessToLocation(location: String, role: String): F[Unit]

  def removePrivilege(role: String, component: Component, grantString: String): F[Unit]
}

class SentryClientImpl[F[_]](
    transactor: Transactor[F],
    client: SentryGenericServiceClient,
    loginContextProvider: LoginContextProvider
)(implicit val F: Effect[F])
    extends SentryClient[F] with LazyLogging {

  implicit val han: LogHandler = CustomLogHandler.logHandler(this.getClass)

  def roles: F[Seq[String]] = sql"""SHOW ROLES""".query[String].to[Seq].transact(transactor)

  override def createRole(name: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      roles.flatMap {
        case roles if !roles.contains(name) =>
          (fr"CREATE ROLE" ++ Fragment.const(name)).update.run.transact(transactor).void
        case _ =>
          Sync[F].unit
      }
    }

  override def grantGroup(group: String, role: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      (fr"GRANT ROLE" ++ Fragment.const(role) ++ fr"TO GROUP" ++ Fragment.const(group)).update.run
        .transact(transactor)
        .flatMap(_ => Sync[F].unit)
    }

  override def enableAccessToDB(database: String, role: String, databaseRole: DatabaseRole): F[Unit] = {
    logger.debug(s"Granting $databaseRole to $database for role $role").pure[F] *>
      loginContextProvider.hadoopInteraction {
        (databaseRole match {
          case Manager =>
            fr"GRANT ALL ON DATABASE" ++ Fragment.const(database) ++ fr"TO ROLE" ++ Fragment.const(role) ++ fr"WITH GRANT OPTION"
          case ReadWrite =>
            fr"GRANT ALL ON DATABASE" ++ Fragment.const(database) ++ fr"TO ROLE" ++ Fragment.const(role)
          case ReadOnly =>
            fr"GRANT SELECT ON DATABASE" ++ Fragment.const(database) ++ fr"TO ROLE" ++ Fragment.const(role)
        }).update.run.transact(transactor).flatMap(_ => Sync[F].unit)
      }
  }

  override def enableAccessToLocation(location: String, role: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      (fr"GRANT ALL ON URI '" ++ Fragment.const(location) ++ fr"' TO ROLE" ++ Fragment.const(role) ++ fr"WITH GRANT OPTION").update.run
        .transact(transactor)
        .flatMap(_ => Sync[F].unit)
    }

  override def grantPrivilege(role: String, component: Component, grantString: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      F.delay(
          client.grantPrivilege("archway", component.name, component.name, component.privilege(grantString))
        )
        .void
    }

  override def dropRole(role: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      roles.flatMap {
        case roles if roles.contains(role) =>
          (fr"DROP ROLE" ++ Fragment.const(role)).update.run.transact(transactor).void
        case _ =>
          Sync[F].unit
      }
    }

  override def removeAccessToDB(database: String, role: String, databaseRole: DatabaseRole): F[Unit] =
    loginContextProvider.hadoopInteraction {
      (databaseRole match {
        case Manager | ReadWrite =>
          fr"REVOKE ALL ON DATABASE" ++ Fragment.const(database) ++ fr"FROM ROLE" ++ Fragment.const(role)
        case ReadOnly =>
          fr"REVOKE SELECT ON DATABASE" ++ Fragment.const(database) ++ fr"FROM ROLE" ++ Fragment.const(role)
      }).update.run.transact(transactor).flatMap(_ => Sync[F].unit)
    }

  override def revokeGroup(group: String, role: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      (fr"REVOKE ROLE" ++ Fragment.const(role) ++ fr"FROM GROUP" ++ Fragment.const(group)).update.run
        .transact(transactor)
        .flatMap(_ => Sync[F].unit)
    }

  override def removeAccessToLocation(location: String, role: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      (fr"REVOKE ALL ON URI '" ++ Fragment.const(location) ++ fr"' FROM ROLE" ++ Fragment.const(role)).update.run
        .transact(transactor)
        .flatMap(_ => Sync[F].unit)
    }

  override def removePrivilege(role: String, component: Component, grantString: String): F[Unit] =
    loginContextProvider.hadoopInteraction {
      F.delay(
          client.dropPrivilege("archway", component.name, component.privilege(grantString))
        )
        .void
    }
}
