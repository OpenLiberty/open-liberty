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
package com.ibm.ws.cdi.ejb.impl;

import javax.ejb.NoSuchEJBException;

import org.jboss.weld.ejb.api.SessionObjectReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBReference;

public class SessionObjectReferenceImpl implements SessionObjectReference {
    private static final long serialVersionUID = 7454276397339715750L;

    private static final TraceComponent tc = Tr.register(SessionObjectReferenceImpl.class);

    private EJBReference reference;

    public SessionObjectReferenceImpl(EJBReference ref) {
        this.reference = ref;
    }

    @Override
    public <S> S getBusinessObject(Class<S> businessInterfaceType) {
        if (reference == null) {
            throw new NoSuchEJBException();
        }
        S result = reference.getBusinessObject(businessInterfaceType);
        return result;
    }

    @Override
    public void remove() {
        if (reference != null) {
            try {
                reference.remove();
                reference = null;
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "failed to remove stateful session bean", t);
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return reference == null;
    }
}
