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
 * Represents &lt;application-exception>.
 */
public interface ApplicationException
{
    /**
     * @return &lt;exception-class>
     */
    String getExceptionClassName();

    /**
     * @return true if &lt;rollback> is specified
     * @see #isRollback
     */
    boolean isSetRollback();

    /**
     * @return &lt;rollback> if specified
     * @see #isSetRollback
     */
    boolean isRollback();

    /**
     * @return true if &lt;inherited> is specified
     * @see #isInherited
     */
    boolean isSetInherited();

    /**
     * @return &lt;inherited> if specified
     * @see #isSetInherited
     */
    boolean isInherited();
}
