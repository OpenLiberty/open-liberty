/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.deltaspike.scheduler;

/**
 *
 */
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.deltaspike.scheduler.api.Scheduled;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Scheduled(cronExpression = "0/2 * * * * ?")
public class MyScheduler implements Job
{
    private static final Logger LOG = Logger.getLogger(MyScheduler.class.getName());

    @Inject
    private GlobalResultHolder globalResultHolder;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        LOG.info("#increase called by " + getClass().getName());
        globalResultHolder.increase();
    }
}
