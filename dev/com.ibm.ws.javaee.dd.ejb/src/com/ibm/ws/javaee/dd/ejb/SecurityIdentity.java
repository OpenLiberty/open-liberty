/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.ejb;

import com.ibm.ws.javaee.dd.common.Describable;
import com.ibm.ws.javaee.dd.common.RunAs;

/**
 * Represents &lt;security-identity>.
 */
public interface SecurityIdentity
                extends Describable
{
    /**
     * @return true if &lt;use-caller-identity> is specified; false if {@link #getRunAs} returns non-null
     */
    boolean isUseCallerIdentity();

    /**
     * @return &lt;run-as>, or null if unspecified or {@link #isUseCallerIdentity} returns true
     */
    RunAs getRunAs();
}
