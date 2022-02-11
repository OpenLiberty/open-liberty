/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

import java.util.List;

/**
 * Common type for environment references which contain
 * a context reference.
 * 
 * Added for Jakarta EE 10.
 */
public interface JNDIContextServiceRef extends JNDIEnvironmentRef {
    /**
     * @return &lt;description&gt;, or null if unspecified
     */
    Description getDescription();
    
    /**
     * @return &lt;context-service-ref&gt;, or null if unspecified
     */
    String getContextServiceRef();

    /**
     * @return &lt;property&gt; elements as a read-only list
     */
    List<Property> getProperties();    
}
