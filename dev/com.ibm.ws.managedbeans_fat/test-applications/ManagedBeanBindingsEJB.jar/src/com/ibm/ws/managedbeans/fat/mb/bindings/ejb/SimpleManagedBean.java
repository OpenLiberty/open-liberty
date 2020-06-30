/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.bindings.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.sql.DataSource;

/**
 * Simple un-named Managed Bean.
 **/
@ManagedBean
public class SimpleManagedBean {
    private static final String CLASS_NAME = SimpleManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static int svNextID = 1;

    private final int ivID;
    private String ivValue = null;

    @Resource(name = "myref/injDS")
    private DataSource injDS;

    @Resource(name = "myref/injQ")
    private Queue injQ;

    @Resource(name = "java:app/env/ra")
    private DataSource injResRefDS;

    public SimpleManagedBean() {
        // Use a unique id so it is easy to tell which instance is in use.
        synchronized (SimpleManagedBean.class) {
            svLogger.info("-- ejb.SimpleManagedBean.<init>:" + svNextID);
            ivID = svNextID++;
        }
    }

    /**
     * Returns the unique identifier of this instance.
     */
    public int getIdentifier() {
        svLogger.info("-- getIdentifier : " + this);
        return ivID;
    }

    /**
     * Returns the value.. to verify object is 'stateful'
     */
    public String getValue() {
        svLogger.info("-- getValue : " + this);
        return ivValue;
    }

    /**
     * Sets the value.. to verify object is 'stateful'
     */
    public void setValue(String value) {
        svLogger.info("-- setValue : " + ivValue + "->" + value + " : " + this);
        ivValue = value;
    }

    @Override
    public String toString() {
        return "ejb.SimpleManagedBean(ID=" + ivID + "," + ivValue + ")";
    }

    /**
     * Verifies that the bindings were properly applied.
     **/
    public void verifyBindings() {
        assertNotNull("Expect the datasource to get injected.", injDS);

        try {
            injDS.getConnection();
        } catch (SQLException e) {
            fail("Expected to be able to connect to the data source.");
        }

        assertNotNull("Expect the resource reference to get injected.", injResRefDS);

        try {
            injResRefDS.getConnection();
        } catch (SQLException e) {
            fail("Expected to be able to connect to the resource reference data source.");
        }

        assertNotNull("The queue should be injected.", injQ);

        String queueName = null;
        try {
            queueName = injQ.getQueueName();
        } catch (JMSException e) {
            fail("Expected a different queue name.");
        }

        assertEquals("Expected a different queue name.", queueName, "MBQueue");

        svLogger.info("< " + CLASS_NAME + ".verifyBindings()");
    }
}
