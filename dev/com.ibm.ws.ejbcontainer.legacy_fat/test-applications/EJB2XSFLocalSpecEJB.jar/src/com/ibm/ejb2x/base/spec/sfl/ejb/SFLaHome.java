/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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

package com.ibm.ejb2x.base.spec.sfl.ejb;

/**
 * Home interface for Enterprise Bean: SFLa
 */
public interface SFLaHome extends javax.ejb.EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: SFLa
     */
    public SFLa create() throws javax.ejb.CreateException;

    /**
    */
    public SFLa create(boolean booleanValue, byte byteValue, char charValue, short shortValue, int intValue, long longValue, float floatValue, double doubleValue,
                       String stringValue) throws javax.ejb.CreateException;
}
