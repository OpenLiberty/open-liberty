/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.server.ejb;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import test.TestRemoteInterface;

/**
 * Singleton bean implementation for testing lookup of remote beans, remote asynchronous methods
 * and remote bean state.
 **/
@Singleton
@Remote(TestRemoteInterface.class)
public class TestRemoteSingletonBean {
    private static final Logger logger = Logger.getLogger(TestRemoteSingletonBean.class.getName());

    private final String beanName = TestRemoteSingletonBean.class.getSimpleName();

    private int state = 0;

    /**
     * Simple method that returns the bean name
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Increments bean state, returning new value.
     */
    public int increment(int value) {
        state += value;
        return state;
    }

    /**
     * Verifies the passed remote bean has the same bean name.
     */
    public boolean verifyRemoteBean(TestRemoteInterface remoteBean) {
        String remoteName = (remoteBean == null) ? null : remoteBean.getBeanName();
        if (beanName == null || !beanName.equals(remoteName)) {
            logger.info("verifyRemoteBean: " + beanName + " != " + remoteName);
            return false;
        }
        return true;
    }

    /**
     * Asynchronous methods that returns the bean name.
     */
    @Asynchronous
    public Future<String> asynchMethodReturn() {
        return new AsyncResult<String>(getBeanName());
    }
}