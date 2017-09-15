/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.j2c.SecurityHelper;

/**
 *
 * <P> The DefaultSecurityHelper is used in the case where the given
 * platform doesn't need to any special security processing with
 * respect to the current Subject. The DefaultSecurityHelper
 * implements each method as a NO-OP.
 *
 * <P>Scope : EJB server and Web server
 *
 * <P>Object model : 1 instance per ManagedConnectionFactory
 *
 */

public class DefaultSecurityHelper implements SecurityHelper {
    private static final TraceComponent tc = Tr.register(DefaultSecurityHelper.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);

    /**
     * The finalizeSubject method is used to set what the final Subject
     * will be for processing.
     *
     * The primary intent of this method is to allow the Subject to be
     * defaulted.
     *
     * @param Subject subject
     * @param ConnectionRequestInfo reqInfo
     * @param cmConfigData
     * @return Subject
     * @exception ResourceException
     */
    @Override
    public Subject finalizeSubject(Subject subject,
                                   ConnectionRequestInfo reqInfo,
                                   CMConfigData cmConfigData) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "finalizeSubject");
        }

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "finalizeSubject");
        }

        return subject; // Pass back unchanged Subject

    }

    /**
     * The beforeCreateManageConnection() method is used to allow
     * special security processing to be performed prior to calling
     * a resource adapter to get a connection.
     *
     * @param Subject subject
     * @param ConnectionRequestInfo reqInfo
     * @return Object if non-null, the user identity defined by the
     *         Subject was pushed to thread. The Object in
     *         this case needs to be passed as input to
     *         afterGettingConnection method processing and
     *         will be used to restore the thread identity
     *         back to what it was.
     * @exception ResourceException
     */
    @Override
    public Object beforeGettingConnection(Subject subject,
                                          ConnectionRequestInfo reqInfo) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "beforeGettingConnection");
        }

        Object retObject = null;

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "beforeGettingConnection");
        }

        return retObject;

    }

    /**
     * The afterGettingConnection() method is used to allow
     * special security processing to be performed after calling
     * a resource adapter to get a connection.
     *
     * @param Subject subject
     * @param ConnectionRequestInfo reqInfo
     * @param Object credentialToken
     * @return void
     * @exception ResourceException
     */
    @Override
    public void afterGettingConnection(Subject subject,
                                       ConnectionRequestInfo reqInfo,
                                       Object credentialToken) throws ResourceException {

        final boolean isTracingEnabled = TraceComponent.isAnyTracingEnabled();

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "afterGettingConnection");
        }

        // Since the beforeGettingConnection never pushes the Subject
        // to the thread, ignore the input credToken Object and
        // simply return without doing anything.

        if (isTracingEnabled && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "afterGettingConnection");
        }

    }
}