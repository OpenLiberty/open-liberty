/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.ejb;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
@Singleton
@Startup
public class SingletonPredestroyBean {

    @PreDestroy
    public void destroy() {
        System.out.println("SingletonPDBean: PreDestroy");
        OtherPDBean bean = null;
        try {
            bean = (OtherPDBean) new InitialContext().lookup("java:global/SingletonLifecycleDeadlockTest/SingletonLifecycleDeadlockEJB/OtherPDBean!com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.ejb.OtherPDBean");
        } catch (NamingException e1) {
            e1.printStackTrace();
        }
        Future<String> result = bean.asyncMethod();
        try {
            System.out.println("SingletonPDBean: PreDestroy: waiting for Future");
            result.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("SingletonPDBean: PreDestroy: done");
    }

    @Lock(LockType.WRITE)
    public void businessMethod() {
        System.out.println("SingletonPDBean: BusinessMethod");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("SingletonPDBean: BusinessMethod done sleep");
    }

    public void otherBusinessMethod() {
        System.out.println("SingletonPDBean: OtherBusinessMethod");
    }

}
