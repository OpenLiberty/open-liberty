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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 * Viewer role binding: {@code
 * <viewer-role>
 *      <user>userName</user>
 *      <user-access-id>user:realm/utle</user-access-id>  or <user-access-id>realm/utle</user-access-id>                 //realm is required
 *      <group>groupName</group>
 *      <group-access-id>group:realm/group1</group-access-id> or <group-access-id>realm/group1</group-access-id>        //realm is required
 * </viewer-role> }
 */
public class ViewerRole extends AbstractManagementRole {
    static final TraceComponent tc = Tr.register(ViewerRole.class);

    public ViewerRole() {
        super(tc);
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public String getRoleName() {
        return ManagementSecurityConstants.VIEWER_ROLE_NAME;
    }
}
