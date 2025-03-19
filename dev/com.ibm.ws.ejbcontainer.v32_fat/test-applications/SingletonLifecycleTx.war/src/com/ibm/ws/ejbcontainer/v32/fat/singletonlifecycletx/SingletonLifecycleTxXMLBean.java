/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.singletonlifecycletx;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;

import com.ibm.wsspi.uow.UOWManager;

@Singleton
public class SingletonLifecycleTxXMLBean {
    @Resource(lookup = "java:comp/websphere/UOWManager")
    private UOWManager uowManager;
    private SingletonLifecycleTxUOWState postConstructUOWState;

    @PostConstruct
    protected void postConstruct() {
        postConstructUOWState = new SingletonLifecycleTxUOWState(uowManager);
    }

    public SingletonLifecycleTxUOWState getPostConstructUOWState() {
        return postConstructUOWState;
    }
}
