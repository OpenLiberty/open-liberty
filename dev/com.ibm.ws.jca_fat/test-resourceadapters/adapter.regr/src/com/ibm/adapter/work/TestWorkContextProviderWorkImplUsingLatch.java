/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.work;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.resource.spi.work.WorkContext;

public class TestWorkContextProviderWorkImplUsingLatch extends TestWorkContextProviderWorkImpl {

    /**  */
    private static final long serialVersionUID = 1L;

    private final CountDownLatch latch;

    /**
     * @param workName
     * @param workContexts
     */
    public TestWorkContextProviderWorkImplUsingLatch(String workName,
                                                     List<WorkContext> workContexts,
                                                     CountDownLatch latch) {
        super(workName, workContexts);

        this.latch = latch;
    }

    @Override
    public void setWorkStarted(boolean workAccepted) {
        try {
            System.out.println("Begin waiting on the latch.");
            latch.await();
            System.out.println("The latch let us through, indicating startWork has returned.");
        } catch (InterruptedException e1) {
            System.out.println("InterruptedException caught while waiting on the count down latch.");
        }

        super.setWorkStarted(workAccepted);
    }

}
