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
package concurrent.fat.quartz.web;

import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz job that looks up a resource reference that is only available
 * when running with the context of the application component.
 */
public class LookupOnRunJob implements Job {
    @Override
    public void execute(JobExecutionContext jobCtx) throws JobExecutionException {
        System.out.println("LookupOnRunJob execute on 0x" + Long.toHexString(Thread.currentThread().getId()) + " " + Thread.currentThread().getName());

        try {
            Object instance = InitialContext.doLookup("java:module/env/concurrent/quartzExecutorRef");
            UserTransaction tran = InitialContext.doLookup("java:comp/UserTransaction");
            jobCtx.setResult(new Object[] { instance, tran });
        } catch (NamingException x) {
            System.out.println("ResourceReferenceLookupJob execute failed with " + x);
            jobCtx.setResult(x);
            throw new JobExecutionException(x);
        }
    }
}