/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.management.security.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 *
 */
public class AdministratorRoleTest {

    @Test
    public void checkRoleName() {
        ManagementRole role = new AdministratorRole();
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     role.getRoleName());
    }

}
