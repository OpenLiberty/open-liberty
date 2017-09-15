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
package com.ibm.ws.javaee.ddmodel.wsbnd;

import java.util.Map;

public interface WebserviceEndpoint {

    public static String PORT_COMPONENT_NAME_ATTRIBUTE_NAME = "port-component-name";

    public static String ADDRESS_ATTRIBUTE_NAME = "address";

    public static String PROPERTIES_ELEMENT_NAME = "properties";

    /**
     * @return port-component-name="..." attribute value
     */
    public String getPortComponentName();

    /**
     * @return address="..." attribute value
     */
    public String getAddress();

    /**
     * @return all attributes defined in the properties element
     */
    public Map<String, String> getProperties();
}
