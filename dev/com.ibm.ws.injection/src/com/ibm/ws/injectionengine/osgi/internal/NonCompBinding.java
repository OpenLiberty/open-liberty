/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.injectionengine.InjectionBinding;

@Trivial
public class NonCompBinding {
    private static final TraceComponent tc = Tr.register(NonCompBinding.class);

    final OSGiInjectionScopeData scopeData;
    final InjectionBinding<?> binding;
    private int refs;

    NonCompBinding(OSGiInjectionScopeData scopeData, InjectionBinding<?> binding) {
        this.scopeData = scopeData;
        this.binding = binding;
        refs = 1;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "<init>", this);
        }
    }

    @Override
    public String toString() {
        return super.toString() +
               "[binding=" + binding +
               ", scope=" + scopeData +
               ", refs=" + refs +
               ']';
    }

    public void ref() {
        refs++;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ref", this);
        }
    }

    public void unref() {
        refs--;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unref", this);
        }

        if (refs == 0) {
            scopeData.removeNonCompBinding(binding);
        }
    }
}
