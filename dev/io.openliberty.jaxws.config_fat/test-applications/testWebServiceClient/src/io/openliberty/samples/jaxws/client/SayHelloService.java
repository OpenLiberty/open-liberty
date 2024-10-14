/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.samples.jaxws.client;

import javax.jws.WebService;

@WebService(name = "SayHello", targetNamespace = "http://jaxws.samples.openliberty.io/")
public interface SayHelloService {
    public String sayHello(String name);
}
