/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.server.deprecated;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.osgi.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/** 
 *
 */
@Deprecated
public class JaxRsExtensionProcessor extends WebExtensionProcessor {

    /**
     * @param context
     */
    public JaxRsExtensionProcessor(IServletContext context) {
        super(context);
    }

    @Override
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */

}
