/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.jpa.container.v40;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.SynchronizationType;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.management.JPAPCtxtAttributeAccessor;

@Component(service = JPAPCtxtAttributeAccessor.class,
           name = "com.ibm.ws.jpa.pctxtAttributeAccessor",
           property = Constants.SERVICE_RANKING + ":Integer=21")
public class JPAPCtxtAttributeAccessorV40 extends JPAPCtxtAttributeAccessor {
    @Override
    public boolean isUnsynchronized(PersistenceContext pCtxt) {
        return pCtxt.synchronization() == SynchronizationType.UNSYNCHRONIZED;
    }
}
