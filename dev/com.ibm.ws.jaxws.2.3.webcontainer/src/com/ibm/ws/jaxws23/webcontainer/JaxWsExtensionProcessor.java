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
package com.ibm.ws.jaxws23.webcontainer;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.osgi.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class JaxWsExtensionProcessor extends WebExtensionProcessor {

    /**
     * @param context
     */
    public JaxWsExtensionProcessor(IServletContext context) {
        super(context);
    }

    /** {@inheritDoc} */
    @Override
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {}

}
