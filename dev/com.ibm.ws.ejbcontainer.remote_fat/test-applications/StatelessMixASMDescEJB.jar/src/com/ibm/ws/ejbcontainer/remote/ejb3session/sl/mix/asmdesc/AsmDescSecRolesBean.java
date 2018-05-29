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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.asmdesc;

import javax.ejb.Local;
import javax.ejb.Stateless;

@Stateless(name = "SecRoleBean")
@Local(AsmDescSecRolesLocal.class)
public class AsmDescSecRolesBean {
    public String secRoleMethod() {
        String result = "FAIL: secRoleMethod() was successfully called. " +
                        "This means that the security-role in XML was not picked up.";

        return result;
    }

    public String unSecureMethod() {
        String result = "PASS: unSecureMethod was successfully called as expeceted.";

        return result;
    }
}