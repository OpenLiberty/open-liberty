/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webcontainerext.ws;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Factory for creating instances of PrepareJspHelper classes that are
 * specific to the spec version.
 */
public interface PrepareJspHelperFactory {

    public PrepareJspHelper createPrepareJspHelper(AbstractJSPExtensionProcessor s, IServletContext webapp, JspOptions options);
}
