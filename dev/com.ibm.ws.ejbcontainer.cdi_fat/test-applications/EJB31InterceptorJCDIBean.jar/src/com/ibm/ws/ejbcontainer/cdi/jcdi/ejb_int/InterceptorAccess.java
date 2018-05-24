/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int;

import java.util.List;

/**
 * Provider access to bean state for interceptors.
 **/
public interface InterceptorAccess {
    /**
     * Returns the current PostConstruct call stack, so an interceptor may
     * add itself to the stack.
     **/
    public List<String> getPostConstructStack();

    /**
     * Returns the current PreDestroy call stack, so an interceptor may
     * add itself to the stack.
     **/
    public List<String> getPreDestroyStack();

    /**
     * Returns the current PostActivate call stack, so an interceptor may
     * add itself to the stack.
     **/
    public List<String> getPostActivateStack();

    /**
     * Returns the current PrePassivate call stack, so an interceptor may
     * add itself to the stack.
     **/
    public List<String> getPrePassivateStack();
}
