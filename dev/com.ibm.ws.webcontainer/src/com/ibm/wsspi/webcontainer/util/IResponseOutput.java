/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.util;

import java.io.IOException;

public interface IResponseOutput 
{
	/* Has the response object obtained a writer? */
	public boolean writerObtained();

	/* Has the response object obtained an outputStream? */
	public boolean outputStreamObtained();
	
    /*************************************
	 ** Methods added for defect 112206 **
	 *************************************/
	public boolean isCommitted();
	
	public void reset();
	
	public void flushBuffer(boolean flushToWire) throws IOException;
}

