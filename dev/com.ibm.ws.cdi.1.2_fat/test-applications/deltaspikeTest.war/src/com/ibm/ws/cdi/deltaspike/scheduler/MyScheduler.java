/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
