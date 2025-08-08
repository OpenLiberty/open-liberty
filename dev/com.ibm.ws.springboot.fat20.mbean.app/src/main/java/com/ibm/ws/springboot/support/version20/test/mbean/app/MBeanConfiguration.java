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
package com.ibm.ws.springboot.support.version20.test.mbean.app;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedResource;

import junit.framework.Assert;

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
