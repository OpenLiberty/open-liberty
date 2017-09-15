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
package com.ibm.wsspi.webcontainer.extension;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * 
 * ExtensionProcessor classes are responsible for handling requests that filter down to 
 * them by the URL matching process. They could leverage the <b>IServletContext</b>
 * instance that becomes available when the ExtensionProcessor is created, for 
 * advanced functionality. 
 * @ibm-private-in-use
 */
public interface ExtensionProcessor extends RequestProcessor
{
	/**
	 *
	 * The list of patterns that this ExtensionProcessor wants to be associated
	 * with <b>in addition</b> to the patterns specified by the WebExtensionFactory
	 * that created this ExtensionProcessor.
	 * 
	 * @return patternList
	 */
    @SuppressWarnings("unchecked")
	public List getPatternList();

	public IServletWrapper getServletWrapper(ServletRequest req, ServletResponse resp) throws Exception;

	public WebComponentMetaData getMetaData();
}
