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


/**
 * 
 * Interface that the webcontainer expects the response objects to implement. The methods
 * on this interface will be called by the webcontainer in the process of writing back
 * the response.
 *
 * @ibm-private-in-use
 * 
 * @deprecated v7.0 Application developers requiring this functionality
 *  should implement this using com.ibm.websphere.servlet.response.IResponse.
 */

public interface IResponse extends com.ibm.websphere.servlet.response.IResponse {

    //methods moved to com.ibm.websphere.servlet.response.IResponse
}
