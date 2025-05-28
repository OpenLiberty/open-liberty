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
package com.ibm.ws.springboot.support.version30.test.data.app;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import com.ibm.ws.springboot.support.version30.test.data.app.employee.Employee;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = Employee.class, entityManagerFactoryRef = "employeeEntityManagerFactory")
@ConditionalOnProperty(name = "test.persistence", havingValue = "eclipselink")
public class EclipseLinkPersistenceEmployeeConfiguration {

	@Bean()
	DataSource employeeDataSource() {
		return new JndiDataSourceLookup().getDataSource("jdbc/EMPLOYEE_UNIT");
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean employeeEntityManagerFactory(
			@Qualifier("employeeDataSource") DataSource employeeDataSource) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setJtaDataSource(employeeDataSource);
		em.setPackagesToScan(Employee.class.getPackage().getName());
		em.setPersistenceXmlLocation("classpath:META-INF/persistence-eclipselink.xml");
		em.setPersistenceUnitName("employee-unit");

		EclipseLinkJpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(true);
		em.setJpaVendorAdapter(vendorAdapter);
		return em;
	}
}