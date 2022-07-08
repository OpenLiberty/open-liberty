/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.osgi.request;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.request.IRequest40Impl;
import com.ibm.wsspi.http.HttpInboundConnection;

import io.openliberty.websphere.servlet60.IRequest60;


/**
 *
 */
public class IRequest60Impl extends IRequest40Impl implements IRequest60 {

    private static final TraceComponent tc = Tr.register(IRequest60Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param connection
     */
    public IRequest60Impl(HttpInboundConnection connection) {
        super(connection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor", " inboundConnection [" + connection + "]");
        }
      
    }
}
