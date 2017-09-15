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

/**
 * Represents &lt;schedule> in &lt;timer>.
 */
public interface TimerSchedule
{
    /**
     * @return &lt;second>, or null if unspecified
     */
    String getSecond();

    /**
     * @return &lt;minute>, or null if unspecified
     */
    String getMinute();

    /**
     * @return &lt;hour>, or null if unspecified
     */
    String getHour();

    /**
     * @return &lt;day-of-month>, or null if unspecified
     */
    String getDayOfMonth();

    /**
     * @return &lt;month>, or null if unspecified
     */
    String getMonth();

    /**
     * @return &lt;day-of-week>, or null if unspecified
     */
    String getDayOfWeek();

    /**
     * @return &lt;year>, or null if unspecified
     */
    String getYear();
}
