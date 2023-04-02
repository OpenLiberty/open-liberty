/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxws.ejbbasic;

import javax.ejb.Stateless;
import javax.jws.WebService;

import com.ibm.ws.jaxws.ejbbasic.view.SayHelloInterface;

/**
 *
 */
@Stateless
@WebService(endpointInterface = "com.ibm.ws.jaxws.ejbbasic.view.SayHelloInterface")
public class SayHello implements SayHelloInterface {

    @Override
    public String sayHello(String userName) {
        return "hello, " + userName;
    }
}
