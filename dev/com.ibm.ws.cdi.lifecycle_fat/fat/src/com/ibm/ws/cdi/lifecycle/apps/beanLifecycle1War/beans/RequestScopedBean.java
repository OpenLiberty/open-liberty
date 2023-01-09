/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.cdi.lifecycle.apps.beanLifecycle1War.beans;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.EventMetadata;

import com.ibm.ws.cdi.lifecycle.apps.beanLifecycle1War.resources.GlobalState;

/**
 *
 */
@RequestScoped
public class RequestScopedBean {

    public void doSomething() {
        int i = 1;
        i = 1 + 1;
    }

    public static void onStart(@Observes @Initialized(RequestScoped.class) Object e, EventMetadata em) {
        GlobalState.recordRequestStart();

    }

    public static void onStop(@Observes @Destroyed(RequestScoped.class) Object e) {

        GlobalState.recordRequestStop();

    }

}
