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

import com.ibm.ws.javaee.dd.common.Describable;

/**
 * Represents &lt;interceptor-binding>.
 */
public interface InterceptorBinding
                extends Describable
{
    /**
     * @return &lt;ejb-name>
     */
    String getEjbName();

    /**
     * @return &lt;interceptor-class> as a read-only list; empty if {@link #getInterceptorOrder} is non-null
     */
    List<String> getInterceptorClassNames();

    /**
     * @return &lt;interceptor-order>, or null if unspecified or {@link #getInterceptorClassNames} is non-empty
     */
    InterceptorOrder getInterceptorOrder();

    /**
     * @return true if &lt;exclude-default-interceptors> is specified
     * @see #isExcludeDefaultInterceptors
     */
    boolean isSetExcludeDefaultInterceptors();

    /**
     * @return &lt;exclude-default-interceptors> if specified
     * @see #isSetExcludeDefaultInterceptors
     */
    boolean isExcludeDefaultInterceptors();

    /**
     * @return true if &lt;exclude-class-interceptors> is specified
     * @see #isExcludeClassInterceptors
     */
    boolean isSetExcludeClassInterceptors();

    /**
     * @return &lt;exclude-class-interceptors> if specified
     * @see #isSetExcludeClassInterceptors
     */
    boolean isExcludeClassInterceptors();

    /**
     * @return &lt;method>, or null if unspecified
     */
    NamedMethod getMethod();
}
