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

/**
 *
 */
public interface FilterMapping {

    static enum DispatcherEnum {
        // lexical value must be (FORWARD|INCLUDE|REQUEST|ASYNC|ERROR)
        FORWARD,
        INCLUDE,
        REQUEST,
        ASYNC,
        ERROR;
    }

    /**
     * @return &lt;filter-name>
     */
    String getFilterName();

    /**
     * @return &lt;url-pattern>, or null if not specified
     */
    String getURLPattern();

    /**
     * @return &lt;servlet-name>, or null if not specified
     */
    String getServletName();

    /**
     * @return &lt;dispatcher> as a read-only list
     */
    List<DispatcherEnum> getDispatcherValues();

}
