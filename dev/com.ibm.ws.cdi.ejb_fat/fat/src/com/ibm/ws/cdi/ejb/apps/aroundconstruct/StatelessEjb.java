/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.apps.aroundconstruct;

import javax.ejb.Stateless;

import com.ibm.ws.cdi.ejb.utils.StatelessIntercepted;

@Stateless
@StatelessIntercepted
public class StatelessEjb {
    public StatelessEjb() {} // necessary to be proxyable

    public void doSomething() {}
}
