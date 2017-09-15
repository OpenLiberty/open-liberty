/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;

//Defect 191113: The reason for this exception is to allow BatchJmsEndpointListener to
//distinguish between exceptions that it should vs. shouldn't roll back the message upon
public class BatchSubmitInvalidParametersException extends BatchContainerRuntimeException {
	
	private static final long serialVersionUID = 1L;

	public BatchSubmitInvalidParametersException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
	
	public BatchSubmitInvalidParametersException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

}
