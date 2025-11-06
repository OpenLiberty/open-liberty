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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class BookingService {

	private final static Logger logger = LoggerFactory.getLogger(BookingService.class);

	private final JdbcTemplate jdbcTemplate1;

	private final TransactionTemplate transactionTemplate;

	public BookingService(@Qualifier("db1JdbcTemplate") JdbcTemplate jdbcTemplate1, DataSourceTransactionManager tm) {
		this.jdbcTemplate1 = jdbcTemplate1;
		// first database will allow first names up to 5 characters long
		jdbcTemplate1.update("create table BOOKINGS(ID bigint, FIRST_NAME varchar(5) NOT NULL)");
		logger.info("jdbcTemplate1 datasource = " + jdbcTemplate1.getDataSource());

		transactionTemplate = new TransactionTemplate(tm);
	}

	public boolean book(String... persons) {
		return transactionTemplate.execute(s -> {
			for (String person : persons) {
				logger.info("Booking1: " + person + " in a seat...");
				jdbcTemplate1.update("insert into BOOKINGS(FIRST_NAME) values (?)", person);
			}
			return true;
		});
	}

	public List<String> findAllBookings1() {
		return jdbcTemplate1.query("select FIRST_NAME from BOOKINGS",
				(rs, rowNum) -> rs.getString("FIRST_NAME"));
	}
}
