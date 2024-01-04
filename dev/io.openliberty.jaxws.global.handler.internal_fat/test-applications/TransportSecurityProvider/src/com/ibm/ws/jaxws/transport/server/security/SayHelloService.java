/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxws.transport.server.security;

import javax.jws.WebService;

@WebService(name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/should_not_used_interface/")
public interface SayHelloService {
    String sayHello(String name);
}
