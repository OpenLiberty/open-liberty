/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.aroundconstruct;

import javax.ejb.Stateless;

import com.ibm.ws.cdi12.test.utils.Intercepted;

@Stateless
@Intercepted
public class StatelessEjb {
    public StatelessEjb() {} // necessary to be proxyable

    public void doSomething() {}
}
