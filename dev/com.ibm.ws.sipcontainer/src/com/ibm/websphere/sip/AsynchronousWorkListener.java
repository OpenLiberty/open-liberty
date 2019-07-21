/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.sip;

import java.io.Serializable;

/**
 * This listener will receive notification on a result of the asynchronous work execution. 
 * @See {@link com.ibm.websphere.sip.AsynchronousWork}
 * <p>
 * 
 * @author Galina Rubinshtein, Dec 2008
 * @ibm-api
 */

public interface AsynchronousWorkListener {
	
	/**
	 * This method will be called when the asynchronous work is completed.
	 * @param result
	 */
	public void onCompleted(Serializable result);	
	
	/**
	 * This method will be called when the response to the asynchronous work request on the receiving container returns an error.
	 * 
	 * @param reasonCode int
	 * @param reason String
	 */
	public void onFailed(int reasonCode, String reason); 

}
