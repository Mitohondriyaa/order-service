package io.github.mitohondriyaa.order;

import io.github.mitohondriyaa.order.stub.InventoryClientStub;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class OrderServiceApplicationTests {
	static Network network = Network.newNetwork();
	@ServiceConnection
	@SuppressWarnings("resource")
	static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8")
		.withNetwork(network)
		.withNetworkAliases("mysql");
	@ServiceConnection
	static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
		.withListener("kafka:19092")
		.withNetwork(network)
		.withNetworkAliases("kafka");
	@SuppressWarnings("resource")
	static GenericContainer<?> schemaRegistryContainer = new GenericContainer<>("confluentinc/cp-schema-registry:7.4.0")
		.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
		.withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
		.withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
		.withExposedPorts(8081)
		.withNetwork(network)
		.withNetworkAliases("schema-registry")
		.waitingFor(Wait.forHttp("/subjects"));
	@LocalServerPort
	Integer port;

	static {
		mySQLContainer.start();
		kafkaContainer.start();
		schemaRegistryContainer.start();
	}

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.producer.properties.schema.registry.url",
			() -> "http://localhost:" + schemaRegistryContainer.getMappedPort(8081));
	}

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	@Test
	void shouldPlaceOrder() {
		String requestBody = """
			{
				"skuCode": "iPhone_15",
				"price": 799,
				"quantity": 1,
				"userDetails": {
			        "email": "mitohondriyaa@gmail.com",
			        "firstName": "Nikita",
			        "lastName": "Dymko"
			    }
			}
			""";

		InventoryClientStub.stubInventoryCall("iPhone_15", 1);

		RestAssured.given()
				.contentType(ContentType.JSON)
				.body(requestBody)
				.when()
				.post("/api/order")
				.then()
				.statusCode(201)
				.body("id", Matchers.notNullValue())
				.body("orderNumber", Matchers.notNullValue())
				.body("skuCode", Matchers.is("iPhone_15"))
				.body("price", Matchers.is(799))
				.body("quantity", Matchers.is(1));
	}

	@AfterAll
	static void stopContainers() {
		mySQLContainer.stop();
		kafkaContainer.stop();
		schemaRegistryContainer.stop();
	}
}
