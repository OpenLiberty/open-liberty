/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.impl.utils.ClaimsUtils;

/**
 *
 */
public class DefaultJsonWebTokenImpl implements JsonWebToken, Serializable {
    private static TraceComponent tc = Tr.register(DefaultJsonWebTokenImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /**  */
    private static final long serialVersionUID = 1L;
    /**
     * Names of serializable fields.
     */
    private static final String PRINCIPAL = "principal", JWT = "jwt", TYPE = "type";

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
            new ObjectStreamField(PRINCIPAL, String.class),
            new ObjectStreamField(JWT, String.class),
            new ObjectStreamField(TYPE, String.class)
    };

    public static final char SERVICE_NAME_SEPARATOR = ';';
    private transient String principal;
    private transient String jwt;
    private transient String type;
    private JwtClaims claimsSet;

    private ClaimsUtils claimsUtils = new ClaimsUtils();

    public DefaultJsonWebTokenImpl(String jwt, String type, String name) {
        String methodName = "<init>";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwt, type, name);
        }
        this.jwt = jwt;
        this.type = type;
        //this.claimsSet = claimsSet;
        principal = name;
        handleClaims(jwt);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    @Override
    public final JsonWebToken clone() {
        return new DefaultJsonWebTokenImpl(this.jwt, this.type, this.principal);
    }

    /**
     * @param jwt
     * @return
     *
     */
    private void handleClaims(String jwt) {
        try {
            if (claimsUtils == null) {
                claimsUtils = new ClaimsUtils();
            }
            this.claimsSet = claimsUtils.getJwtClaims(jwt);
        } catch (JoseException e) {
            Tr.error(tc, "ERROR_GETTING_CLAIMS_FROM_JWT_STRING", new Object[] { e.getLocalizedMessage() });
        }
    }

    @Override
    public <T> Optional<T> claim(String claimName) {
        T claim = (T) getClaim(claimName);
        return Optional.ofNullable(claim);
    }
    //
    //        /** {@inheritDoc} */
    //        @Override
    //        public <T> T getClaim(String arg0) {
    //            // TODO Auto-generated method stub
    //            return null;
    //        }

    @Override
    @FFDCIgnore({ IllegalArgumentException.class })
    public Object getClaim(String claimName) {
        Claims claimType = Claims.UNKNOWN;
        Object claim = null;
        try {
            claimType = Claims.valueOf(claimName);
        } catch (IllegalArgumentException e) {

        }
        // Handle the jose4j NumericDate types and
        switch (claimType) {
        case exp:
        case iat:
        case auth_time:
        case nbf:
        case updated_at:
            try {
                claim = claimsSet.getClaimValue(claimType.name(), Long.class);
                if (claim == null) {
                    claim = new Long(0);
                }
            } catch (MalformedClaimException e) {
            }
            break;
        case groups:
            claim = getGroups();
            break;
        case aud:
            claim = getAudience();
            break;
        case UNKNOWN:
            claim = claimsSet.getClaimValue(claimName);
            break;
        default:
            claim = claimsSet.getClaimValue(claimType.name());
        }
        return claim;
    }

    @Override
    public String toString() {
        return claimsSet.toJson();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getClaimNames() {
        return new HashSet<String>(claimsSet.getClaimNames());
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return principal;
    }

    @Override
    public Set<String> getAudience() {
        Set<String> audSet = new HashSet<String>();
        try {
            List<String> audList = claimsSet.getStringListClaimValue("aud");
            if (audList != null) {
                audSet.addAll(audList);
            }
        } catch (MalformedClaimException e) {
            try {
                String aud = claimsSet.getStringClaimValue("aud");
                audSet.add(aud);
            } catch (MalformedClaimException e1) {
                Tr.warning(tc, "CLAIM_MALFORMED", new Object[] { "aud", e.getLocalizedMessage() });
            }
        }
        return audSet.size() == 0 ? null : audSet;
    }

    @Override
    public Set<String> getGroups() {
        HashSet<String> groups = new HashSet<String>();
        try {
            List<String> globalGroups = claimsSet.getStringListClaimValue("groups");
            if (globalGroups != null) {
                groups.addAll(globalGroups);
            }
        } catch (MalformedClaimException e) {
            Tr.warning(tc, "CLAIM_MALFORMED", new Object[] { "groups", e.getLocalizedMessage() });
        }
        return groups;
    }

    /**
     * Deserialize json web token.
     *
     * @param in
     *            The stream from which this object is read.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        principal = (String) fields.get(PRINCIPAL, null);
        jwt = (String) fields.get(JWT, null);
        type = (String) fields.get(TYPE, null);
        handleClaims(jwt);
    }

    /**
     * Serialize json web token.
     *
     * @param out
     *            The stream to which this object is serialized.
     *
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put(PRINCIPAL, principal);
        fields.put(JWT, jwt);
        fields.put(TYPE, type);
        out.writeFields();
    }

}
