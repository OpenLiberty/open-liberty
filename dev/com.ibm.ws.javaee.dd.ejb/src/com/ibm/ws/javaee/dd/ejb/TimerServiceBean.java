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

import java.util.List;

/**
 * Represents the group of elements common to beans that support the timer
 * service.
 */
public interface TimerServiceBean
                extends EnterpriseBean
{
    /**
     * @return &lt;timeout-method>, or null if unspecified
     */
    NamedMethod getTimeoutMethod();

    /**
     * @return &lt;timer> as a read-only list
     */
    List<Timer> getTimers();
}
