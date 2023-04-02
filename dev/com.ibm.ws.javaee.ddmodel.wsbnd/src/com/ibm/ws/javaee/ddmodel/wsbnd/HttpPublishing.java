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

public interface HttpPublishing {
    String CONTEXT_ROOT_ATTRIBUTE_NAME = "context-root";
    String WEBSERVICE_SECURITY_ELEMENT_NAME = "webservice-security";

    String getContextRoot();
    WebserviceSecurity getWebserviceSecurity();
}
