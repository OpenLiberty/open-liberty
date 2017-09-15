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
package com.ibm.ws.jsp22.webcontainerext;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jsp.webcontainerext.JspVersion;
import com.ibm.ws.jsp.webcontainerext.JspVersionFactory;

@Component(property = { "service.vendor=IBM" })
public class JspVersionFactoryImpl implements JspVersionFactory {

    private static final JspVersionImpl jv = new JspVersionImpl();

    @Override
    public JspVersion getJspVersion() {
        // TODO Auto-generated method stub
        return jv;
    }

}
