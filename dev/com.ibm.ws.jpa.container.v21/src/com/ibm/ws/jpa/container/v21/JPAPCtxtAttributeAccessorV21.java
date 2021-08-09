/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21;

import javax.persistence.PersistenceContext;
import javax.persistence.SynchronizationType;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jpa.management.JPAPCtxtAttributeAccessor;

@Component(service = JPAPCtxtAttributeAccessor.class,
           name = "com.ibm.ws.jpa.pctxtAttributeAccessor",
           property = Constants.SERVICE_RANKING + ":Integer=21")
public class JPAPCtxtAttributeAccessorV21 extends JPAPCtxtAttributeAccessor {
    @Override
    public boolean isUnsynchronized(PersistenceContext pCtxt) {
        return pCtxt.synchronization() == SynchronizationType.UNSYNCHRONIZED;
    }
}
