/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.component;

import java.util.List;
import java.util.Set;

import org.apache.cxf.jaxrs.sse.SseContextProvider;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;
import com.ibm.ws.jaxrs21.sse.LibertySseEventSinkContextProvider;

/**
 * This class registers the SSE providers - specifically the
 * <code>LibertySseEventSinkContextProvider</code> and
 * <code>SseContextProvider</code>.
 */
@Component(immediate = true)
public class LibertySseFeature implements JaxRsProviderRegister {

    /* (non-Javadoc)
     * @see com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister#installProvider(boolean, java.util.List, java.util.Set)
     */
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        if (!clientSide) {
            providers.add(new LibertySseEventSinkContextProvider());
            providers.add(new SseContextProvider());
        }
    }
}
