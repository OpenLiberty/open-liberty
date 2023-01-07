/*******************************************************************************
 * Copyright (c) 1999, 2012 IBM Corporation and others.
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
package com.ibm.ejs.csi;

import java.security.Identity;

/**
 * This class remains for serialization compatibility only.
 */
@SuppressWarnings("deprecation")
public class NullSecurityCollaborator
{
    // d454046.1 - private class for creating singleton 
    // unauthenticated Identity object. 
    private static class UnauthenticatedIdentity extends java.security.Identity
    {
        private static final long serialVersionUID = -8903829931892409420L;

        private UnauthenticatedIdentity(String identity)
        {
            super(identity);
        }
    }

    // d454046.1 - the singleton unauthenticated Identity object.
    public static final Identity UNAUTHENTICATED = new UnauthenticatedIdentity("UNAUTHENTICATED");
} // SecurityCollaborator
