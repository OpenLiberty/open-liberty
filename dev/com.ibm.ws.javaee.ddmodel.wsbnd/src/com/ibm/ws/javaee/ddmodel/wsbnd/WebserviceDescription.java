/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.wsbnd;

public interface WebserviceDescription {
    String WEBSERVICE_DESCRIPTION_NAME_ATTRIBUTE_NAME = "webservice-description-name";
    String WSDL_PUBLISH_LOCATION_ATTRIBUTE_NAME = "wsdl-publish-location";

    String getWebserviceDescriptionName();
    String getWsdlPublishLocation();
}
