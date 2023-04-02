/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * Home interface for Enterprise Bean: SFBean
 */
public interface SFLocalHome extends EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: SFLa
     */
    public SFLocal create() throws CreateException;

    /**
     */
    public SFLocal create(int intValue) throws CreateException;
}