/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import java.io.Serializable;

/**
 * The <code>StatefulSessionKey</code> interface serves to mark all
 * instances of a key instance returned by a
 * <code>StatefulSessionKeyFactory</code>. <p>
 */
public interface StatefulSessionKey
                extends Serializable
{
    /**
     * Return a string containing [a-zA-Z0-9_.-] characters only. Typically,
     * the result is a formatted UUID string.
     */
    public String toString();

    // New BeanId support, provide a method to return the bytes representing
    // this key. Used in improving the performance of BeanIds.
    public byte[] getBytes();
}
