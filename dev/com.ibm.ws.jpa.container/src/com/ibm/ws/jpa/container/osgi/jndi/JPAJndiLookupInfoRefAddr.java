/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.osgi.jndi;

import javax.naming.RefAddr;

/*
 * Naming InfoRefAddr object used for JPA @PersistenceUnit and @PersistenceContext
 * JNDI Reference binding.
 */
public class JPAJndiLookupInfoRefAddr extends RefAddr {
    private static final long serialVersionUID = -3175608835610568086L;

    public static final String Addr_Type = "JPAJndiLookupInfo";

    // Info object
    JPAJndiLookupInfo ivInfo = null;

    /**
     * Constructs a new instance of JPAJndiLookupInfoRefAddr with JPAJndiLookupInfo.
     */
    public JPAJndiLookupInfoRefAddr(JPAJndiLookupInfo info) {
        super(Addr_Type);
        this.ivInfo = info;
    }

    /**
     * @see javax.naming.RefAddr#getContent()
     */
    @Override
    public Object getContent() {
        return ivInfo;
    }
}
