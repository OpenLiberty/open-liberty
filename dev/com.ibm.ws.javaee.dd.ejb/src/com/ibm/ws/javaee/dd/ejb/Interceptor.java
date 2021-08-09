/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.common.Describable;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;

/**
 * Represents &lt;interceptor>.
 */
public interface Interceptor extends Describable, SessionInterceptor {
    /**
     * @return &lt;interceptor-class>
     */
    String getInterceptorClassName();

    /**
     * @return &lt;around-construct> as a read-only list
     */
    List<LifecycleCallback> getAroundConstruct();
}
