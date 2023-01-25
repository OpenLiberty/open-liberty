/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package concurrentApp;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.http.HttpServlet;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConcurrentApp extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    @ConfigProperty(name = "scheduledTime")
    private int SCHEDULED_TIME;

    @Inject
    @ConfigProperty(name = "repeatTrigger")
    private Boolean REPEAT_TRIGGER;

    @Inject
    @ConfigProperty(name = "repeatManagedExec")
    private Boolean REPEAT_MANAGED_EXEC;

    @Resource(lookup = "jndi/managedExec")
    ManagedScheduledExecutorService managedExec;

    private Callable<Integer> scheduledTask = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            System.out.println("Scheduled thread completed");
            return 1;
        }
    };

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        System.out.println("Thread scheduled at startup");
        if (REPEAT_TRIGGER) {
            managedExec.schedule(scheduledTask, new RepeatedTrigger(SCHEDULED_TIME, Instant.now()));
        } else if (REPEAT_MANAGED_EXEC) {
            managedExec.scheduleAtFixedRate((Runnable) scheduledTask, SCHEDULED_TIME, SCHEDULED_TIME, TimeUnit.MILLISECONDS);
        } else {
            managedExec.schedule(scheduledTask, SCHEDULED_TIME, TimeUnit.MILLISECONDS);
        }
    }

}
