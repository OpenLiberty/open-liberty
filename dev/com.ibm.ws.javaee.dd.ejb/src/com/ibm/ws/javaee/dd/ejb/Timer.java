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

import com.ibm.ws.javaee.dd.common.Describable;

/**
 * Represents &lt;timer>.
 */
public interface Timer
                extends Describable
{
    /**
     * @return &lt;schedule>
     */
    TimerSchedule getSchedule();

    /**
     * @return &lt;start>, or null if unspecified
     */
    String getStart();

    /**
     * @return &lt;end>, or null if unspecified
     */
    String getEnd();

    /**
     * @return &lt;timeout-method>
     */
    NamedMethod getTimeoutMethod();

    /**
     * @return true if &lt;persistent> is specified
     * @see #isPersistent
     */
    boolean isSetPersistent();

    /**
     * @return &lt;persistent> if specified
     * @see #isSetPersistent
     */
    boolean isPersistent();

    /**
     * @return &lt;timezone>, or null if unspecified
     */
    String getTimezone();

    /**
     * @return &lt;info>, or null if unspecified
     */
    String getInfo();
}
