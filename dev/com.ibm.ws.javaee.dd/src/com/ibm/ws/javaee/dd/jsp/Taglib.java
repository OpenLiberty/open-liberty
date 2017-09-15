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
package com.ibm.ws.javaee.dd.jsp;

/**
 * Represents the taglibType type from the jsp XSD.
 */
public interface Taglib {

    /**
     * @return &lt;taglib-uri>
     */
    String getTaglibURI();

    /**
     * @return &lt;taglib-location>
     */
    String getTaglibLocation();

}
