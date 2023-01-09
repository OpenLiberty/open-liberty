/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webcontainerext.ws;

import javax.servlet.http.HttpServletRequest;
/**
 *
 */
public interface PrepareJspServletRequest {
    
    public void setQueryString(String string);

    public void setRequestURI(String string);

    public void setServletPath(String string);
    
    public HttpServletRequest getHttpServletRequest();

}
