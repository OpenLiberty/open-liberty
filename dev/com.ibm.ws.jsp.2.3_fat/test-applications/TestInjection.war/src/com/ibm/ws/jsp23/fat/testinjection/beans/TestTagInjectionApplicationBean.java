/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.testinjection.beans;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@SessionScoped
@Named
public class TestTagInjectionApplicationBean implements Serializable {

    private static final long serialVersionUID = -108725347266997794L;
    private static AtomicInteger preDestroyCount = new AtomicInteger(0);

    public static String getPreDestroyCount() {
        return "The preDestroyCount is " + preDestroyCount.get();
    }

    public String getHitMe() {
        String response = "ApplicationBean Hit";
        return response;
    }

    @PreDestroy
    public void destruct() {
        preDestroyCount.incrementAndGet();
        //System.out.println("Calling PreDestroy on " + this);
    }

    private int myCounter = 0;

    public int incAndGetMyCounter() {
        myCounter++;
        return myCounter;
    }
}
