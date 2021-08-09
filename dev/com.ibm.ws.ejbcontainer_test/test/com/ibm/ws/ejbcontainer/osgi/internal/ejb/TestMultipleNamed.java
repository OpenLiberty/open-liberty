/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejb;

import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

@Stateless(name = TestMultipleNamed.STATELESS_NAME)
@Stateful(name = TestMultipleNamed.STATEFUL_NAME)
@Singleton(name = TestMultipleNamed.SINGLETON_NAME)
@MessageDriven(name = TestMultipleNamed.MESSAGE_DRIVEN_NAME)
public class TestMultipleNamed {
    public static final String STATELESS_NAME = "MultipleNamedStateless";
    public static final String STATEFUL_NAME = "MultipleNamedStateful";
    public static final String SINGLETON_NAME = "MultipleNamedSingleton";
    public static final String MESSAGE_DRIVEN_NAME = "MultipleNamedMessageDriven";
}
