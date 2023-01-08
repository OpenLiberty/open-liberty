/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package timeoutTest;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@ApplicationScoped
public class TimeoutService {

    @Inject
    @ConfigProperty(name = "timeout")
    private Boolean createTimeout;

    @Asynchronous
    public Future<Void> spawnTimeoutThread() {
        Future<Void> completable = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(produceTimeout());
                } catch (TimeoutException te) {
                    System.out.println("TIMEOUT OCCURED");
                    System.out.println(te.getClass().getName());
                } catch (Exception e) {
                    System.out.println("Exception Thrown");
                    System.out.println(e.getClass().getName());
                }
            }
        });
        return completable;
    }

    @Timeout(value = 20, unit = ChronoUnit.SECONDS)
    public String produceTimeout() throws TimeoutException, InterruptedException {
        if (createTimeout) {
            Thread.sleep(Long.MAX_VALUE);
        } else {
            Thread.sleep(15000);
        }
        return "timeout did not occur";
    }

}
