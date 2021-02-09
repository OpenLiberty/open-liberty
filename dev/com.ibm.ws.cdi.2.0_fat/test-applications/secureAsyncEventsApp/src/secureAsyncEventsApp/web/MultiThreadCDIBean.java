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
package secureAsyncEventsApp.web;

import java.security.Principal;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class MultiThreadCDIBean {
    @Inject
    private Principal user;
    @Resource
    private ManagedScheduledExecutorService executor;

    public String getName() {

        return getName(false);
    }

    public String getName(boolean supressException) {

        try {
            Future<String> future = executor.submit(new Callable<String>() {

                @Override
                public String call() throws Exception {
                    System.out.println("user.getName(): " + user.getName());
                    return user.getName();
                }
            });

            String futureGet = future.get();
            return futureGet;
        } catch (InterruptedException e) {
            if (!supressException) {
                e.printStackTrace();
            }
        } catch (ExecutionException e) {
            if (!supressException) {
                e.printStackTrace();
            }
        }
        return "World!";
    }
}
