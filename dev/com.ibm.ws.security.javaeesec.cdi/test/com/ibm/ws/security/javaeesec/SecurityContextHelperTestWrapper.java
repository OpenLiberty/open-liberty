/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import org.jmock.Mockery;

import com.ibm.ws.security.intfc.SubjectManagerService;
import com.ibm.ws.security.javaeesec.cdi.extensions.SecurityContextHelper;

/**
 * Expose CDIHelper's protected methods for unit testing.
 */
public class SecurityContextHelperTestWrapper {

    SecurityContextHelper securityContextHelper;
    private final Mockery mockery;

    public SecurityContextHelperTestWrapper(Mockery mockery) {
        securityContextHelper = new SecurityContextHelper();
        this.mockery = mockery;

    }

    public void setSubjectManagerService(final SubjectManagerService smService) {
        securityContextHelper.setSubjectManagerService(smService);
    }

    public void unsetSubjectManagerService(final SubjectManagerService smService) {
        securityContextHelper.unsetSubjectManagerService(smService);
    }

}
