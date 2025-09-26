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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebContext {
	private final static Logger logger = LoggerFactory.getLogger(WebContext.class);

	@Autowired
	private BookingService bookingService;

	@RequestMapping("/testTransactions")
	public String testTransactions() {
		bookingService.book("Tom", "Dave", "Anjum");
		Assert.isTrue(bookingService.findAllBookings1().size() == 6,
				"First booking should work with no problem");

		logger.info("Tom, Dave and Anjum have been booked");
		try {
			bookingService.book("Colin", "Smitha");
		} catch (RuntimeException e) {
			logger.info("v--- The following exception is expect because 'Smitha' is too " +
					"big for the DB ---v");
			logger.error(e.getMessage());
		}

		for (String person : bookingService.findAllBookings1()) {
			logger.info("Bookings1: So far, " + person + " is booked.");
		}

		logger.info("You shouldn't see Colin or Smitha. Smitha violated DB constraints, " +
				"and Colin was rolled back in the same TX");
		Assert.isTrue(bookingService.findAllBookings1().size() == 6,
				"'Smitha' should have triggered a rollback");

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
		Assert.isTrue(bookingService.findAllBookings1().size() == 6,
				"'null' should have triggered a rollback");

		logger.info("WebContext: TRANSACTION TESTS PASSED");
		return "TESTED TRANSACTIONS";
	}
}
