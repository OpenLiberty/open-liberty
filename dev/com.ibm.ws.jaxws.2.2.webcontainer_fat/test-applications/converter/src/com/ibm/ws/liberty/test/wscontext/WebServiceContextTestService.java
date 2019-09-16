/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.liberty.test.wscontext;

import javax.jws.WebService;

@WebService(name = "WebServiceContextTestServicePortType", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com")
public interface WebServiceContextTestService {
    boolean isInjectionInstanceNull();

    boolean isDefaultJndiLookupInstanceNull();

    boolean isSelfDefinedJndiLookupInstanceNull();

    boolean isMessageContextNull();

    boolean isServletContextNull();

    String getServletContextParameter();
}
