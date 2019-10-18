package io.phdata.rest

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.applicative._
import io.phdata.rest.authentication.{LdapAuthService, TokenAuthServiceImpl}
import io.phdata.test.fixtures._
import io.phdata.rest.authentication.TokenAuthServiceImpl
import io.phdata.services.AccountService
import org.http4s._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class TokenAuthServiceImplSpec extends FlatSpec with Matchers with MockFactory {

  behavior of "Auth Service"

  it should "allow a user to perform approvals" in {
    val accountService = mock[AccountService[IO]]
    accountService.validate _ expects infraApproverToken returning EitherT.right(infraApproverUser.pure[IO])

    val authService = new TokenAuthServiceImpl[IO](accountService)
    val Some(result) = authService.validate(u => u.permissions.platformOperations || u.permissions.riskManagement)(
      Request(uri = Uri.uri("/profile"), headers = Headers.of(Header("Authorization", infraApproverToken)))).value.unsafeRunSync()
    result.permissions.platformOperations shouldBe true
  }

  it should "not allow a user to perform approvals" in {
    val accountService = mock[AccountService[IO]]
    accountService.validate _ expects basicUserToken returning EitherT.right(basicUser.pure[IO])

    val authService = new TokenAuthServiceImpl[IO](accountService)
    val result = authService.validate(u => u.permissions.platformOperations || u.permissions.riskManagement)(
      Request(uri = Uri.uri("/profile"), headers = Headers.of(Header("Authorization", basicUserToken)))).value.unsafeRunSync()
    result should not be defined
  }

}
