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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jndi.JndiObjectFactoryBean;

import com.ibm.ws.springboot.support.version30.test.data.app.employee.Employee;

import jakarta.persistence.EntityManagerFactory;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = Employee.class, entityManagerFactoryRef = "employeeEntityManagerFactory")
@ConditionalOnProperty(name = "test.persistence", havingValue = "liberty")
public class LibertyPersistenceEmployeeConfiguration {
	@Bean
	JndiObjectFactoryBean employeeEntityManagerFactory() {
		JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
		factoryBean.setJndiName("java:comp/env/persistence/employeeEMF");
		// must force proxy of EMF to avoid class visibility issues
		factoryBean.setProxyInterface(EntityManagerFactory.class);
		return factoryBean;
	}

//	@Bean
//	LocalContainerEntityManagerFactoryBean employeeEntityManagerFactory() {
//		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean() {
//			@Override
//			protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
//				try {
//					return (EntityManagerFactory) new JndiTemplate().lookup("java:comp/env/persistence/employeeEMF");
//				} catch (NamingException e) {
//					throw new PersistenceException(e);
//				}
//			}
//		};
//		factoryBean.setPersistenceUnitName("employee-unit");
//		return factoryBean;
//	}
}