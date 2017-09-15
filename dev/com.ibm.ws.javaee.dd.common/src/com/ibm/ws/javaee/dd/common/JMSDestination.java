/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 * Represents &lt;jms-destination>.
 */
public interface JMSDestination extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;interface-name>
     */
    String getInterfaceNameValue();

    /**
     * @return &lt;class-name>, or null if unspecified
     */
    String getClassNameValue();

    /**
     * @return &lt;resource-adapter>, or null if unspecified
     */
    String getResourceAdapter();

    /**
     * @return &lt;destination-name>, or null if unspecified
     */
    String getDestinationName();

    /**
     * @return &lt;property> as a read-only list
     */
    List<Property> getProperties();
}
