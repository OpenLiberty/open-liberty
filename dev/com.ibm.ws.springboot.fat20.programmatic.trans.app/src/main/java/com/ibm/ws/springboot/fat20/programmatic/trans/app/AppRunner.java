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
package com.ibm.ws.springboot.fat20.programmatic.trans.app;

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
	}

	private void testTransactions() {
		bookingService.book("Alice", "Bob", "Carol");
		Assert.isTrue(bookingService.findAllBookings1().size() == 3,
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
		logger.info("You shouldn't see Chris or Samuel. Samuel violated DB constraints, " +
				"and Chris was rolled back in the same TX");
		Assert.isTrue(bookingService.findAllBookings1().size() == 3,
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
		logger.info("You shouldn't see Buddy or null. null violated DB constraints, and " +
				"Buddy was rolled back in the same TX");
		Assert.isTrue(bookingService.findAllBookings1().size() == 3,
				"'null' should have triggered a rollback");

		logger.info("AppRunner: TRANSACTION TESTS PASSED");
	}

}
