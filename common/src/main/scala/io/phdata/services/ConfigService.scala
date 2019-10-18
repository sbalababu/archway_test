package io.phdata.services

trait ConfigService[F[_]] {

  def getAndSetNextGid: F[Long]

  def verifyDbConnection(): F[Unit]

}
