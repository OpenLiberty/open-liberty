/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.session.impl;

import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.util.ArrayEnumeration;
import com.ibm.wsspi.session.ISession;

import io.openliberty.session.impl.http.HttpSessionImpl60;
import jakarta.servlet.ServletContext;

/*
 * The WAS Http Session adaptation.  Extends the core HttpSessionImpl and adds WAS-specific function
 *
 * Since Servlet 6.0
 */
public class SessionData60 extends HttpSessionImpl60 {

    private static final long serialVersionUID = -76305717244905946L;

    public SessionData60(ISession session, SessionContext sessCtx, ServletContext servCtx) {
        super(session, sessCtx, servCtx);

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, "SessionData60" + " Constructor");
        }
    }

    /*
     * Servlet 6 Update:
     * - Keep here to support getValueNames(); looping of move to AbstractSessionData
     *
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    @Override
    public Enumeration getAttributeNames() {
        Enumeration attrNameEnum;
        if (!_hasSecurityInfo) {
            attrNameEnum = super.getAttributeNames();
        } else {
            String[] attrNames = getValueNames();
            attrNameEnum = new ArrayEnumeration(attrNames);
        }
        return attrNameEnum;
    }

    private String[] getValueNames() {
        Enumeration enumeration = super.getAttributeNames();
        Vector valueNames = new Vector();
        String name = null;
        boolean securityPropFound = false;
        while (enumeration.hasMoreElements()) {
            name = (String) enumeration.nextElement();
            if (!name.equals(SECURITY_PROP_NAME)) {
                valueNames.add(name);
            } else {
                securityPropFound = true;
            }
        }
        _hasSecurityInfo = securityPropFound;
        String[] names = new String[valueNames.size()];
        return (String[]) valueNames.toArray(names);
    }
}
