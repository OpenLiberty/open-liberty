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
public class LookupOnInitJob implements Job {
    private Object lookedUpInstance;
    private UserTransaction lookedUpTran;

    public LookupOnInitJob() throws NamingException {
        // Quartz uses the ThreadExecutor for its single scheduler thread that instantiates the job,
        // which allows this to work,
        lookedUpInstance = InitialContext.doLookup("java:module/env/concurrent/quartzExecutorRef");
        lookedUpTran = InitialContext.doLookup("java:comp/UserTransaction");
        System.out.println("instantiate on 0x" + Long.toHexString(Thread.currentThread().getId()) + " " + Thread.currentThread().getName());
    }

    @Override
    public void execute(JobExecutionContext jobCtx) throws JobExecutionException {
        System.out.println("LookupOnInitJob execute on 0x" + Long.toHexString(Thread.currentThread().getId()) + " " + Thread.currentThread().getName());
        jobCtx.setResult(new Object[] { lookedUpInstance, lookedUpTran });
    }
}