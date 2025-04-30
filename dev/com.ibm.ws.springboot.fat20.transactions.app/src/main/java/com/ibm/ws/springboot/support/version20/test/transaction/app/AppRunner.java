/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.transaction.app;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
class AppRunner implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(AppRunner.class);

	private final BookingService bookingService;

	public AppRunner(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@Override
	public void run(String... args) throws Exception {
		try {
			testTransactions();
		} catch (Exception e) {
			logger.error("Exception on testTransactions.", e);
		}
		try {
			testJNDI("AppRunner");
		} catch (Exception e) {
			logger.error("Exception on testJNDI.", e);
		}
	}

	private void testTransactions() {
		bookingService.book("Alice", "Bob", "Carol");
		Assert.isTrue(bookingService.findAllBookings1().size() == 3,
				"First booking should work with no problem");
		Assert.isTrue(bookingService.findAllBookings2().size() == 3,
				"First booking should work with no problem");
		logger.info("Alice, Bob and Carol have been booked");
		try {
			bookingService.book("Chris", "Samuel");
		} catch (RuntimeException e) {
			logger.info("v--- The following exception is expect because 'Samuel' is too " +
					"big for the DB ---v");
			logger.error(e.getMessage());
		}

		for (String person : bookingService.findAllBookings1()) {
			logger.info("Bookings1: So far, " + person + " is booked.");
		}
		for (String person : bookingService.findAllBookings2()) {
			logger.info("Bookings2: So far, " + person + " is booked.");
		}
		logger.info("You shouldn't see Chris or Samuel. Samuel violated DB constraints, " +
				"and Chris was rolled back in the same TX");
		Assert.isTrue(bookingService.findAllBookings1().size() == 3,
				"'Samuel' should have triggered a rollback");
		Assert.isTrue(bookingService.findAllBookings2().size() == 3,
				"'Samuel' should have triggered a rollback");
		try {
			bookingService.book("Buddy", null);
		} catch (RuntimeException e) {
			logger.info("v--- The following exception is expect because null is not " +
					"valid for the DB ---v");
			logger.error(e.getMessage());
		}

		for (String person : bookingService.findAllBookings1()) {
			logger.info("Bookings1: So far, " + person + " is booked.");
		}
		for (String person : bookingService.findAllBookings2()) {
			logger.info("Bookings2: So far, " + person + " is booked.");
		}
		logger.info("You shouldn't see Buddy or null. null violated DB constraints, and " +
				"Buddy was rolled back in the same TX");
		Assert.isTrue(bookingService.findAllBookings1().size() == 3,
				"'null' should have triggered a rollback");
		Assert.isTrue(bookingService.findAllBookings2().size() == 3,
				"'null' should have triggered a rollback");

		logger.info("AppRunner: TRANSACTION TESTS PASSED");
	}

	public static void testJNDI(String tag) {
		try {
			printJavaJNDI();
		} catch (NamingException e) {
			logger.error("Error on printJavaJNDI.", e);
		}

		
		try {
			InitialContext ic = new InitialContext();
			Object tm = ic.lookup("java:comp/TransactionManager");
			Object ejb = ic.lookup("java:global/ejbapp1/TestObserver");
			if (tm == null || ejb == null) {
				logger.error(tag + ": JNDI TESTS FAILED : tm=" + tm + " ejb=" + ejb);
			} else {
				logger.info(tag + ": JNDI TESTS PASSED");
			}
		} catch (NamingException e) {
			logger.error(tag + ": JNDI TESTS FAILED", e);
		}
	}

	private static void printJavaJNDI() throws NamingException {
		logger.info('\n' + "java:comp\n" + print(toMap((Context) new InitialContext().lookup("java:comp")), new StringBuilder(), "").toString());
		logger.info('\n' + "java:global\n" + print(toMap((Context) new InitialContext().lookup("java:global")), new StringBuilder(), "").toString());
		logger.info('\n' + "java:app\n" + print(toMap((Context) new InitialContext().lookup("java:app")), new StringBuilder(), "").toString());
		logger.info('\n' + "java:module\n" + print(toMap((Context) new InitialContext().lookup("java:module")), new StringBuilder(), "").toString());
	}

	@SuppressWarnings("unchecked")
	private static StringBuilder print(Map<String, Object> contextMap, StringBuilder builder, String indent) {
		builder.append('{').append('\n');
		for (Map.Entry<String, Object> entry : contextMap.entrySet()) {
			builder.append(indent).append("    \"").append(entry.getKey()).append("\": ");
			if (entry.getValue() instanceof Map) {
				print((Map<String, Object>) entry.getValue(), builder, indent + "    ");
			} else {
				builder.append('"').append(entry.getValue()).append('"').append('\n');
			}
		}
		builder.append(indent).append('}').append('\n');
		return builder;
	}

	public static Map<String, Object> toMap(Context ctx) throws NamingException {
		Map<String, Object> map = new LinkedHashMap<>();
		String namespace = ctx instanceof InitialContext ? ctx.getNameInNamespace() : "";
		try {
			NamingEnumeration<NameClassPair> list = ctx.list(namespace);
			while (list.hasMoreElements()) {
				NameClassPair next = list.next();
				String name = next.getName();
				if (name.isEmpty()) {
					continue;
				}
				String jndiPath = namespace + name;
				Object lookup;
	
				try {
					Object tmp = ctx.lookup(jndiPath);
					if (tmp instanceof Context) {
						lookup = toMap((Context) tmp);
					} else {
						lookup = next.getClassName() + " - " + tmp.toString();
					}
				} catch (Throwable t) {
					lookup = t.getMessage();
				}
				map.put(name, lookup);
			}
		} catch (NamingException e) {
			// ignore
		}
		return map;
	}
}
