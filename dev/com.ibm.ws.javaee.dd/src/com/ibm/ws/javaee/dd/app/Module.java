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
package com.ibm.ws.javaee.dd.app;

/**
 *
 */
public interface Module {

    /**
     * Represents &lt;connector> for {@link #getModuleType}.
     */
    int TYPE_CONNECTOR = 0;

    /**
     * Represents &lt;ejb> for {@link #getModuleType}.
     */
    int TYPE_EJB = 1;

    /**
     * Represents &lt;java> for {@link #getModuleType}.
     */
    int TYPE_JAVA = 2;

    /**
     * Represents &lt;web> for {@link #getModuleType}.
     */
    int TYPE_WEB = 3;

    /**
     * @return the type of module
     *         <ul>
     *         <li>{@link #TYPE_CONNECTOR} - connector
     *         <li>{@link #TYPE_EJB} - ejb
     *         <li>{@link #TYPE_JAVA} - java
     *         <li>{@link #TYPE_WEB} - web
     *         </ul>
     */
    int getModuleType();

    /**
     * @return the path of module, or &lt;web-uri> when TYPE_WEB.
     */
    String getModulePath();

    /**
     * @return &lt;web>&lt;context-root> when TYPE_WEB.
     */
    String getContextRoot();

    /**
     * @return &lt;alt-dd>, or null if unspecified
     */
    String getAltDD();
}
