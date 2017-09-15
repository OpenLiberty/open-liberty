/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.web.common;

import java.util.List;

import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.ParamValue;

/**
 *
 */
public interface Filter
                extends DescriptionGroup {

    /**
     * @return &lt;filter-name>
     */
    String getFilterName();

    /**
     * @return &lt;filter-class>, or null if unspecified
     */
    String getFilterClass();

    /**
     * @return true if &lt;async-supported> is specified
     * @see #isAsyncSupported
     */
    boolean isSetAsyncSupported();

    /**
     * @return &lt;async-supported> if specified
     * @see #isSetAsyncSupported
     */
    boolean isAsyncSupported();

    /**
     * @return &lt;init-param> as a read-only list
     */
    List<ParamValue> getInitParams();

}
