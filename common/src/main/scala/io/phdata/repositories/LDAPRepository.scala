package io.phdata.repositories

import java.time.Instant

import cats.data.OptionT
import io.phdata.models.LDAPRegistration
import doobie.free.connection.ConnectionIO

trait LDAPRepository {

  def create(lDAPRegistration: LDAPRegistration): ConnectionIO[LDAPRegistration]

  def delete(ldapRegistration: LDAPRegistration): ConnectionIO[Unit]

  def findAll(resource: String, resourceId: Long): ConnectionIO[List[LDAPRegistration]]

  def find(resource: String, resourceId: Long, role: String): OptionT[ConnectionIO, LDAPRegistration]

  def groupCreated(id: Long, time: Instant): ConnectionIO[Int]

  def roleCreated(id: Long, time: Instant): ConnectionIO[Int]

  def groupAssociated(id: Long, time: Instant): ConnectionIO[Int]

}
