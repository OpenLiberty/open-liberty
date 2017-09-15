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

public interface WebserviceDescription {

    public static String WEBSERVICE_DESCRIPTION_NAME_ATTRIBUTE_NAME = "webservice-description-name";

    public static String WSDL_PUBLISH_LOCATION_ATTRIBUTE_NAME = "wsdl-publish-location";

    /**
     * @return webservice-description-name="..." attribute value
     */
    public String getWebserviceDescriptionName();

    /**
     * @return wsdl-publish-location="..." attribute value
     */
    public String getWsdlPublishLocation();
}
