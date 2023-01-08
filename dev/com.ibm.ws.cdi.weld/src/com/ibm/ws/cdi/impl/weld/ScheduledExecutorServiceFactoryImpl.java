/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl.weld;

import java.util.concurrent.ScheduledExecutorService;

import org.jboss.weld.resources.spi.ScheduledExecutorServiceFactory;

public class ScheduledExecutorServiceFactoryImpl implements ScheduledExecutorServiceFactory
{
    private ScheduledExecutorService scheduledExecutorService;

    public ScheduledExecutorServiceFactoryImpl(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() {
        scheduledExecutorService = null;
    }

    /** {@inheritDoc} */
    @Override
    public ScheduledExecutorService get() {
        return scheduledExecutorService;
    }
}
