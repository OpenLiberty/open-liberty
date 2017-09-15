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
package com.ibm.ws.webcontainer31.osgi.request;

import com.ibm.websphere.servlet31.request.IRequest31;
import com.ibm.ws.webcontainer.osgi.request.IRequestImpl;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;


public class IRequest31Impl extends IRequestImpl implements IRequest31 {

    /**
     * @param connection
     */
    public IRequest31Impl(HttpInboundConnection connection) {
        super(connection);
    }
    
    /**
     * New API added in Servlet 3.1
     * @return
     */
    public long getContentLengthLong(){
      long rc = this.request.getContentLength();

      return rc;
    }
}
