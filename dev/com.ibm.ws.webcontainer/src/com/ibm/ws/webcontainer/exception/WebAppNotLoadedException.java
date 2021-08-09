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
package com.ibm.ws.webcontainer.exception;


public class WebAppNotLoadedException extends WebContainerException
{    
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257008739499192883L;

	public WebAppNotLoadedException(String s)
    {
        super("Failed to load webapp: " + s);
    }
	
	/**
	 * @param s
	 * @param t
	 */
	public WebAppNotLoadedException(String s, Throwable th) {
        super("Failed to load webapp: " + s, th);
	}
	/**
	 * @param th
	 */
	public WebAppNotLoadedException(Throwable th) {
		super("Failed to load webapp: ", th);

	}
}
