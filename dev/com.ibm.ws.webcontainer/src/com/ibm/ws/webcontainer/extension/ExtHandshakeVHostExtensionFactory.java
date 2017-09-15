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
package com.ibm.ws.webcontainer.extension;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

@SuppressWarnings("unchecked")
public class ExtHandshakeVHostExtensionFactory implements ExtensionFactory {
    private ArrayList patList = new ArrayList();
    
    public ExtHandshakeVHostExtensionFactory() {
        patList.add("_WS_EH*");
    }
    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.extension.ExtensionFactory#createExtensionProcessor(com.ibm.wsspi.webcontainer.servlet.IServletContext)
     */
    public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception {
        return new ExtHandshakeVHostExtensionProcessor();
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.webcontainer.extension.ExtensionFactory#getPatternList()
     */
    public List getPatternList() {
        return patList;
    }

}
