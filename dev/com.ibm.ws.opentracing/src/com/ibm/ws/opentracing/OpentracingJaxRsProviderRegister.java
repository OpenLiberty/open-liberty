/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.opentracing;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

/**
 * <p>The open tracing filter service.</p>
 */
@Component(immediate = true, service = { JaxRsProviderRegister.class })
public class OpentracingJaxRsProviderRegister implements JaxRsProviderRegister {
    private static final TraceComponent tc = Tr.register(OpentracingJaxRsProviderRegister.class);

    private OpentracingClientFilter opentracingClientFilter = null;;
    private OpentracingContainerFilter opentracingContainerFilter = null;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister#installProvider(boolean, java.util.List, java.util.Set)
     */
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        if (clientSide) {
            if (opentracingClientFilter == null) {
                opentracingClientFilter = new OpentracingClientFilter();
                providers.add(opentracingClientFilter);
            }
        } else {
            if (opentracingContainerFilter == null) {
                opentracingContainerFilter = new OpentracingContainerFilter();
                providers.add(opentracingContainerFilter);
            }
        }
    }
}