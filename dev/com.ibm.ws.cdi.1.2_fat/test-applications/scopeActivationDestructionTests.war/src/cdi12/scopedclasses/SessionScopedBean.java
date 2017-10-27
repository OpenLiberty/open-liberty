/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.scopedclasses;

import java.io.Serializable;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;

import cdi12.resources.GlobalState;

/**
 *
 */
@SessionScoped
public class SessionScopedBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    public void doSomething() {
        int i = 1;
        i = 1 + 1;
    }

    public static void onStart(@Observes @Initialized(SessionScoped.class) Object e) {

        GlobalState.recordSessionStart();
    }

    public static void onStop(@Observes @Destroyed(SessionScoped.class) Object e) {
        GlobalState.recordSessionStop();

    }

}
