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
package com.ibm.ws.ejbcontainer.exception2x.ejb;

import javax.ejb.EJBLocalObject;

/**
 * This is an Enterprise Java Bean Local Interface
 */
public interface TestExLItf extends EJBLocalObject {
    public int addMore(int i);

    public void throwRuntimeException(String s);

    public void throwTransactionRequiredException(String s);

    public void throwTransactionRolledbackException(String s);

    public void throwInvalidTransactionException(String s);

    public void throwAccessException(String s);

    public void throwActivityRequiredException(String s);

    public void throwInvalidActivityException(String s);

    public void throwActivityCompletedException(String s);

    public void throwNoSuchObjectException(String s);
}