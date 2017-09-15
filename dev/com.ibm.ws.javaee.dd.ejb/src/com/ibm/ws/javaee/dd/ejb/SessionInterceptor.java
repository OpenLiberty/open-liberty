/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejb;

import java.util.List;

import com.ibm.ws.javaee.dd.common.LifecycleCallback;

/**
 * Represents the group of methods to intercept a session bean.
 */
public interface SessionInterceptor
                extends MethodInterceptor
{
    /**
     * @return &lt;post-activate> as a read-only list
     */
    List<LifecycleCallback> getPostActivate();

    /**
     * @return &lt;pre-passivate> as a read-only list
     */
    List<LifecycleCallback> getPrePassivate();
}
