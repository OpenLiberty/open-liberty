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
package com.ibm.ws.samples.jaxws.sharedlib;

import javax.jws.WebService;

@WebService(name = "CalculatorPortType", targetNamespace = "http://sharedlib.jaxws.samples.ibm.com")
public abstract interface Calculator {
    public abstract int add(int a, int b);
}
