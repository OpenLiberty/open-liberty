/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

import com.ibm.websphere.servlet.request.extended.IRequestExtended;


/**
 * 
 *
 * Interface that the webcontainer recognizes as the types of requests that it can handle.
 * The webcontainer will call the methods on this interface during request processing.
 *
 * @ibm-private-in-use
 * 
 * @deprecated v7.0 Application developers requiring this functionality
 *  should implement this using com.ibm.websphere.servlet.request.IRequest.
 * 
 */
public interface IRequest extends IRequestExtended
{
	//methods moved to com.ibm.websphere.servlet.request.IRequest
}
