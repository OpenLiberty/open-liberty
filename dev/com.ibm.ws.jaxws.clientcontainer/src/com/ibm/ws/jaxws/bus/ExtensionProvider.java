/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.bus;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.Extension;

/**
 * The interface provides Liberty extension to add CXF extension.
 * 
 * In the OSGi environment, CXF will use bundle listener to monitor /META-INF/cxf/bus-extensions.txt files for each bundle,
 * and add those extensions in the ExtensionRegistry, which are added in each created bus.
 * 
 * The typical scenario for this provider is that, it could be used to override any existing extension provided by CXF.
 * 
 * While implementing the interface, getExtension method will be invoked for each created bus
 * 
 */
public interface ExtensionProvider {

    /**
     * Return the extension provided by current provider.
     * 
     * @param bus The instance which the Extension will be added
     * @return return the created Extension instance or null if the current bus instance should be skipped
     */
    public Extension getExtension(Bus bus);
}
