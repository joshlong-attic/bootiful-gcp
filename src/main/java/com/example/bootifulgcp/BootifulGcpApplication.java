package com.example.bootifulgcp;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootApplication
public class BootifulGcpApplication {
		@Bean
		ImageAnnotatorClient visionClient(CredentialsProvider cp) throws IOException {
				return ImageAnnotatorClient.create(ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(cp)
					.build());
		}

		@Bean
		RestTemplate restTemplate() {
				return new RestTemplate();
		}

		public static void main(String[] args) {
				SpringApplication.run(BootifulGcpApplication.class, args);
		}
}

@RestController
class VisionController {
		private final ImageAnnotatorClient imageAnnotatorClient;

		VisionController(ImageAnnotatorClient imageAnnotatorClient) {
				this.imageAnnotatorClient = imageAnnotatorClient;
		}

		@PostMapping("/analyze")
		public String analyze (@RequestParam MultipartFile image) throws IOException {
				BatchAnnotateImagesResponse batchAnnotateImagesResponse = imageAnnotatorClient.batchAnnotateImages(
					Collections.singletonList(AnnotateImageRequest.newBuilder()
						.addFeatures(Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build())
						.addFeatures(Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build())
						.setImage(Image.newBuilder()
							.setContent(ByteString.copyFrom(image.getBytes()))
							.build())
						.build())
				);

				return batchAnnotateImagesResponse.toString();
		}
}

/*
@Log4j2
@Component
@Profile("jdbc")
class JdbcReservationRunner implements ApplicationRunner {

		private final JdbcTemplate jdbcTemplate;

		JdbcReservationRunner(JdbcTemplate jdbcTemplate) {
				this.jdbcTemplate = jdbcTemplate;
		}

		@Override
		public void run(ApplicationArguments args) throws Exception {
				List<Reservation> query = this.jdbcTemplate.query("select * from reservations",
					(rs, rowNum) -> new Reservation(rs.getLong("id"), rs.getString("reservation_name")));
				query.forEach(log::info);
		}
}
*/


@RestController
class ProducerRestController {

		private final PubSubTemplate template;

		ProducerRestController(PubSubTemplate template) {
				this.template = template;
		}

		@PostMapping("/greet/{name}")
		ListenableFuture<String> greet(@PathVariable String name) {
				return this.template.publish("messages", "greetings " + name + "!");
		}
}


@Log4j2
@Component
class ConsumerRunner implements ApplicationRunner {

		private final PubSubTemplate template;

		ConsumerRunner(PubSubTemplate template) {
				this.template = template;
		}

		@Override
		public void run(ApplicationArguments args) {
				this.template.subscribe("messages-subscription", (message, consumer) -> {
						log.info("received message '" + message.getData().toStringUtf8() + "'");
						consumer.ack();
				});
		}
}

@Component
class SpannerReservationRunner implements ApplicationRunner {

		private final ReservationRepository repo;

		SpannerReservationRunner(ReservationRepository repo) {
				this.repo = repo;
		}

		@Override
		public void run(ApplicationArguments args) throws Exception {
				repo.deleteAll();
				Stream.of("ray", "josh")
					.map(name -> new Reservation(UUID.randomUUID().toString(), name))
					.map(repo::save)
					.forEach(System.out::println);
		}
}


@Log4j2
@Component
class TraceDemoRunner implements ApplicationRunner {

		private final RestTemplate restTemplate;

		TraceDemoRunner(RestTemplate restTemplate) {
				this.restTemplate = restTemplate;
		}

		@Override
		@NewSpan("runner-client")
		public void run(ApplicationArguments args) throws Exception {

				IntStream
					.range(0, 10)
					.mapToObj(i -> this.restTemplate
						.exchange("http://localhost:8080/reservations", HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
						})
						.getBody()
					)
					.forEach(log::info);
		}
}

@RepositoryRestResource
interface ReservationRepository extends PagingAndSortingRepository<Reservation, String> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table
class Reservation {
		@Id
		@PrimaryKey
		private String id;

		@Column(name = "reservation_name")
		private String reservationName;
}