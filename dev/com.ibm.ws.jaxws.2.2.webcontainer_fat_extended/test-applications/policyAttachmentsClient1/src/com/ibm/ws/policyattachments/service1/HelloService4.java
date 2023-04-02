/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.policyattachments.service1;

import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

@Addressing(enabled = true, required = true)
@WebService(portName = "HelloService4Port",
            serviceName = "HelloService4",
            targetNamespace = "http://service1.policyattachments.ws.ibm.com/")
public class HelloService4 {
    public String helloWithoutPolicy() {
        return "helloWithoutPolicy invoked";
    }

    public String helloWithPolicy() {
        return "helloWithPolicy invoked";
    }

    public String helloWithOptionalPolicy() {
        return "helloWithOptionalPolicy invoked";
    }

    public String helloWithYouWant() {
        return "helloWithYouWant invoked";
    }
}
