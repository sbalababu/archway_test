package io.phdata

import java.net.URLEncoder
import java.util.Properties

import cats.effect.{Async, ContextShift, Resource}
import com.typesafe.scalalogging.StrictLogging
import doobie._
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Strategy
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, _}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

package object config extends StrictLogging {

  object AvailableFeatures {
    val messaging = "messaging"
    val applications = "applications"

    val all = Seq(messaging, applications).mkString(",")
  }

  implicit final val finiteDurationDecoder: Decoder[Duration] =
    (c: HCursor) => {
      for {
        duration <- c.as[String]
      } yield Duration(duration)
    }

  case class Password(value: String) {
    override def toString: String = "***********"
  }

  object Password {

    implicit val passwordDecoder: Decoder[Password] =
      (c: HCursor) => {
        for {
          value <- c.as[String]
        } yield Password(value)
      }

    implicit val passwordEncoder: Encoder[Password] = new Encoder[Password] {
      override def apply(a: Password): Json = Json.obj(
        ("value", a.value.asJson)
      )
    }
  }

  case class CredentialsConfig(username: String, password: Password)

  case class ServiceOverride(host: Option[String], port: Int)

  case class ClusterConfig(
      sessionRefresh: FiniteDuration,
      url: String,
      name: String,
      nameservice: String,
      environment: String,
      beeswaxPort: Int,
      hiveServer2Port: Int,
      admin: CredentialsConfig,
      hueOverride: ServiceOverride
  ) {
    private val encodedName: String = URLEncoder.encode(name, "utf-8").replaceAll("\\+", "%20")

    val clusterUrl: String = s"$url/api/v18/clusters/$encodedName"

    def hostListUrl = s"$url/api/v18/hosts"

    def hostUrl(hostId: String) = s"$url/hosts/$hostId"

    val serviceListUrl = s"$clusterUrl/services"

    def serviceUrl(service: String) = s"$serviceListUrl/$service"

    def serviceConfigUrl(service: String) = s"${serviceUrl(service)}/config"

    def yarnApplications(service: String) = s"${serviceUrl(service)}/yarnApplications"

    def serviceRoleListUrl(service: String) = s"${serviceUrl(service)}/roles"

    def serviceRoleUrl(service: String, roleId: String) = s"${serviceRoleListUrl(service)}/$roleId"

    val mgmtServiceUrl = s"$url/api/v18/cm/service"

    val mgmtRoleListUrl = s"$mgmtServiceUrl/roles"

    def mgmtRoleConfigGroups(roleConfigGroupName: String) =
      s"$mgmtServiceUrl/roleConfigGroups/$roleConfigGroupName/config?view=full"

    def yarnRoleConfig(serviceName: String, role: String) =
      s"${serviceUrl(serviceName)}/roles/$role/config?view=full"

    val refreshUrl = s"$clusterUrl/commands/poolsRefresh"
  }

  object ClusterConfig {

    import io.circe.generic.semiauto._

    implicit val credentialsConfigDecoder: Decoder[CredentialsConfig] = deriveDecoder
    implicit val serviceOverrideDecoder: Decoder[ServiceOverride] = deriveDecoder

    implicit val decoder: Decoder[ClusterConfig] = Decoder.instance { cursor =>
      for {
        sessionRefresh <- cursor
          .downField("sessionRefresh")
          .as[String]
          .map(Duration.apply(_).asInstanceOf[FiniteDuration])
        url <- cursor.downField("url").as[String]
        name <- cursor.downField("name").as[String]
        nameservice <- cursor.downField("nameservice").as[String]
        environment <- cursor.downField("environment").as[String]
        beeswaxPort <- cursor.downField("beeswaxPort").as[Int]
        hiveServer2Port <- cursor.downField("hiveServer2Port").as[Int]
        admin <- cursor.downField("admin").as[CredentialsConfig]
        hueOverride <- cursor.downField("hueOverride").as[ServiceOverride]
      } yield ClusterConfig(
        sessionRefresh,
        url,
        name,
        nameservice,
        environment,
        beeswaxPort,
        hiveServer2Port,
        admin,
        hueOverride
      )
    }

    implicit val credentialsConfigEncoder: Encoder[CredentialsConfig] = deriveEncoder
    implicit val serviceOverrideEncoder: Encoder[ServiceOverride] = deriveEncoder

    implicit val encoder: Encoder[ClusterConfig] = new Encoder[ClusterConfig] {
      override def apply(a: ClusterConfig): Json = Json.obj(
        ("sessionRefresh", a.sessionRefresh.toString().asJson),
        ("url", a.url.asJson),
        ("name", a.name.asJson),
        ("environment", a.environment.asJson),
        ("beeswaxPort", a.beeswaxPort.asJson),
        ("hiveServer2Port", a.hiveServer2Port.asJson),
        ("admin", a.admin.asJson),
        ("hueOverride", a.hueOverride.asJson)
      )
    }

  }

  case class RestConfig(
      port: Int,
      secret: Password,
      authType: String,
      principal: String,
      httpPrincipal: String,
      keytab: String,
      sslStore: Option[String] = None,
      sslStorePassword: Option[Password] = None,
      sslKeyManagerPassword: Option[Password] = None
  )

  case class UIConfig(url: String, staticContentDir: String)

  object UIConfig {

    private def handleDefault(confParam: String) =
      if (confParam.isEmpty) {
        sys.env("ARCHWAY_UI_HOME")
      } else confParam

    implicit val decoder: Decoder[UIConfig] = Decoder.instance { cursor =>
      for {
        url <- cursor.downField("url").as[String]
        staticContentDir <- cursor.downField("staticContentDir").as[String]
      } yield UIConfig(url, handleDefault(staticContentDir))
    }
  }

  case class SMTPConfig(
      fromEmail: String,
      host: String,
      port: Int,
      auth: Boolean,
      user: Option[String],
      pass: Option[Password],
      ssl: Boolean,
      smtps: Boolean
  ) {
    if (auth) {
      assert(
        user.isDefined && pass.isDefined,
        "Smtp authorization is enabled but username or password are not provided"
      )
    }
  }

  case class ApprovalConfig(notificationEmail: Seq[String], infrastructure: Option[String], risk: Option[String]) {
    val required: Int = List(infrastructure, risk).flatten.length
  }

  object ApprovalConfig {
    private def separateAddresses(input: String): Seq[String] = input.split(",").map(_.trim)

    implicit val decoder: Decoder[ApprovalConfig] = Decoder.instance { cursor =>
      for {
        emailAddresses <- cursor.downField("notificationEmail").as[String]
        infrastructure <- cursor.downField("infrastructure").as[Option[String]]
        risk <- cursor.downField("risk").as[Option[String]]
      } yield ApprovalConfig(separateAddresses(emailAddresses), infrastructure, risk)
    }
  }

  case class WorkspaceConfigItem(
      defaultSize: Int,
      defaultCores: Int,
      defaultMemory: Int,
      poolParents: String
  )

  case class WorkspaceConfig(
      user: WorkspaceConfigItem,
      sharedWorkspace: WorkspaceConfigItem,
      dataset: WorkspaceConfigItem
  )

  case class LDAPBinding(server: String, port: Int, bindDN: String, bindPassword: Password) {
    assert(bindPassword.value.nonEmpty && bindDN.nonEmpty, s"LDAPBinding bindPassword and bindDN need to be set")
  }

  case class LDAPConfig(
      lookupBinding: LDAPBinding,
      provisioningBinding: LDAPBinding,
      filterTemplate: String,
      memberDisplayTemplate: String,
      baseDN: String,
      groupPath: String,
      userPath: Option[String],
      realm: String,
      syncInterval: FiniteDuration,
      authorizationDN: String
  )

  object LDAPConfig {
    import AppConfig._

    implicit val decoder: Decoder[LDAPConfig] = Decoder.instance { cursor =>
      for {
        lookupBinding <- cursor.downField("lookupBinding").as[LDAPBinding]
        provisioningBinding <- cursor.downField("provisioningBinding").as[LDAPBinding]
        filterTemplate <- cursor.downField("filterTemplate").as[String]
        memberDisplayTemplate <- cursor.downField("memberDisplayTemplate").as[String]
        baseDN <- cursor.downField("baseDN").as[String]
        groupPath <- cursor.downField("groupPath").as[String]
        userPath <- cursor.downField("userPath").as[Option[String]]
        realm <- cursor.downField("realm").as[String]
        syncInterval <- cursor
          .downField("syncInterval")
          .as[String]
          .map(d => Duration.apply(d).asInstanceOf[FiniteDuration])
        authorization <- cursor.downField("authorizationDN").as[String]
      } yield LDAPConfig(
        lookupBinding,
        provisioningBinding,
        filterTemplate,
        memberDisplayTemplate,
        baseDN,
        groupPath,
        userPath,
        realm,
        syncInterval,
        authorization
      )
    }

    implicit val encoder: Encoder[LDAPConfig] = new Encoder[LDAPConfig] {
      override def apply(a: LDAPConfig): Json = Json.obj(
        ("lookupBinding", a.lookupBinding.asJson),
        ("provisioningBinding", a.provisioningBinding.asJson),
        ("filterTemplate", a.filterTemplate.asJson),
        ("memberDisplayTemplate", a.memberDisplayTemplate.asJson),
        ("baseDN", a.baseDN.asJson),
        ("groupPath", a.groupPath.asJson),
        ("userPath", a.userPath.asJson),
        ("realm", a.realm.asJson),
        ("syncInterval", Json.fromString(a.syncInterval.toString)),
        ("authorizationDN", a.authorizationDN.asJson)
      )
    }
  }

  case class DatabaseConfigItem(driver: String, url: String, username: Option[String], password: Option[Password]) {

    def hiveTx[F[_]: Async: ContextShift]: Transactor[F] = {
      Class.forName(driver)

      // Turn the transactor into no
      val initialHiveTransactor = Transactor.fromDriverManager[F](driver, url)
      val strategy = Strategy.void.copy(always = FC.close)

      Transactor.strategy.set(initialHiveTransactor, strategy)
    }

    def impalaTx[F[_]: Async: ContextShift]: Transactor[F] = {
      try {
        Class.forName(driver)
      } catch {
        case e: Exception =>
          logger.warn(
            """Impala driver not found. Impala driver must be added to the classpath to use the automatic
              |Impala invalidate metadata functionality for new databases and Sentry permissions.""".stripMargin,
            e
          )
      }

      val properties = new Properties()
      properties.setProperty("SYNC_DDL", "true")

      // Turn the transactor into no
      val initialImpalaTransactor = Transactor.fromDriverManager[F](driver, url, properties)

      val strategy = Strategy.void.copy(always = FC.close)

      Transactor.strategy.set(initialImpalaTransactor, strategy)

    }

    def tx[F[_]: Async: ContextShift](
        connectionEC: ExecutionContext,
        transactionEC: ExecutionContext
    ): Resource[F, HikariTransactor[F]] =
      HikariTransactor.newHikariTransactor[F](
        driver,
        url,
        username.getOrElse(""),
        password.getOrElse(Password("")).value,
        connectionEC,
        transactionEC
      )

  }

  implicit val databaseConfigItemDecoder: Decoder[DatabaseConfigItem] = deriveDecoder

  case class ProvisioningConfig(threadPoolSize: Int, provisionInterval: FiniteDuration)

  object ProvisioningConfig {

    implicit val decoder: Decoder[ProvisioningConfig] = Decoder.instance { cursor =>
      for {
        provisionInterval <- cursor
          .downField("provisionInterval")
          .as[String]
          .map(d => Duration.apply(d).asInstanceOf[FiniteDuration])
        threadpoolSize <- cursor.downField("threadPoolSize").as[Int]
      } yield ProvisioningConfig(threadpoolSize, provisionInterval)
    }

    implicit val encoder: Encoder[ProvisioningConfig] = new Encoder[ProvisioningConfig] {
      override final def apply(a: ProvisioningConfig): Json = Json.obj(
        ("threadPoolSize", a.threadPoolSize.asJson),
        ("provisionInterval", Json.fromString(a.provisionInterval.toString))
      )
    }
  }

  case class DatabaseConfig(meta: DatabaseConfigItem, hive: DatabaseConfigItem, impala: Option[DatabaseConfigItem])

  object DatabaseConfig {

    implicit val decoder: Decoder[DatabaseConfig] = Decoder.instance { cursor =>
      for {
        meta <- cursor.downField("meta").as[DatabaseConfigItem]
        hive <- cursor.downField("hive").as[DatabaseConfigItem]
        impala <- cursor.downField("impala").as[DatabaseConfigItem]
      } yield DatabaseConfig(meta, hive, if (impala.url.isEmpty) None else Some(impala))
    }
  }

  case class KafkaConfig(zookeeperConnect: String, secureTopics: Boolean)

  case class TemplateConfig(
      templateRoot: String,
      topicGenerator: String,
      applicationGenerator: String,
      ldapGroupGenerator: String
  )

  case class AppConfig(
      templates: TemplateConfig,
      rest: RestConfig,
      ui: UIConfig,
      smtp: SMTPConfig,
      cluster: ClusterConfig,
      approvers: ApprovalConfig,
      ldap: LDAPConfig,
      db: DatabaseConfig,
      workspaces: WorkspaceConfig,
      kafka: KafkaConfig,
      provisioning: ProvisioningConfig,
      featureFlags: String
  )

  object AppConfig {

    import io.circe.generic.semiauto._

    implicit val restConfigDecoder: Decoder[RestConfig] = deriveDecoder
    implicit val sMTPConfigDecoder: Decoder[SMTPConfig] = deriveDecoder
    implicit val workspaceConfigItemDecoder: Decoder[WorkspaceConfigItem] = deriveDecoder
    implicit val workspaceConfigDecoder: Decoder[WorkspaceConfig] = deriveDecoder
    implicit val ldapBindingDecoder: Decoder[LDAPBinding] = deriveDecoder
    implicit val lDAPConfigDecoder: Decoder[LDAPConfig] = LDAPConfig.decoder
    implicit val databaseConfigItemDecoder: Decoder[DatabaseConfigItem] = deriveDecoder
    implicit val kafkaConfigDecoder: Decoder[KafkaConfig] = deriveDecoder
    implicit val generatorConfigDecoder: Decoder[TemplateConfig] = deriveDecoder
    implicit val provisioningConfigDecoder: Decoder[ProvisioningConfig] = ProvisioningConfig.decoder
    implicit val appConfigDecoder: Decoder[AppConfig] = deriveDecoder

    implicit val restConfigEncoder: Encoder[RestConfig] = deriveEncoder
    implicit val uIConfigEncoder: Encoder[UIConfig] = deriveEncoder
    implicit val sMTPConfigEncoder: Encoder[SMTPConfig] = deriveEncoder
    implicit val approvalConfigEncoder: Encoder[ApprovalConfig] = deriveEncoder
    implicit val workspaceConfigItemEncoder: Encoder[WorkspaceConfigItem] = deriveEncoder
    implicit val workspaceConfigEncoder: Encoder[WorkspaceConfig] = deriveEncoder
    implicit val ldapBindingEncoder: Encoder[LDAPBinding] = deriveEncoder
    implicit val lDAPConfigEncoder: Encoder[LDAPConfig] = LDAPConfig.encoder
    implicit val databaseConfigItemEncoder: Encoder[DatabaseConfigItem] = deriveEncoder
    implicit val databaseConfigEncoder: Encoder[DatabaseConfig] = deriveEncoder
    implicit val kafkaConfigEncoder: Encoder[KafkaConfig] = deriveEncoder
    implicit val generatorConfigEncoder: Encoder[TemplateConfig] = deriveEncoder
    implicit val provisioningConfigEncoder: Encoder[ProvisioningConfig] = ProvisioningConfig.encoder
    implicit val appConfigEncoder: Encoder[AppConfig] = deriveEncoder
  }

}
