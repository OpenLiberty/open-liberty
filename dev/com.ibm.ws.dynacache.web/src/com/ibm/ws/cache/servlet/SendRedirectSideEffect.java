/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class SendRedirectSideEffect implements ResponseSideEffect
{
    private static final long serialVersionUID = -5172434918005316276L;
    private String url = null;

    public String toString() {
       StringBuffer sb = new StringBuffer("SendRedirect side effect:\n\t");
       sb.append("url: ").append(url).append("\n\t");
       return sb.toString();
    }

    public SendRedirectSideEffect(String url) {
       this.url = url;
    }

    public void performSideEffect(HttpServletResponse response) {
       try {
          response.sendRedirect(url);
       } catch (IOException e) {
         com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.SendRedirectSideEffect.performSideEffect", "47", this);
          throw new IllegalStateException(e.getMessage());
       }
    }    
}
