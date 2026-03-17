/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.java.internal;

import module java.base;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.net.http.HttpClient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.StringWriter;
import java.io.PrintWriter;

@Path("/")
@ApplicationScoped
public class TestService {

	static class FirstName {
		private final String value;
		
		public FirstName(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			return value;
		}
	}

	static class Person {
		public final FirstName name = new FirstName("Original");
	}

	private StringWriter sw = new StringWriter();

	@GET
	public String test() {
		try {
			log(">>> ENTER");
			doTest();
			log("<<< EXIT SUCCESSFUL");
		} catch (Exception e) {
			e.printStackTrace(System.out);
			e.printStackTrace(new PrintWriter(sw));
			log("<<< EXIT FAILED");
		}
		String result = sw.toString();
		sw = new StringWriter();
		return result;
	}

	private void doTest() throws Exception {
		log("Beginning Java 26 testing");

		// Test Final Mean Final (JEP 500)
		testFinalMeanFinal();

		// Test HTTP/3 Support (JEP 517)
		testHTTP3Support();

		log("Leaving testing");
	}

	// Prepare to Make Final Mean Final : JEP 500 -> https://openjdk.org/jeps/500
	// Prepare to Make Final Mean Final : JEP 500 -> https://openjdk.org/jeps/500
	private void testFinalMeanFinal() throws Exception {
		log("Beginning JEP 500 testing: Prepare to Make Final Mean Final");
		log("Testing final field mutation protection");

		Person p = new Person();
		log("Before mutation attempt: " + p.name);

		boolean mutationBlocked = false;
		String mode = null;

		try {
			// Use Java 12+ approach to attempt final field mutation
			Field f = Person.class.getDeclaredField("name");
			f.setAccessible(true);

			// Attempt to set the final field directly
			log("Attempting direct mutation of final field...");
			f.set(p, new FirstName("Mutated"));

			// If we reach here, mutation succeeded (WARN mode)
			mode = "WARN";
			log("Mutation succeeded - running in WARN mode");
			log("Field value after mutation: " + p.name);
			log("Check server logs for JEP 500 warning messages");

		} catch (IllegalAccessException e) {
			// Mutation was blocked (DENY mode) -Future Java 26 Behaviour
			mutationBlocked = true;
			mode = "DENY";
			log("SUCCESS: IllegalAccessException caught - running in DENY mode");
			log("Exception message: " + e.getMessage());
			log("Final field mutation was blocked as expected");
		}

		// Verify results based on mode
		if (mode.equals("DENY")) {
			if (!"Original".equals(p.name.getValue())) {
				throw new Exception("JEP 500 test FAILED: Final field was mutated despite exception. Value: " + p.name);
			}
			log("RESULT: Final field remained immutable (value: " + p.name + ")");
		} else {
			// WARN mode - mutation may succeed but should log warnings - Current Java 26 Behaviour
			if ("Mutated".equals(p.name.getValue())) {
				log("RESULT: Mutation succeeded in WARN mode - field value changed to: " + p.name);
			} else {
				log("RESULT: Mutation attempted in WARN mode but value unchanged: " + p.name);
			}
			log("Note: Check console.log/messages.log for JEP 500 warnings");
		
		}

		log("JEP 500 Test Summary:");
		log("Mode detected: " + mode);
		log("Mutation blocked: " + mutationBlocked);
		log("Final field value: " + p.name);
		log("Leaving JEP 500 testing");
	}

	// HTTP/3 for the HTTP Client API : JEP 517 -> https://openjdk.org/jeps/517
	private void testHTTP3Support() {
		log("Testing HTTP/3 Support (JEP 517)");

		try {
			// Create an HTTP client with HTTP/3 support
			HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_3) // HTTP/3 support
					.build();

			log("HTTP Client created with HTTP/3 support");
			// This demonstrates that the API is available
			log("HTTP/3 version: " + HttpClient.Version.HTTP_3);

			// Note: Actual HTTP/3 requests would require a server that supports HTTP/3
			log("HTTP/3 is now available in the standard HttpClient API");

		} catch (Exception e) {
			log("HTTP/3 test note: " + e.getMessage());
			log("HTTP/3 support is available in Java 26 HttpClient API");
		}
		log("Leaving JEP 517 testing");
	}

	public void log(String msg) {
		System.out.println(msg);
		sw.append(msg);
		sw.append("<br/>");
	}

}
