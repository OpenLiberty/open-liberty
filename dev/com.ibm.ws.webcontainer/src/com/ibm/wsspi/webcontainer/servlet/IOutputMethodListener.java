/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;

/**
 * 
 * 
 * Interface to alert what type of output is being sent in a response.
 * @ibm-private-in-use
 */
public interface IOutputMethodListener {
	public void notifyWriterRetrieved (PrintWriter pw);
	public void notifyOutputStreamRetrieved(ServletOutputStream sos);
}
