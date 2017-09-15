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

import com.ibm.ws.javaee.dd.common.DisplayName;

/**
 *
 */
public interface SecurityConstraint {

    /**
     * @return &lt;display-name> as a read-only list
     */
    List<DisplayName> getDisplayNames();

    /**
     * @return &lt;web-resource-collection> as a read-only list
     */
    List<WebResourceCollection> getWebResourceCollections();

    /**
     * @return &lt;auth-constraint>, or null if unspecified
     */
    AuthConstraint getAuthConstraint();

    /**
     * @return &lt;user-data-constraint>, or null if unspecified
     */
    UserDataConstraint getUserDataConstraint();

}
