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
package com.ibm.ws.webcontainer31.servlet;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;

/**
 * This class provides restriction functionality for included responses.
 */
public class IncludedResponse31 extends com.ibm.ws.webcontainer.servlet.IncludedResponse {
    private static final TraceNLS nls = TraceNLS.getTraceNLS(IncludedResponse31.class, "com.ibm.ws.webcontainer.resources.Messages");
    private static final TraceComponent tc = Tr.register(IncludedResponse31.class, WebContainerConstants.TR_GROUP);


    /* (non-Javadoc)
     * @see javax.servlet.ServletResponse#setContentLengthLong(long)
     */
    @Override
    public void setContentLengthLong(long arg0) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setContentLengthLong " + nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"));
        } 
    }
}
