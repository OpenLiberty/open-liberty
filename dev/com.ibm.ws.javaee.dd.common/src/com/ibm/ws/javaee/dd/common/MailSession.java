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
 * Represents &lt;mail-session>.
 */
public interface MailSession extends JNDIEnvironmentRef, Describable {
    /**
     * @return &lt;store-protocol>, or null if unspecified
     */
    String getStoreProtocol();

    /**
     * @return &lt;store-protocol-class>, or null if unspecified
     */
    String getStoreProtocolClassName();

    /**
     * @return &lt;transport-protocol>, or null if unspecified
     */
    String getTransportProtocol();

    /**
     * @return &lt;transport-protocol-class>, or null if unspecified
     */
    String getTransportProtocolClassName();

    /**
     * @return &lt;host>, or null if unspecified
     */
    String getHost();

    /**
     * @return &lt;user>, or null if unspecified
     */
    String getUser();

    /**
     * @return &lt;password>, or null if unspecified
     */
    String getPassword();

    /**
     * @return &lt;from>, or null if unspecified
     */
    String getFrom();

    /**
     * @return &lt;property> as a read-only list
     */
    List<Property> getProperties();
}
