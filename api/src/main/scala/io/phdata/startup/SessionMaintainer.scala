package io.phdata.startup

import cats.effect._
import cats.implicits._
import io.phdata.config.ClusterConfig
import com.typesafe.scalalogging.LazyLogging
import io.phdata.AppContext

class SessionMaintainer[F[_]: Sync: Timer: ContextShift](context: AppContext[F])
    extends ScheduledJob[F] with LazyLogging {

  val clusterConfig: ClusterConfig = context.appConfig.cluster

  override def work: F[Unit] =
    for {
      _ <- logger.info("refreshing kerberos ticket").pure[F]
      _ <- context.loginContextProvider.kinit[F]()
      _ <- logger.info("session refresher going to sleep for {}", clusterConfig.sessionRefresh).pure[F]
      _ <- Timer[F].sleep(clusterConfig.sessionRefresh)
      _ <- work
    } yield ()

}
