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

import com.ibm.ws.javaee.dd.common.Describable;

/**
 *
 */
public interface WebResourceCollection
                extends Describable {

    /**
     * @return &lt;web-resource-name>
     */
    String getWebResourceName();

    /**
     * @return &lt;url-pattern> as a read-only list
     */
    List<String> getURLPatterns();

    /**
     * @return &lt;http-method> as a read-only list
     */
    List<String> getHTTPMethods();

    /**
     * @return &lt;http-method-omission> as a read-only list
     */
    List<String> getHTTPMethodOmissions();

}
