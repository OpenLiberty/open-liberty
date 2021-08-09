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
package com.ibm.websphere.servlet31.response;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

/**
 *
 */
public class StoredResponse31 extends com.ibm.websphere.servlet.response.StoredResponse {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.websphere.servlet.response");
    private static final String CLASS_NAME = "com.ibm.websphere.servlet.response.StoredResponse";    
    
    /**  */
    private static final long serialVersionUID = 6332168752731823366L;

    /**
     * 
     */
    public StoredResponse31() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param isInclude
     */
    public StoredResponse31(boolean isInclude) {
        super(isInclude);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param extResponse
     * @param isInclude
     */
    public StoredResponse31(IExtendedResponse extResponse, boolean isInclude) {
        super(extResponse, isInclude);
        // TODO Auto-generated constructor stub
    }

    /**
     * 
     */
    public void setContentLengthLong(long len) {
        if (!dummyResponse)
            super.setContentLengthLong(len);
        else if (isInclude)
        {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"setContentLengthLong", nls.getString("Illegal.from.included.servlet", "Illegal from included servlet"), "setContentLengthLong length --> " + len);  
            }
        }
        else {
            // ** TODO Servlet 3.1 setLongHeader???
            // setIntHeader("content-length", len);  
        }    
    }
        
}
