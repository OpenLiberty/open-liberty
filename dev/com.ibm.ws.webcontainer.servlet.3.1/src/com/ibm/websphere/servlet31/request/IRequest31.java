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
package com.ibm.websphere.servlet31.request;

import com.ibm.websphere.servlet.request.extended.IRequestExtended;

/**
 *
 */
public interface IRequest31 extends IRequestExtended {

    
    /**
     * Method for getting the Content Length of the Request
     * @return long the length of data in the request. Added
     * for Servlet 3.1 support.
     **/
    public long getContentLengthLong();    
}
