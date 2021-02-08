/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.timer;

import com.ibm.ws.cdi.ejb.apps.timer.view.EjbSessionBeanLocal;

public class IncrementCountersRunnableTask implements Runnable {
    EjbSessionBeanLocal bean;

    public IncrementCountersRunnableTask(EjbSessionBeanLocal ejbSessionBean) {
        this.bean = ejbSessionBean;
    }

    @Override
    public void run() {
        try {
            System.out.println("Scheduled Task occurred");
            bean.incRequestCounter();
            System.out.println("Poll token by task!");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
