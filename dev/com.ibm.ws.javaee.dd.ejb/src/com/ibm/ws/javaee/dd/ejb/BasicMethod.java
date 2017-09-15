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

/**
 * Represents common elements for referencing a method by name and parameters.
 */
public interface BasicMethod
{
    /**
     * @return &lt;method-name>
     */
    String getMethodName();

    /**
     * @return &lt;method-params>, or null if unspecified
     */
    List<String> getMethodParamList();
}
