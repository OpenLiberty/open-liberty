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
package com.ibm.ws.springboot.support.version40.test.data.app;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import com.ibm.ws.springboot.support.version40.test.data.app.customer.Customer;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = Customer.class, entityManagerFactoryRef = "customerEntityManagerFactory")
@ConditionalOnProperty(name = "test.persistence", havingValue = "eclipselink.weaving")
@EnableLoadTimeWeaving
public class EclipseLinkPersistenceCustomerWeavingConfiguration {

	@Bean
	public LocalContainerEntityManagerFactoryBean customerEntityManagerFactory() {
		EclipseLinkJpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(true);

		LocalContainerEntityManagerFactoryBean emf = new EntityManagerFactoryBuilder(vendorAdapter,
				ds -> Map.of("eclipselink.weaving", "true"), null)
					.dataSource(new JndiDataSourceLookup().getDataSource("jdbc/CUSTOMER_UNIT"))
					.managedTypes(PersistenceManagedTypes.of(Customer.class.getName()))
					.jta(true)
					.build();

		emf.setPersistenceUnitName("customer-unit");
		return emf;
	}
}