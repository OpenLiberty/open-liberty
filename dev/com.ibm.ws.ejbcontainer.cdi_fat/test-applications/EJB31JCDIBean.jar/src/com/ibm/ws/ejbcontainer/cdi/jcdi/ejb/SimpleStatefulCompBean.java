/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import javax.ejb.Init;
import javax.ejb.LocalHome;
import javax.ejb.Stateful;

/**
 * Simple Stateful bean with component view to create and remove
 **/
@Stateful(name = "SimpleStatefulCompOnce")
@LocalHome(SimpleStatefulRemoveHome.class)
public class SimpleStatefulCompBean extends SimpleStatefulBean {
    public SimpleStatefulCompBean() {
        ivEJBName = "SimplestatefulCompOnce";
    }

    @Init
    public void ejbCreate() {
        // just need to have this method
    }

}
