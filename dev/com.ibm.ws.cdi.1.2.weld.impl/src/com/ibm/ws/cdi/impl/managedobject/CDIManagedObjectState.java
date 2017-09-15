/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import org.jboss.weld.construction.api.WeldCreationalContext;

import com.ibm.ws.managedobject.ManagedObjectContext;

/**
 * Wrapper around state that CDI cares about for an Object
 */
public class CDIManagedObjectState implements ManagedObjectContext {

    private static final long serialVersionUID = 1L;

    private WeldCreationalContext<?> _cc = null;

    public CDIManagedObjectState(WeldCreationalContext<?> creationalContext) {
        _cc = creationalContext;
    }

    @Override
    public void release() {
        if (_cc != null)
            _cc.release();
    }

    @Override
    public <T> T getContextData(Class<T> klass) {
        if (klass == WeldCreationalContext.class)
            return klass.cast(_cc);
        return null;
    }

}
