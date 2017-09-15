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

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Revision: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 */
public class CSSSASITTPrincipalNameStatic implements CSSSASIdentityToken {

    private final String oid;
    private final String name;
    private transient IdentityToken token;

    public CSSSASITTPrincipalNameStatic(String name) {

        this(GSSUPMechOID.value.substring(4), name);
    }

    public CSSSASITTPrincipalNameStatic(String oid, String name) {
        this.oid = (oid == null ? GSSUPMechOID.value.substring(4) : oid);
        this.name = name;
    }

    @Override
    public IdentityToken encodeIdentityToken(Codec codec) {

        if (token == null) {
            Any any = ORB.init().create_any();
            //TODO consider including a domain in this scoped-username
            GSS_NT_ExportedNameHelper.insert(any, Util.encodeGSSExportName(oid, name));

            byte[] encoding = null;
            try {
                encoding = codec.encode_value(any);
            } catch (InvalidTypeForEncoding itfe) {
                throw new IllegalStateException("Unable to encode principal name '" + name + "' " + itfe, itfe);
            }

            token = new IdentityToken();
            token.principal_name(encoding);
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
        buf.append(spaces).append("CSSSASITTPrincipalNameStatic: [\n");
        buf.append(moreSpaces).append("oid: ").append(oid).append("\n");
        buf.append(moreSpaces).append("name: ").append(name).append("\n");
        buf.append(spaces).append("]\n");
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return ITTPrincipalName.value;
    }

}
