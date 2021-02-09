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
package com.ibm.ws.managedbeans.fat.mb.web;

import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.transaction.UserTransaction;

/**
 * Simple Managed Bean that has been named, has simple injection,
 * and a PreDestroy.
 **/
@ManagedBean("PreDestroyManagedBean")
public class PreDestroyManagedBean {
    private static final String CLASS_NAME = PreDestroyManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static int svNextID = 1;
    private static int svDestroyed = 0;

    private int ivID;
    private String ivValue = null;

    @Resource
    UserTransaction ivUserTran;

    public static synchronized int getDestroyCount() {
        return svDestroyed;
    }

    public PreDestroyManagedBean() {
        // do nothing since a wrapper will also subclass this
    }

    @PostConstruct
    public void initialize() {
        // Use a unique id so it is easy to tell which instance is in use.
        synchronized (PreDestroyManagedBean.class) {
            svLogger.info("-- web.PreDestroyManagedBean.initialize" + svNextID);
            ivID = svNextID++;
        }
        if (ivUserTran != null) {
            ivValue = "PreDestroyManagedBean.INITIAL_VALUE";
        } else {
            ivValue = "PreDestroyManagedBean.NO_USER_TRAN";
        }
    }

    @PreDestroy
    public void destroy() {
        synchronized (PreDestroyManagedBean.class) {
            svDestroyed++;
            svLogger.info("-- web.PreDestroyManagedBean.destroy:" + ivID +
                          ", destroy count = " + svDestroyed);
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

    /**
     * Returns the injected UserTransaction.
     */
    public UserTransaction getUserTransaction() {
        return ivUserTran;
    }

    @Override
    public String toString() {
        return "web.PreDestroyManagedBean(ID=" + ivID + "," + ivValue + ")";
    }

}
