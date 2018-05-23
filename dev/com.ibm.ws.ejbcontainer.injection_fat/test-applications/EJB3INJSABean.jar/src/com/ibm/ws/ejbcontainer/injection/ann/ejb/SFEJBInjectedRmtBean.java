/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.ann.ejb;

import javax.ejb.Remote;
import javax.ejb.Stateful;

@Stateful(name = "SFEJBInjectedRmt")
@Remote(SimpleSFRemote.class)
public class SFEJBInjectedRmtBean {
    public String getString() {
        return "success";
    }
}
