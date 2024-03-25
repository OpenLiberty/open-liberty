/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http:www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi.beanvalidation.stubs;

import javax.jws.WebService;

/**
 * Since we need a seperate implementation for CDI
 * just need to extend and override with a new service name
 * and targetNamespace
 */
@WebService(name = "CDIBeanValidationWebService", targetNamespace = "http://beanvalidation.cdi.jaxws.ws.ibm.com/")
public interface CDIBeanValidationWebService extends BeanValidationWebService {

}
