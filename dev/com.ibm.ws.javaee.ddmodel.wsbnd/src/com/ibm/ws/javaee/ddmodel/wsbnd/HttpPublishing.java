/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd;

public interface HttpPublishing {
    String CONTEXT_ROOT_ATTRIBUTE_NAME = "context-root";
    String WEBSERVICE_SECURITY_ELEMENT_NAME = "webservice-security";

    String getContextRoot();
    WebserviceSecurity getWebserviceSecurity();
}
