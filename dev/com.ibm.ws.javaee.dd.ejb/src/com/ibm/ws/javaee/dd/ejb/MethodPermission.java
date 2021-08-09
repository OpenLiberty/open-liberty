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
 * Represents &lt;method-permission>.
 */
public interface MethodPermission
                extends Describable
{
    /**
     * @return true if &lt;unchecked> is specified
     */
    boolean isUnchecked();

    /**
     * @return &lt;role-name> as a read-only list (empty if {@link #isUnchecked} is true)
     */
    List<String> getRoleNames();

    /**
     * @return &lt;method> as a read-only list
     */
    List<Method> getMethodElements();
}
