package io.phdata.test.fixtures

import cats.effect._
import cats.implicits._
import io.phdata.services.ConfigService

class TestConfigService extends ConfigService[IO] {

  override def getAndSetNextGid: IO[Long] =
    123L.pure[IO]

  override def verifyDbConnection(): IO[Unit] = ().pure[IO]
}
