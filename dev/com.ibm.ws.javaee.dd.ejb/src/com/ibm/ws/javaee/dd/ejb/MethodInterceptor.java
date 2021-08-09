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

import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefsGroup;

/**
 * Represents the group of &lt;post-construct>, &lt;pre-destroy,
 * &lt;around-invoke> and &lt;around-timeout> elements.
 */
public interface MethodInterceptor
                extends JNDIEnvironmentRefsGroup
{
    /**
     * @return &lt;around-invoke> as a read-only list
     */
    List<InterceptorCallback> getAroundInvoke();

    /**
     * @return &lt;around-timeout> as a read-only list
     */
    List<InterceptorCallback> getAroundTimeoutMethods();
}
