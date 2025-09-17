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
package com.ibm.ws.springboot.support.version20.test.data.app;

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jndi.JndiTemplate;

import com.ibm.ws.springboot.support.version20.test.data.app.customer.Customer;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = Customer.class, entityManagerFactoryRef = "customerEntityManagerFactory")
@ConditionalOnProperty(name = "test.persistence", havingValue = "liberty")
public class LibertyPersistenceCustomerConfiguration {
	@Bean
	EntityManagerFactory customerEntityManagerFactory() throws NamingException {
		return ((EntityManagerFactory) new JndiTemplate().lookup("java:comp/env/persistence/customerEMF"))
				// must unwrap the JPAEMFactory wrapper to avoid class visibility issues with Spring proxies
				.unwrap(EntityManagerFactory.class);
	}
}