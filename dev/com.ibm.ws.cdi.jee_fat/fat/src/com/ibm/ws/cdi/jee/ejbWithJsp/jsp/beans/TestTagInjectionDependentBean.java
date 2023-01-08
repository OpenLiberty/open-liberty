/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.ejbWithJsp.jsp.beans;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

@Named
@Dependent
public class TestTagInjectionDependentBean {

    private static AtomicInteger preDestroyCount = new AtomicInteger(0);

    public static String getPreDestroyCount() {
        return "The preDestroyCount is " + preDestroyCount.get();
    }

    public String getHitMe() {
        String response = "DependentBean Hit";
        return response;
    }

    @PreDestroy
    public void destruct() {
        preDestroyCount.incrementAndGet();
        // System.out.println("Calling PreDestroy on " + this);
    }

    private int myCounter = 0;

    public int incAndGetMyCounter() {
        myCounter++;
        return myCounter;
    }
}
