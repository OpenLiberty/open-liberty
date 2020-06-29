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
package com.ibm.ws.managedbeans.fat.mb.ejb;

import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.transaction.UserTransaction;

/**
 * Simple Managed Bean that has been named, has simple injection,
 * and a PostConstruct.
 **/
@ManagedBean("PostConstructManagedBean")
public class PostConstructManagedBean {
    private static final String CLASS_NAME = PostConstructManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static int svNextID = 1;

    private final int ivID;
    private String ivValue = null;

    @Resource
    UserTransaction ivUserTran;

    public PostConstructManagedBean() {
        // Use a unique id so it is easy to tell which instance is in use.
        synchronized (PostConstructManagedBean.class) {
            svLogger.info("-- ejb.PostConstructManagedBean.<init>:" + svNextID);
            ivID = svNextID++;
        }
    }

    @PostConstruct
    public void initialize() {
        svLogger.info("-- ejb.PostConstructManagedBean.initialize:" + svNextID);
        if (ivUserTran != null) {
            ivValue = "PostConstructManagedBean.INITIAL_VALUE";
        } else {
            ivValue = "PostConstructManagedBean.NO_USER_TRAN";
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
        return "ejb.PostConstructManagedBean(ID=" + ivID + "," + ivValue + ")";
    }

}
