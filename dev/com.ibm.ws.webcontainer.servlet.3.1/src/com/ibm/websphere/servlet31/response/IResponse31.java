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

import com.ibm.websphere.servlet.response.IResponse;

/**
 *
 */
public interface IResponse31 extends IResponse {

    
    /**
     * Sets the length of the content body in the response In HTTP servlets, this method sets the HTTP Content-Length header.
     * @param length
     */
    public void setContentLengthLong(long length);
    

}
