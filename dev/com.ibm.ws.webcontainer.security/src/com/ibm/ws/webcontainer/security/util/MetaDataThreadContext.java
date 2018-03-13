/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.util;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * The thread context that holds the WebModuleMetaData in order to support
 * JSR-375 SecurityContext.authentication method which can be used from EJB.
 * The object deletion is only carried out if supplied key object matches the one
 * which was set along with WebModuleMetaData. This is for supporting servlet
 * initialization which might happen in the first method invocation and at that 
 * time, preinvoke/postinvoke are called during regular preinvoke/postinvoke, 
 * these two invocations are used different WebContext object. so it is used 
 * to avoid deleting WebModuleMetaData which still need to stay in the threadlocal.
 */
public class MetaDataThreadContext {
    private static final TraceComponent tc = Tr.register(MetaDataThreadContext.class);

    private Object key;
    private WebModuleMetaData wmmd;

    /**
     * Sets WebModuleMetaData.
     */
    public void setMetaData(Object key, WebModuleMetaData wmmd) {
        this.key = key;
        this.wmmd = wmmd;
    }

    /**
     * Gets WebModuleMetaData.
     */
    public WebModuleMetaData getMetaData() {
        return wmmd;
    }

    /**
     * Clears WebModuleMetaData if the key matches the stored one.
     */
    public void clearMetaData(Object key) {        if (key == this.key) {
            key = null;
            wmmd = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "MetaData was removed.");
            }
        } else {
        }
    }

}
