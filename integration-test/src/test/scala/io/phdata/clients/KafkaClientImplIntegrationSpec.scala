package io.phdata.clients

import java.util.UUID

import cats.effect.IO
import io.phdata.itest.fixtures._
import kafka.admin.AdminUtils
import org.scalatest.{FlatSpec, Matchers}

class KafkaClientImplIntegrationSpec extends FlatSpec with Matchers with IntegrationTest {

  behavior of "KafkaImplCiTest"

  it should "create Kafka topic" in {
    val client = new KafkaClientImpl[IO](itestConfig) {}
    val KAFKA_TEST_TOPIC = s"heimdali_test_kafka_topic_${UUID.randomUUID().toString.take(8)}"

    client.createTopic(KAFKA_TEST_TOPIC, 10, 1).unsafeRunSync()
    AdminUtils.topicExists(client.zkUtils, KAFKA_TEST_TOPIC) shouldBe true

    AdminUtils.deleteTopic(client.zkUtils, KAFKA_TEST_TOPIC)
    Thread.sleep(500)
    AdminUtils.topicExists(client.zkUtils, KAFKA_TEST_TOPIC) shouldBe false
  }

  it should "delete Kafka topic" in {
    val client = new KafkaClientImpl[IO](itestConfig) {}
    val KAFKA_TEST_TOPIC = s"heimdali_test_kafka_topic_${UUID.randomUUID().toString.take(8)}"

    client.createTopic(KAFKA_TEST_TOPIC, 10, 1).unsafeRunSync()
    AdminUtils.topicExists(client.zkUtils, KAFKA_TEST_TOPIC) shouldBe true

    client.deleteTopic(KAFKA_TEST_TOPIC).unsafeRunSync()
    Thread.sleep(500)
    AdminUtils.topicExists(client.zkUtils, KAFKA_TEST_TOPIC) shouldBe false
  }
}
