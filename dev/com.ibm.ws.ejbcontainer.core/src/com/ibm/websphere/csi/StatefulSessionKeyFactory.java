/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.csi;

/**
 * A <code>StatefulSessionKeyFactory</code> constructs unique identifiers
 * that may be used as the "primary key" for stateful session beans. <p>
 */
public interface StatefulSessionKeyFactory {
    /**
     * Return a new <code>StatefulSessionKey</code> instance. <p>
     */
    public StatefulSessionKey create(); //87918.8(2)

    /**
     * Return a new <code>StatefulSessionKey</code> instance. <p>
     *
     * @param bytes A byte array to construct the key with
     */
    public StatefulSessionKey create(byte[] bytes);
}
