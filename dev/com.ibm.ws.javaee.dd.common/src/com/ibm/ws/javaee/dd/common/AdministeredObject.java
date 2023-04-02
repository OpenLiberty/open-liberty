/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

import java.util.List;

/**
 * Represents &lt;administered-object>.
 */
public interface AdministeredObject extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;interface-name>, or null if unspecified
     */
    String getInterfaceNameValue();

    /**
     * @return &lt;class-name>
     */
    String getClassNameValue();

    /**
     * @return &lt;resource-adapter>
     */
    String getResourceAdapter();

    /**
     * @return &lt;property> as a read-only list
     */
    List<Property> getProperties();
}
