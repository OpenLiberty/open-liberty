/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.async.warn.shared;

import javax.ejb.Asynchronous;
import javax.ejb.Local;

@Asynchronous
@Local
public interface AsyncInLocalIf {
    public void test1();

    public void test2();

    public void test3();
}
