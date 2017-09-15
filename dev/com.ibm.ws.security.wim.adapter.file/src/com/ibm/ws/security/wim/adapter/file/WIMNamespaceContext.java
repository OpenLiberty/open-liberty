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
package com.ibm.ws.security.wim.adapter.file;

import java.util.Iterator;
import javax.xml.*;
import javax.xml.namespace.NamespaceContext;

public class WIMNamespaceContext implements NamespaceContext {

    public String getNamespaceURI(String prefix) {
        if (prefix == null)
            throw new NullPointerException("Null prefix");
        else if ("wim".equals(prefix))
            return "http://www.ibm.com/websphere/wim";
        else if ("xml".equals(prefix))
            return XMLConstants.XML_NS_URI;
        else if ("xsi".equals(prefix))
            return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
        else if ("sdo".equals(prefix))
            return "commonj.sdo";
        return XMLConstants.NULL_NS_URI;
    }

    // This method isn't necessary for XPath processing.
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    // This method isn't necessary for XPath processing either.
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}