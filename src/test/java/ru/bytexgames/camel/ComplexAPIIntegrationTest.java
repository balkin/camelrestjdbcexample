package ru.bytexgames.camel;

import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Complex API
 * <p>Description: </p>
 * Date: 9/22/16 - 3:11 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@bytexgames.ru">baron@bytexgames.ru</a>
 * @version 1.0.0.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ComplexAPIIntegrationTest {

	public static final Pattern CSV_PATTERN = Pattern.compile("\\{ID=(\\d+), FIRSTNAME=(.*?), LASTNAME=(.*)\\}");

	public static final Random RANDOM = new Random();

	public static final Integer THREADS = 10;

	public static final Logger log = LoggerFactory.getLogger(ComplexAPIIntegrationTest.class);

	public static final String FIRST = "First";
	public static final String LAST = "Last";

	public static class UserModel {
		private final Integer id;
		private final String firstName;
		private final String lastName;

		public UserModel(Integer id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public Integer getId() {
			return id;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public static UserModel parse(String s) {
			final Matcher matcher = CSV_PATTERN.matcher(s);
			if (matcher.matches()) {
				return new UserModel(Integer.parseInt(matcher.group(1)), matcher.group(2), matcher.group(3));
			}
			return null;
		}

		@Override
		public String toString() {
			return String.format("UserModel{id=%d, firstName='%s', lastName='%s'}", id, firstName, lastName);
		}
	}

	// HELPER METHODS

	private static Integer registerUser() {
		log.debug("Registering user in {}", Thread.currentThread().getName());
		final Response response = given()
				.formParam("firstName", FIRST)
				.formParam("lastName", LAST)
		.when()
				.post(Constants.ENDPOINT)
		.then()
				.statusCode(200)
				.body(Matchers.startsWith("[{"), Matchers.endsWith("}]"))
				.extract()
				.response();
		final UserModel userModel = extractModel(response);
		log.debug("Registered user {} in {}", userModel, Thread.currentThread().getName());
		return userModel == null ? 0 : userModel.id;
	}

	private static UserModel extractModel(Response response) {
		if (response instanceof RestAssuredResponseImpl) {
			final String content = new String((byte[]) ((RestAssuredResponseImpl) response).getContent());
			return UserModel.parse(content.substring(1, content.length() - 1));
		}
		return null;
	}

	private static Integer deleteUser(Integer id) {
		log.debug("Deleting user {} in {}", id, Thread.currentThread().getName());
		when()
				.delete(Constants.ENDPOINT + "{id}", id)
		.then()
				.statusCode(200);
		return id;
	}

	private static UserModel deleteUser(UserModel userModel) {
		log.debug("Deleting user {} in {}", userModel, Thread.currentThread().getName());
		when()
				.delete(Constants.ENDPOINT + "{id}", userModel.id)
		.then()
				.statusCode(200);
		return userModel;
	}

	private static UserModel updateUser(UserModel userModel) {
		log.debug("Updating user {} in {}", userModel, Thread.currentThread().getName());
		given()
				.formParam("firstName", userModel.getLastName())
				.formParam("lastName", userModel.getFirstName())
		.when()
				.put(Constants.ENDPOINT + "{id}", userModel.id)
		.then()
				.statusCode(200)
				.body(Matchers.isEmptyString());
		return userModel;
	}

	private static UserModel validateInitialUser(UserModel userModel) {
		// That's a "filter" method which validates the UserModel and passes it further
		assertThat("User model should not be null", userModel, notNullValue());
		assertThat("First name should be the default first name", userModel.getFirstName(), equalTo(FIRST));
		assertThat("Last name should be the default last name", userModel.getLastName(), equalTo(LAST));
		return userModel;
	}

	private static UserModel validateUpdatedUser(UserModel userModel) {
		// That's a "filter" method which validates the UserModel and passes it further
		assertThat("User model should not be null", userModel, notNullValue());
		assertThat("First name should be swapped with the last name", userModel.getFirstName(), equalTo(LAST));
		assertThat("Last name should be swapped with the first name", userModel.getLastName(), equalTo(FIRST));
		return userModel;
	}

	/**
	 * Random delay filter.
	 *
	 * This filter is used in multithreaded test to demonstrate the bug.
	 */
	private static UserModel randomDelay(UserModel userModel) {
		try {
			final int millis = RANDOM.nextInt(1000);
			log.debug("Waiting {}ms in {}", millis, Thread.currentThread().getName());
			Thread.sleep(millis);
		} catch (InterruptedException ignored) {
		}
		return userModel;
	}

	private static UserModel getUser(Integer id) {
		log.debug("Getting user {} in {}", id, Thread.currentThread().getName());
		final Response response = when()
				.get(Constants.ENDPOINT + "{id}", id)
		.then()
				.statusCode(200)
				.body(Matchers.startsWith("["), Matchers.endsWith("]"))
				.extract()
				.response();
		return extractModel(response);
	}

	private static UserModel getUser(UserModel userModel) {
		return getUser(userModel.id);
	}

	// TESTS (in alphabetical order)

	@Test
	public void testBeginningUserListIsEmpty() {
		when()
				.get(Constants.ENDPOINT)
		.then()
				.statusCode(200)
				.body(equalTo("[]"));
	}

	@Test
	public void testPostAndDelete() throws ExecutionException, InterruptedException {
		final UserModel output = CompletableFuture.supplyAsync(ComplexAPIIntegrationTest::registerUser)
				.thenApply(ComplexAPIIntegrationTest::deleteUser)
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.get();
		assertThat("Object should be deleted after test", output, nullValue());
	}

	@Test
	public void testPostAndDeleteAndGet() throws ExecutionException, InterruptedException {
		final UserModel output = CompletableFuture.supplyAsync(ComplexAPIIntegrationTest::registerUser)
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.get();
		assertThat("Object should be not null after test", output, notNullValue());
		final UserModel output2 = CompletableFuture.supplyAsync(() -> output.id)
				.thenApply(ComplexAPIIntegrationTest::deleteUser)
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.get();
		assertThat("Object should be deleted after second test", output2, nullValue());
	}

	@Test
	/**
	 * Complicated.
	 *
	 * CREATE a user, then READ, then UPDATE (swap his first and last name), then READ (verify they were swapped).
	 * Then UPDATE (swap his first and last names), then READ (verify they're now original names)
	 * Then UPDATE (swap his first and last names again), then READ (verify they're swapped again) and finally DELETE
	 */
	public void testPostAndGetAndUpdateAndGetAndUpdateAndGetAndUpdateAndGetAndDelete() throws ExecutionException, InterruptedException {
		final UserModel output = CompletableFuture.supplyAsync(ComplexAPIIntegrationTest::registerUser)
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.thenApply(ComplexAPIIntegrationTest::validateInitialUser)
				.thenApply(ComplexAPIIntegrationTest::updateUser)                  // swap first and last name
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.thenApply(ComplexAPIIntegrationTest::validateUpdatedUser)
				.thenApply(ComplexAPIIntegrationTest::updateUser)                  // return first name and last name to default values
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.thenApply(ComplexAPIIntegrationTest::validateInitialUser)
				.thenApply(ComplexAPIIntegrationTest::updateUser)                  // swap first and last name again
				.thenApply(ComplexAPIIntegrationTest::getUser)
				.thenApply(ComplexAPIIntegrationTest::validateUpdatedUser)
				.thenApply(ComplexAPIIntegrationTest::deleteUser)
				.get();
		assertThat("Object should be not null after test", output, notNullValue());
		assertThat("First name should be swapped with the last name", output.getFirstName(), equalTo(LAST));
		assertThat("Last name should be swapped with the first name", output.getLastName(), equalTo(FIRST));
		assertThat("Created object should be deleted after the test", getUser(output), nullValue());
	}

	@Test
	public void testMultithreadedPostAndDelete() throws ExecutionException, InterruptedException {
		// This will cause last test to fail because the app creates 10 users but returns about 5 users.
		final CompletableFuture[] completableFutures = new CompletableFuture[THREADS];
		for (int i = 0; i < THREADS; i++) {
			completableFutures[i] = CompletableFuture.supplyAsync(ComplexAPIIntegrationTest::registerUser)
					.thenApply(ComplexAPIIntegrationTest::getUser)
					.thenApply(ComplexAPIIntegrationTest::randomDelay)
					.thenApply(ComplexAPIIntegrationTest::deleteUser);
		}
		CompletableFuture.allOf(completableFutures).get();
	}

	@Test
	public void testMultithreadedPostAndGetAndUpdateAndGetAndDelete() throws ExecutionException, InterruptedException {
		// This will cause last test to fail because the app creates 10 users but returns about 5 users.
		final CompletableFuture[] completableFutures = new CompletableFuture[THREADS];
		for (int i = 0; i < THREADS; i++) {
			completableFutures[i] = CompletableFuture.supplyAsync(ComplexAPIIntegrationTest::registerUser)
					.thenApply(ComplexAPIIntegrationTest::getUser)
					.thenApply(ComplexAPIIntegrationTest::validateInitialUser)
					.thenApply(ComplexAPIIntegrationTest::updateUser)                  // swap first and last name
					.thenApply(ComplexAPIIntegrationTest::randomDelay)
					.thenApply(ComplexAPIIntegrationTest::getUser)
					.thenApply(ComplexAPIIntegrationTest::validateUpdatedUser)
					.thenApply(ComplexAPIIntegrationTest::randomDelay)
					.thenApply(ComplexAPIIntegrationTest::deleteUser);
		}
		CompletableFuture.allOf(completableFutures).get();
		// Actually we'll never reach this point:
		for (CompletableFuture future: completableFutures) {
			final UserModel userModel = (UserModel) future.get();
			assertThat("User model should have first name swapped with the last name", userModel.getFirstName(), equalTo(LAST));
			assertThat("User model should have last name swapped with the first name", userModel.getLastName(), equalTo(FIRST));
		}
	}

	@Test
	public void testWhenEverythingIsFinishedDatabaseIsClear() {
		// This is the last test, because of the W letter
		when()
				.get(Constants.ENDPOINT)
		.then()
				.statusCode(200)
				.body(equalTo("[]"));
	}
}
