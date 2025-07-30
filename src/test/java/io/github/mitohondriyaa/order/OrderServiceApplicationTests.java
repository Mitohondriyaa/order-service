package io.github.mitohondriyaa.order;

import io.github.mitohondriyaa.order.event.OrderPlacedEvent;
import io.github.mitohondriyaa.order.model.Order;
import io.github.mitohondriyaa.order.repository.OrderRepository;
import io.github.mitohondriyaa.order.stub.InventoryClientStub;
import io.github.mitohondriyaa.order.stub.ProductClientStub;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.Lifecycle;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
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
	static final String PRODUCT_ID = "6885edd749327c54f0627f8b";
	@LocalServerPort
	Integer port;
	@MockitoBean
	JwtDecoder jwtDecoder;
	final ConsumerFactory<String, OrderPlacedEvent> consumerFactory;
	final OrderRepository orderRepository;

	static {
		mySQLContainer.start();
		kafkaContainer.start();
		schemaRegistryContainer.start();
	}

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.producer.properties.schema.registry.url",
			() -> "http://localhost:" + schemaRegistryContainer.getMappedPort(8081));
		registry.add("spring.kafka.consumer.properties.schema.registry.url",
			() -> "http://localhost:" + schemaRegistryContainer.getMappedPort(8081));
	}

	@BeforeEach
	void setUp() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;

		Jwt jwt = Jwt.withTokenValue("mock-token")
			.header("alg", "none")
			.claim("email", "test@example.com")
			.claim("given_name", "Alexander")
			.claim("family_name", "Sidorov")
			.claim("sub", "h7g3hg383837h7733hf38h37")
			.build();

		when(jwtDecoder.decode(anyString())).thenReturn(jwt);
	}

	@Test
	void shouldPlaceOrder() {
		String requestBody = """
			{
				"productId": "%s",
				"quantity": 1
			}
			""".formatted(PRODUCT_ID);

		InventoryClientStub.stubInventoryCall(PRODUCT_ID, 1);
		ProductClientStub.stubProductCall(PRODUCT_ID);

		RestAssured.given()
			.contentType(ContentType.JSON)
			.header("Authorization", "Bearer mock-token")
			.body(requestBody)
			.when()
			.post("/api/order")
			.then()
			.statusCode(201)
			.body("id", Matchers.notNullValue())
			.body("orderNumber", Matchers.notNullValue())
			.body("productId", Matchers.is("6885edd749327c54f0627f8b"))
			.body("price", Matchers.is(500))
			.body("quantity", Matchers.is(1))
			.body("userDetails.email", Matchers.is("test@example.com"))
			.body("userDetails.firstName", Matchers.is("Alexander"))
			.body("userDetails.lastName", Matchers.is("Sidorov"));

		try (Consumer<String, OrderPlacedEvent> consumer = consumerFactory.createConsumer()) {
			consumer.subscribe(List.of("order-placed"));

			ConsumerRecords<String, OrderPlacedEvent> records =
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

			Assertions.assertFalse(records.isEmpty());
		}
	}

	@Test
	void shouldDeleteOrderById() {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());
		order.setProductId(PRODUCT_ID);
		order.setPrice(new BigDecimal(799));
		order.setQuantity(1);
		order.setEmail("test@example.com");
		order.setFirstName("Alexander");
		order.setLastName("Sidorov");
		order.setUserId("h7g3hg383837h7733hf38h37");

		Long id = orderRepository.save(order).getId();

		RestAssured.given()
			.header("Authorization", "Bearer mock-token")
			.when()
			.delete("/api/order/my/" + id)
			.then()
			.statusCode(204);

		try (Consumer<String, OrderPlacedEvent> consumer = consumerFactory.createConsumer()) {
			consumer.subscribe(List.of("order-cancelled"));

			ConsumerRecords<String, OrderPlacedEvent> records =
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

			Assertions.assertFalse(records.isEmpty());
		}

		Assertions.assertFalse(orderRepository.existsById(id));
	}

	@Test
	void shouldGetOrdersByUserId() {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());
		order.setProductId(PRODUCT_ID);
		order.setPrice(new BigDecimal(799));
		order.setQuantity(1);
		order.setEmail("test@example.com");
		order.setFirstName("Alexander");
		order.setLastName("Sidorov");
		order.setUserId("h7g3hg383837h7733hf38h37");

		orderRepository.save(order);

		RestAssured.given()
			.header("Authorization", "Bearer mock-token")
			.when()
			.get("/api/order/my")
			.then()
			.statusCode(200)
			.body("size()", Matchers.is(1));
	}

	@Test
	void shouldGetOrderForUser() {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());
		order.setProductId(PRODUCT_ID);
		order.setPrice(new BigDecimal(799));
		order.setQuantity(1);
		order.setEmail("test@example.com");
		order.setFirstName("Alexander");
		order.setLastName("Sidorov");
		order.setUserId("h7g3hg383837h7733hf38h37");

		Long id = orderRepository.save(order).getId();

		RestAssured.given()
			.when()
			.header("Authorization", "Bearer mock-token")
			.get("/api/order/my/" + id)
			.then()
			.statusCode(200)
			.body("id", Matchers.is(id.intValue()))
			.body("orderNumber", Matchers.is(order.getOrderNumber()))
			.body("productId", Matchers.is(order.getProductId()))
			.body("price", Matchers.is(799.0F))
			.body("quantity", Matchers.is(order.getQuantity()))
			.body("userDetails.email", Matchers.is(order.getEmail()))
			.body("userDetails.firstName", Matchers.is(order.getFirstName()))
			.body("userDetails.lastName", Matchers.is(order.getLastName()));
	}

	@AfterEach
	void tearDown() {
		orderRepository.deleteAll();
	}

	@AfterAll
	static void stopContainers() {
		mySQLContainer.stop();
		kafkaContainer.stop();
		schemaRegistryContainer.stop();
		network.close();
	}
}
