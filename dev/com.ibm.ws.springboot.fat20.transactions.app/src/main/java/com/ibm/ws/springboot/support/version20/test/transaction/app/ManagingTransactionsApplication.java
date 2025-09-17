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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.config.JtaTransactionManagerFactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;

@SpringBootApplication
public class ManagingTransactionsApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(ManagingTransactionsApplication.class);
	}

	@Bean
	JtaTransactionManager transactionManager() {
		JtaTransactionManager transactionManager = new JtaTransactionManagerFactoryBean().getObject();
		return transactionManager;
	}

	public static void main(String[] args) {
		SpringApplication.run(ManagingTransactionsApplication.class, args);
	}

    @Primary
    @Bean(name = "db1DataSource")
    DataSource db1DataSource() {
    	return new JndiDataSourceLookup().getDataSource("jdbc/DerbyDS1");
    }

    @Primary
    @Bean(name = "db1JdbcTemplate")
    JdbcTemplate db1JdbcTemplate(@Qualifier("db1DataSource") DataSource dataSource) {
    	System.out.println("dataSource class: " + dataSource.getClass());
        return new JdbcTemplate(dataSource);
    }

    @Primary
    @Bean(name = "db2DataSource")
    DataSource db2DataSource() {
    	return new JndiDataSourceLookup().getDataSource("jdbc/DerbyDS2");
    }

    @Primary
    @Bean(name = "db2JdbcTemplate")
    JdbcTemplate db2JdbcTemplate(@Qualifier("db2DataSource") DataSource dataSource) {
    	System.out.println("dataSource class: " + dataSource.getClass());
        return new JdbcTemplate(dataSource);
    }
}
