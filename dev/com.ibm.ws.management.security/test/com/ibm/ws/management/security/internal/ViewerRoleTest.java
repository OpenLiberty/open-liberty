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
package com.ibm.ws.management.security.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 *
 */
public class ViewerRoleTest {

    @Test
    public void checkRoleName() {
        ManagementRole role = new ViewerRole();
        assertEquals(ManagementSecurityConstants.VIEWER_ROLE_NAME,
                     role.getRoleName());
    }

}
