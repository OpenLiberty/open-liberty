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
package com.ibm.ws.transport.iiop.spi;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Allows replacing remote objects prior to being written by
 * Util.writeRemoteObject.
 */
public interface RemoteObjectReplacer {
    /**
     * Replaces remove objects prior to being written by Util.writeRemoteObject.
     * If an object is not replaced and is not already exported, it will be
     * automatically exported as RMI.
     * <p>
     * Implementations are strongly encouraged to annotate the parameter
     * and return value with {@link Sensitive} to avoid tracing user data.
     *
     * @param object the object being written
     * @return a remote object reference, or null if no replacement is needed
     */
    @Sensitive
    Object replaceRemoteObject(@Sensitive Object object);
}
