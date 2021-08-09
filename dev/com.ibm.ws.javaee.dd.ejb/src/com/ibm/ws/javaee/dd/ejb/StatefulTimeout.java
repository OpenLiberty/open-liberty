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

import java.util.concurrent.TimeUnit;

/**
 * Represents &lt;stateful-timeout>.
 */
public interface StatefulTimeout
                extends Timeout
{
    /**
     * @return &lt;unit>, or TimeUnit.MINUTES if unspecified and the
     *         implementation does not require XSD validation
     */
    @Override
    TimeUnit getUnitValue();
}
