/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.simultaneousRequests.test;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

@ApplicationScoped
public class TestBean {

    @Inject
    Config config;

    private static AtomicBoolean firstRequest = new AtomicBoolean(true);

    public void testSimultaneousRequests() throws InterruptedException {
        TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue"); //Ensure that a Config object is actually associated with both requests before either destroys it's Config.

        Exception e = null;
        if (firstRequest.compareAndSet(true, false)) {
            delayFirstThread();
            TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue");
        } else {
            TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue");
            synchronized (firstRequest) {
                firstRequest.notifyAll();
            }
        }
    }

    private void delayFirstThread() throws InterruptedException {
        synchronized (firstRequest) {
            firstRequest.wait();
            Thread.sleep(5000); //We wait for the second thread to finish, plus extra time for Liberty's internals to ensure that if Config will get deleted we will see it.
        }
    }

}
