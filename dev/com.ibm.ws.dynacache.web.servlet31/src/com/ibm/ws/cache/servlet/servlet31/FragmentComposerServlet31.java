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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.servlet.CacheProxyRequest;
import com.ibm.ws.cache.servlet.CacheProxyResponse;
import com.ibm.ws.cache.servlet.FragmentComposer;


/**
 * There is a FragmentComposer for each call to include an entry (ie, each
 * include or forward call).
 * 
 * The response object keeps a stack of these cooresponding to the current level
 * of entry execution.
 * 
 * @ibm-private-in-use
 */
public class FragmentComposerServlet31 extends FragmentComposer {

	private static TraceComponent tc = Tr.register(FragmentComposerServlet31.class,
			"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");


	/**
	 * Constructor.
	 */
	public FragmentComposerServlet31(CacheProxyRequest request, CacheProxyResponse response) 
	{
		super(request,response);
	}

	public FragmentComposerServlet31() { super(); }		
	
	/**
	 * This adds a content length to the list of state that is remembered just
	 * prior to the execution of a JSP so that it can be executed again without
	 * executing its parent JSP.
	 * 
	 * @param contentLength
	 *            The content length.
	 */
	protected void setContentLengthLong(long contentLength) {
		if (getConsumeSubfragments() || currentChild == null) {

			ContentLengthLongSideEffect contentLengthSideEffect = new ContentLengthLongSideEffect(
					contentLength);

			contentVector.add(contentLengthSideEffect);
		}
	}
}