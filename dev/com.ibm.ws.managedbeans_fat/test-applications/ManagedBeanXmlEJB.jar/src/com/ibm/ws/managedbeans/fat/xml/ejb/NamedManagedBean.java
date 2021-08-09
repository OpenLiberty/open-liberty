/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.xml.ejb;

import java.util.logging.Logger;

import javax.annotation.ManagedBean;

/**
 * Simple Managed Bean that has been named.
 **/
@ManagedBean("NamedManagedBean")
public class NamedManagedBean {
    private static final String CLASS_NAME = NamedManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static int svNextID = 1;

    private final int ivID;
    private String ivValue = null;

    public NamedManagedBean() {
        // Use a unique id so it is easy to tell which instance is in use.
        synchronized (NamedManagedBean.class) {
            svLogger.info("-- ejb.NamedManagedBean.<init>:" + svNextID);
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
        return "ejb.NamedManagedBean(ID=" + ivID + "," + ivValue + ")";
    }

}
