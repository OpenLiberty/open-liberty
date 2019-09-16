/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.app_exception.ann.ejb;

public interface ThrownExLocalInterface {
    public ResultObject test(int i);

    public ResultObject test2(int i);

    public ResultObject test3(int i);

    public void throwException(int i) throws ThrownException;

    public void throwAppExceptionInheritFalse(int i) throws ThrownAppExInheritFalse;

    public void throwAppExceptionInheritTrue(int i) throws ThrownAppExInheritTrue;
}
