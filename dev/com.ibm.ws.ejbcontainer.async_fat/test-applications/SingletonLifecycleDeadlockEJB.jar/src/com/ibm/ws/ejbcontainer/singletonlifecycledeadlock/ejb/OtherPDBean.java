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
package com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.ejb;

import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 */
@Stateless
@Asynchronous
public class OtherPDBean {

    @EJB
    SingletonPredestroyBean bean;

    @Asynchronous
    public Future<String> asyncMethod() {
        System.out.println("OtherPDBean: asyncMethod");
        System.out.println("OtherPDBean: asyncMethod: calling Singleton");
        bean.businessMethod();
        System.out.println("OtherPDBean: asyncMethod: after Singleton Call");
        return new AsyncResult<String>("Hello World");
    }
}
