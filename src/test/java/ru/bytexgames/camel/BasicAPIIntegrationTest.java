package ru.bytexgames.camel;

import org.hamcrest.Matchers;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static io.restassured.RestAssured.when;

/**
 * <p>Description: </p>
 * Date: 9/22/16 - 3:11 PM
 *
 * @author Ruslan Balkin <a href="mailto:baron@bytexgames.ru">baron@bytexgames.ru</a>
 * @version 1.0.0.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicAPIIntegrationTest {

	// TESTS (in alphabetical order)

	@Test
	public void testBeginningUserListIsEmpty() {
		when()
				.get(Constants.ENDPOINT)
		.then()
				.statusCode(200)
				.body(Matchers.equalTo("[]"));
	}

	@Test
	public void testDeleteNonexistingUser() {
		// It's weird why the response code is 200
		when().
				delete(Constants.ENDPOINT + Integer.MAX_VALUE).
		then()
				.statusCode(200)
				.body(Matchers.isEmptyString());
	}

	@Test
	public void testReadNonexistingUser() {
		// It's weird why the response code is 200
		when().
				get(Constants.ENDPOINT + Integer.MAX_VALUE).
		then()
				.statusCode(200)
				.body(Matchers.equalTo("[]"));
	}

	@Test
	public void testPutNonexistingUser() {
		// It's weird why the response code is 200
		when().
				put(Constants.ENDPOINT + Integer.MAX_VALUE).
		then()
				.statusCode(200)
				.body(Matchers.isEmptyString());
		// After PUT, user should not appear
		when().
				get(Constants.ENDPOINT + Integer.MAX_VALUE).
		then()
				.statusCode(200)
				.body(Matchers.equalTo("[]"));
	}

	@Test
	public void testUpdateNonexistingUser() {
		// It's weird why the response code is 200
		when().
				put(Constants.ENDPOINT + Integer.MAX_VALUE).
		then()
				.statusCode(200)
				.body(Matchers.isEmptyString());
		// After PUT, user should not appear
		when().
				get(Constants.ENDPOINT + Integer.MAX_VALUE).
		then()
				.statusCode(200)
				.body(Matchers.equalTo("[]"));
	}

	@Test
	public void testProhibitedToDeleteEndpoint() {
		when()
				.delete(Constants.ENDPOINT)
		.then()
				.statusCode(405);
	}

	@Test
	public void testProhibitedToPUTToTheEndpoint() {
		when()
				.put(Constants.ENDPOINT)
		.then()
				.statusCode(405);
	}

	@Test
	public void testProhibitedToPOSTTheInstance() {
		when()
				.post(Constants.ENDPOINT + Integer.MAX_VALUE)
		.then()
				.statusCode(405);
	}

	@Test
	public void testWhenEverythingIsFinishedDatabaseIsClear() {
		// This is the last test, because of the W letter
		when()
				.get(Constants.ENDPOINT)
		.then()
				.statusCode(200)
				.body(Matchers.equalTo("[]"));
	}
}
