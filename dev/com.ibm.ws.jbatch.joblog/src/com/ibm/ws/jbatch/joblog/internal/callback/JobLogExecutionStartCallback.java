/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog.internal.callback;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.callback.IJobExecutionStartCallbackService;
import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.ws.jbatch.joblog.services.IJobLogManagerService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class JobLogExecutionStartCallback implements
                IJobExecutionStartCallbackService {
    private IJobLogManagerService joblogManager;

    @Reference
    protected void setJobLogManagerService(IJobLogManagerService reference) {
        this.joblogManager = reference;
    }

    @Override
    public void jobStarted(WorkUnitDescriptor ctx) {
        if (joblogManager != null) {
            joblogManager.workUnitStarted(ctx);
        }
    }

}
