/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.internal;

import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.event.ExecutorServiceFactory;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class WorkStageExecutorServiceFactory implements ExecutorServiceFactory {

    ExecutorService executorService;

    @Reference
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    protected void unsetExecutorService(ExecutorService executorService) {
        if (executorService == this.executorService) {
            this.executorService = null;
        }
    }

    @Override
    public ExecutorService getExecutorService(String name) {
        return executorService;
    }
}
