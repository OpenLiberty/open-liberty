/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.mbean.app;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MBeanServerFactoryBean;

@Configuration
public class MBeanConfiguration {
	
	@Bean
	MBeanExporter exporter(TestMBean testMBean) {
		MBeanExporter exporter = new MBeanExporter();
		Map<String, Object> beans = new HashMap<>();
		beans.put("bean:name=testBean1", testMBean);
		exporter.setBeans(beans);
		return exporter;
	}

	@Bean
	TestMBean testMBean() throws Exception {
		TestMBean testMBean = new TestMBean();
		return testMBean;
	}

}
