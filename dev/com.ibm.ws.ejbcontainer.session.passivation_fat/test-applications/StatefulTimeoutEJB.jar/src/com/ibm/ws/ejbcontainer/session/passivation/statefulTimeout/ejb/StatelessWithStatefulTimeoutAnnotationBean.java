/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb;

import java.util.concurrent.TimeUnit;

import javax.ejb.StatefulTimeout;
import javax.ejb.Stateless;

@Stateless
@StatefulTimeout(value = 5, unit = TimeUnit.MINUTES)
public class StatelessWithStatefulTimeoutAnnotationBean {
    public long getInvocationTime() {
        return System.currentTimeMillis();
    }
}