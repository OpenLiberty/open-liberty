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
package io.openliberty.springboot.support.version30.test.transaction.app;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingService {

	private final static Logger logger = LoggerFactory.getLogger(BookingService.class);

	private final JdbcTemplate jdbcTemplate1;
	private final JdbcTemplate jdbcTemplate2;

	public BookingService(@Qualifier("db1JdbcTemplate") JdbcTemplate jdbcTemplate1, @Qualifier("db2JdbcTemplate") JdbcTemplate jdbcTemplate2) {
		this.jdbcTemplate1 = jdbcTemplate1;
		// first database will allow first names up to 10 characters long
		jdbcTemplate1.update("create table BOOKINGS(ID bigint, FIRST_NAME varchar(10) NOT NULL)");
		logger.info("jdbcTemplate1 datasource = " + jdbcTemplate1.getDataSource());
		this.jdbcTemplate2 = jdbcTemplate2;
		// second database will allow first names up to 5 characters long
		jdbcTemplate2.update("create table BOOKINGS(ID bigint, FIRST_NAME varchar(5) NOT NULL)");
		logger.info("jdbcTemplate2 datasource = " + jdbcTemplate2.getDataSource());
	}

	@Transactional
	public void book(String... persons) {
		// insert into the first database that allows 10 chars so it succeeds
		for (String person : persons) {
			logger.info("Booking1: " + person + " in a seat...");
			jdbcTemplate1.update("insert into BOOKINGS(FIRST_NAME) values (?)", person);
		}
		// insert into the second database which may fail because it only allows up to 5 chars
		for (String person : persons) {
			logger.info("Booking2: " + person + " in a seat...");
			jdbcTemplate2.update("insert into BOOKINGS(FIRST_NAME) values (?)", person);
		}
	}

	public List<String> findAllBookings1() {
		return jdbcTemplate1.query("select FIRST_NAME from BOOKINGS",
				(rs, rowNum) -> rs.getString("FIRST_NAME"));
	}

	public List<String> findAllBookings2() {
		return jdbcTemplate2.query("select FIRST_NAME from BOOKINGS",
				(rs, rowNum) -> rs.getString("FIRST_NAME"));
	}
}
