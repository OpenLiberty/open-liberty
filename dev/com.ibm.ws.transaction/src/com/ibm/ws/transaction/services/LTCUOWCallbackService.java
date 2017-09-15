/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.tx.ltc.embeddable.impl.EmbeddableLTCUOWCallback;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;

/**
 * This service provides access to a UOWScopeCallback implementation
 */
@Component
public class LTCUOWCallbackService implements UOWScopeCallback {

    private UOWScopeCallback callback;

    @Activate
    protected void activate(ComponentContext ctxt) {
        //Get the instance
        callback = EmbeddableLTCUOWCallback.getUserTransactionCallback();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        callback = null;
    }

    @Override
    public void contextChange(int typeOfChange, UOWScope scope) throws IllegalStateException {
        if (callback != null) {
            callback.contextChange(typeOfChange, scope);
        }
    }
}
