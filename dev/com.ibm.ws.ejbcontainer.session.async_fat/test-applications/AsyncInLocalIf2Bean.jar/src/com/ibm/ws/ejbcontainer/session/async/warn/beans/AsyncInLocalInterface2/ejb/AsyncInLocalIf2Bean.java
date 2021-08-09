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
package com.ibm.ws.ejbcontainer.session.async.warn.beans.AsyncInLocalInterface2.ejb;

import javax.ejb.Asynchronous;

/**
 * Bean implementation class for Enterprise Bean: AsyncNotInLocalIf2
 **/

public class AsyncInLocalIf2Bean {
    @Asynchronous
    public void test1() {
        return;
    }

    public void test2() {
        return;
    }

    @Asynchronous
    public void test3() {
        return;
    }
}
