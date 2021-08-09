/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.css;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Revision: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 */
public class CSSSASITTPrincipalNameDynamic implements CSSSASIdentityToken {

    private final String oid;
    private final String domain;
    private final String realm;

    public CSSSASITTPrincipalNameDynamic(String oid, Class principalClass, String domain, String realm) {
        this.oid = (oid == null ? GSSUPMechOID.value.substring(4) : oid);
        this.domain = domain;
        this.realm = realm;
    }

    public CSSSASITTPrincipalNameDynamic(String oid, String domain) {
        this.oid = (oid == null ? GSSUPMechOID.value.substring(4) : oid);
        this.domain = domain;
        this.realm = domain;
    }

    /**
     * TODO should also use login domains?
     * 
     * @return IdentityToken
     */
    @Override
    public IdentityToken encodeIdentityToken(Codec codec) {
        IdentityToken token = null;

        SubjectManager subjectManager = new SubjectManager();
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }

        Set<WSPrincipal> principals = subject.getPrincipals(WSPrincipal.class);
        Principal principal = (principals.isEmpty()) ? null : principals.iterator().next();

        // There must be a WSPrincipal in the subject, but check for null to satisfy findbugs
        if (principal != null) {

            Any any = ORB.init().create_any();

            //TODO consider including a domain in this scoped-username
            String principalName = principal.getName();
            GSS_NT_ExportedNameHelper.insert(any, Util.encodeGSSExportName(oid, principalName));

            byte[] encoding = null;
            try {
                encoding = codec.encode_value(any);
            } catch (InvalidTypeForEncoding itfe) {
                throw new IllegalStateException("Unable to encode principal name '" + principalName + "' " + itfe, itfe);
            }

            token = new IdentityToken();
            token.principal_name(encoding);
        } else {
            token = new IdentityToken();
            token.anonymous(true);
        }

        return token;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("CSSSASITTPrincipalNameDynamic: [\n");
        buf.append(moreSpaces).append("oid: ").append(oid).append("\n");
        buf.append(moreSpaces).append("domain: ").append(domain).append("\n");
        buf.append(moreSpaces).append("realm: ").append(realm).append("\n");
        buf.append(spaces).append("]\n");
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return ITTPrincipalName.value;
    }

}
