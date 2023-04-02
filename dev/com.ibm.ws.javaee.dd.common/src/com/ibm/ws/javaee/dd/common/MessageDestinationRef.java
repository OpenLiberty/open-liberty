/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

/**
 * Represents &lt;message-destination-ref>.
 */
public interface MessageDestinationRef
                extends ResourceGroup, Describable
{
    /**
     * Represents an unspecified value for {@link #getUsageValue}.
     */
    int USAGE_UNSPECIFIED = -1;

    /**
     * Represents "Consumes" for {@link #getUsageValue}.
     * 
     * @see org.eclipse.jst.j2ee.common.MessageDestinationUsageType#CONSUMES
     */
    int USAGE_CONSUMES = 0;

    /**
     * Represents "Produces" for {@link #getUsageValue}.
     * 
     * @see org.eclipse.jst.j2ee.common.MessageDestinationUsageType#PRODUCES
     */
    int USAGE_PRODUCES = 1;

    /**
     * Represents "ConsumesProduces" for {@link #getUsageValue}.
     * 
     * @see org.eclipse.jst.j2ee.common.MessageDestinationUsageType#CONSUMES_PRODUCES
     */
    int USAGE_CONSUMES_PRODUCES = 2;

    /**
     * @return &lt;message-destination-type>, or null if unspecified
     */
    String getType();

    /**
     * @return &lt;message-destination-type>
     *         <ul>
     *         <li>{@link #USAGE_UNSPECIFIED} if unspecified
     *         <li>{@link #USAGE_CONSUMES} - Consumes
     *         <li>{@link #USAGE_PRODUCES} - Produces
     *         <li>{@link #USAGE_CONSUMES_PRODUCES} - ConsumesProduces
     */
    int getUsageValue();

    /**
     * @return &lt;message-destination-link>, or null if unspecified
     */
    String getLink();
}
