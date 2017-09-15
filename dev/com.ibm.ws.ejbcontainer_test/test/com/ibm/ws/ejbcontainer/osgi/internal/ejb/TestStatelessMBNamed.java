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

import javax.annotation.ManagedBean;
import javax.ejb.Stateless;

@Stateless
@ManagedBean(TestStatelessMBNamed.NAME)
public class TestStatelessMBNamed {
    public static final String NAME = "StatelessMBNamed";
}
