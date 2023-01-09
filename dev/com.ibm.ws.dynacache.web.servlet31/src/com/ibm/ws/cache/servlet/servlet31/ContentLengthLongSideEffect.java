/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 
package com.ibm.ws.cache.servlet.servlet31;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cache.servlet.ResponseSideEffect;


/**
 * @author kortega
 *
 */
public class ContentLengthLongSideEffect implements ResponseSideEffect {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -1359718310565387970L;
	private long length = 0L;
    
    public String toString() {
        StringBuffer sb = new StringBuffer("Content length long side effect: \n\t");
        sb.append("length: ").append(length).append("\n");
        return sb.toString();
     }
    
    /**
     * Constructor with parameter.
     * 
     * @param length The content length.
     */
    public
    ContentLengthLongSideEffect(long length)
    {
        this.length = length;
    }
    
	/* (non-Javadoc)
	 * @see com.ibm.ws.cache.servlet.ResponseSideEffect#performSideEffect(javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void performSideEffect(HttpServletResponse response) {
		 response.setContentLengthLong(length);

	}

}
