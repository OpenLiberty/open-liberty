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
package io.openliberty.springboot.support.version40.test.mbean.app;

import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException;

public interface InterfaceTestMXBean{
	String getMessage(String log_message) throws Exception;
	String setMessage(String message, String log_message);
	int getCounter(String log_message);
	void incrementCounter(String log_message);
	void resetCounter(String log_message);
	String getDescriptor(String log_message);
}