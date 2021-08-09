/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.config.admin.internal.WSConfigAdminActivator;
import com.ibm.ws.config.xml.internal.WSConfigXMLActivator;

/**
 * 
 */
public class WSConfigActivator implements BundleActivator {

    WSConfigAdminActivator adminActivator = new WSConfigAdminActivator();
    WSConfigXMLActivator xmlActivator = new WSConfigXMLActivator();

    @Override
    public void start(BundleContext bc) {
        adminActivator.start(bc);
        xmlActivator.start(bc);
    }

    @Override
    public void stop(BundleContext bc) {
        xmlActivator.stop(bc);
        adminActivator.stop(bc);
    }
}
