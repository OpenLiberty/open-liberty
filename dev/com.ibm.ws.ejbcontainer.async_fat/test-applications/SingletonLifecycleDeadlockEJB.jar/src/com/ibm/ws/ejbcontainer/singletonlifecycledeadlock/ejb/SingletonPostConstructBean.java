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

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
@Singleton
@AccessTimeout(10 * 1000)
public class SingletonPostConstructBean {

    @PostConstruct
    public void initialize() {
        System.out.println("SingletonPCBean: PostConstruct");
        OtherPCBean bean = null;
        try {
            bean = (OtherPCBean) new InitialContext().lookup("java:global/SingletonLifecycleDeadlockTest/SingletonLifecycleDeadlockEJB/OtherPCBean!com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.ejb.OtherPCBean");
        } catch (NamingException e1) {
            e1.printStackTrace();
        }
        Future<String> result = bean.asyncMethod();
        try {
            System.out.println("SingletonPCBean: PostConstruct: waiting for Future");
            result.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("SingletonPCBean: PostConstruct: done");
    }

    @Lock(LockType.WRITE)
    public void businessMethod() {
        System.out.println("SingletonPCBean: BusinessMethod");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("SingletonPCBean: BusinessMethod done sleep");
    }

    public void otherBusinessMethod() {
        System.out.println("SingletonPCBeanBean: OtherBusinessMethod");
    }

}
