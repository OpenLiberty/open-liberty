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
package io.openliberty.springboot.support.version30.test.jms.app;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;

@EnableJms
@Configuration
public class JmsConfig {
    @Bean
    public ConnectionFactory connectionFactory() throws NamingException{
        return (ConnectionFactory) new JndiTemplate().lookup("jms/XA_CF1");
    }
    
    @Bean
    public JtaTransactionManager transactionManager() {
    	return new JtaTransactionManager();
    }
	
    @Bean
    public JmsListenerContainerFactory<?> myListenerContainerFactory(ConnectionFactory connectionFactory, JtaTransactionManager transactionManager) throws Exception {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
	    factory.setConnectionFactory(connectionFactory);
	    factory.setTransactionManager(transactionManager);
        return factory;
    }
	 
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean(name = "Q1")
    public Queue queue() throws NamingException {
        return (Queue) new JndiTemplate().lookup("jms/Q1");
    }
    
    @Primary
	@Bean(name = "db1DataSource")
    public DataSource db1DataSource() {
		return new JndiDataSourceLookup().getDataSource("jdbc/DerbyDS1");
	}

	@Primary
	@Bean(name = "db1JdbcTemplate")
	public JdbcTemplate db1JdbcTemplate(@Qualifier("db1DataSource") DataSource dataSource) {
		System.out.println("dataSource class: " + dataSource.getClass());
		return new JdbcTemplate(dataSource);
	}

	@Primary
	@Bean(name = "db2DataSource")
	public DataSource db2DataSource() {
		return new JndiDataSourceLookup().getDataSource("jdbc/DerbyDS2");
	}

	@Primary
	@Bean(name = "db2JdbcTemplate")
	public JdbcTemplate db2JdbcTemplate(@Qualifier("db2DataSource") DataSource dataSource) {
		System.out.println("dataSource class: " + dataSource.getClass());
		return new JdbcTemplate(dataSource);
	}
}
