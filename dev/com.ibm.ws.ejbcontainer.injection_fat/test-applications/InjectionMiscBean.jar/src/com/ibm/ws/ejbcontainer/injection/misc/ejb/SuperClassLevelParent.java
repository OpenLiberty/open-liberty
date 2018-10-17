/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.misc.ejb;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@EJB(name = "ejb/ref", beanName = "SuperClassLevelBean")
public class SuperClassLevelParent {
    public void test() throws NamingException {
        ((SuperClassLevelBean) new InitialContext().lookup("java:comp/env/ejb/ref")).test2();
    }

    public void test2() {}
}
