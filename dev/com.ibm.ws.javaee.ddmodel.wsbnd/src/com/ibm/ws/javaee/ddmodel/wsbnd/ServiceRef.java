/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd;

import java.util.List;
import java.util.Map;

public interface ServiceRef {
    String NAME_ATTRIBUTE_NAME = "name";
    String COMPONENT_NAME_ATTRIBUTE_NAME = "component-name";
    String PORT_ADDRESS_ATTRIBUTE_NAME = "port-address";
    String WSDL_LOCATION_ATTRIBUTE_NAME = "wsdl-location";
    String PORT_ELEMENT_NAME = "port";
    String PROPERTIES_ELEMENT_NAME = "properties";

    String getName();
    String getComponentName();
    String getPortAddress();
    String getWsdlLocation();
    List<Port> getPorts();
    Map<String, String> getProperties();
}
