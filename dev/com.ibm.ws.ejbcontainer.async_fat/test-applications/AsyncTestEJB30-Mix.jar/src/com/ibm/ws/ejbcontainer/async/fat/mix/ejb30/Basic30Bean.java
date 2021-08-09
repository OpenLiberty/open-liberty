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
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb30;

import java.util.concurrent.Future;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

@Stateless
public class Basic30Bean {
    @Asynchronous
    public Future<Void> test() {
        return null;
    }
}