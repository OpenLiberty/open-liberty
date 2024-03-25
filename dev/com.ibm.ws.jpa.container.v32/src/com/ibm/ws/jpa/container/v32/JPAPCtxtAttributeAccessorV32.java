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
package com.ibm.ws.jpa.container.v32;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.SynchronizationType;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.management.JPAPCtxtAttributeAccessor;

@Component(service = JPAPCtxtAttributeAccessor.class,
           name = "com.ibm.ws.jpa.pctxtAttributeAccessor",
           property = Constants.SERVICE_RANKING + ":Integer=21")
public class JPAPCtxtAttributeAccessorV32 extends JPAPCtxtAttributeAccessor {
    @Override
    public boolean isUnsynchronized(PersistenceContext pCtxt) {
        return pCtxt.synchronization() == SynchronizationType.UNSYNCHRONIZED;
    }
}
