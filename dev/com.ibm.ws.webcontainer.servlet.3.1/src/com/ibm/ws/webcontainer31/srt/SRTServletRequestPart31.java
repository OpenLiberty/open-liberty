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
package com.ibm.ws.webcontainer31.srt;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Part;

import org.apache.commons.fileupload.disk.DiskFileItem;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.srt.SRTServletRequestPart;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class SRTServletRequestPart31 extends SRTServletRequestPart implements Part {
    
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer31.srt");
    private final static TraceComponent tc = Tr.register(SRTServletRequestPart31.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    private static final String CLASS_NAME="com.ibm.ws.webcontainer31.srt.SRTServletRequestPart31";

   // private static final TraceNLS nls = TraceNLS.getTraceNLS(SRTServletRequestPart31.class, "com.ibm.ws.webcontainer.resources.Messages");

    public SRTServletRequestPart31(DiskFileItem item) {
        super(item);
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"SRTServletRequestPart31","constructor");
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.Part#getSubmittedFileName()
     */
    @Override
    public String getSubmittedFileName() {
        if (_part.isFormField()) {
            return null;
        }
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"getSubmittedFileName()",_part.getName());
        }
        return _part.getName();
    }

}
