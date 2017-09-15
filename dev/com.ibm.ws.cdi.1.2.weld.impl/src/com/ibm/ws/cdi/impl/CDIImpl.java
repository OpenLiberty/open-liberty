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
package com.ibm.ws.cdi.impl;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.AbstractCDI;

import com.ibm.ws.cdi.CDIService;

/**
 * IBM implementation of CDI
 */
public class CDIImpl extends AbstractCDI<Object> {
    private final CDIService cdiService;

    public CDIImpl(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    /** {@inheritDoc} */
    @Override
    public BeanManager getBeanManager() {
        return cdiService.getCurrentBeanManager();
    }

    public static void clear() {
        configuredProvider = null;
    }
}
