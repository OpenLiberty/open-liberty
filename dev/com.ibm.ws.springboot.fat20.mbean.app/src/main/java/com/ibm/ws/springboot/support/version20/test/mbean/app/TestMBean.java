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
package com.ibm.ws.springboot.support.version20.test.mbean.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMBean implements InterfaceTestMXBean{
	
    private String message = "Default Message";
    private int counter = 0;
    private final static Logger logger = LoggerFactory.getLogger(TestMBean.class);

    @Override
    public int getCounter(String log_message) {
        logger.info(log_message + " Get Current Counter Value: " + counter + ": PASSED");
        return counter;
    }

    @Override
    public void incrementCounter(String log_message) {
        int increment = counter++;
        logger.info(log_message + " Increment Counter by: " + increment + ": PASSED");
    }

    @Override
    public void resetCounter(String log_message) {
        counter = 0;
        logger.info(log_message + " Reset Counter Value to: " + counter + ": PASSED");
    }

    @Override
    public String setMessage(String message, String log_message) {
        this.message = message;
        logger.info(log_message + " Set MBean Message with new message value: " + message + ": PASSED");
        return null;
    }

    @Override
    public String getMessage(String log_message) throws Exception {
        if (message == null) {
            logger.error("The value for message was set incorrectly");
        }
        logger.info(log_message + " Get MBean Message with message value: " + message + ": PASSED");
        return message;
    }

    @Override
    public String getDescriptor(String log_message) {
        logger.info(log_message + " Get MBean Descriptor with description: " + "Test MBean" + ": PASSED");
        return "Test MBean";
    }

}