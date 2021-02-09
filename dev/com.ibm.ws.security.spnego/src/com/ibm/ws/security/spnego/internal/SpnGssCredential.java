/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.krb5.Krb5Common;
import com.ibm.ws.security.spnego.SpnegoConfig;

public class SpnGssCredential {
    private static final TraceComponent tc = Tr.register(SpnGssCredential.class);

    private HashMap<String, GSSCredential> spnGssCredentials = null;
    private SpnegoConfig spnegoConfig = null;
    Krb5Util krb5Util = new Krb5Util();

    public void init(List<String> servicePrincipalNames, SpnegoConfig spnegoConfig) {
        this.spnegoConfig = spnegoConfig;
        String oldPropValue = null;
        if (servicePrincipalNames != null && !servicePrincipalNames.isEmpty()) {
            spnGssCredentials = new HashMap<String, GSSCredential>();

            krb5Util.setKrb5ConfigAndKeytabProps(spnegoConfig);
            oldPropValue = Krb5Common.setPropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, "false");

            Iterator<String> itr = servicePrincipalNames.iterator();
            while (itr.hasNext()) {
                String spn = itr.next();
                GSSCredential gssCred = createSpnGSSCredential(spn);
                if (gssCred != null) {
                    String hostName = extractHostName(spn);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "host name: " + hostName + " spn: " + spn + " gssCred: " + gssCred);
                    }
                    if (spnGssCredentials.get(hostName) != null) {
                        Tr.warning(tc, "SPNEGO_MULTIPLE_SPNS_WITH_SAME_HOST_NAME", hostName);
                    } else {
                        spnGssCredentials.put(hostName, gssCred);
                    }
                }
            }
            Krb5Common.restorePropertyAsNeeded(Krb5Common.USE_SUBJECT_CREDS_ONLY, oldPropValue, "true");
        }
        if (spnGssCredentials == null || spnGssCredentials.isEmpty()) {
            Tr.error(tc, "SPNEGO_NO_SPN_GSS_CREDENTIAL");
        }
    }

    public boolean isEmpty() {
        return (spnGssCredentials != null ? spnGssCredentials.isEmpty() : true);
    }

    public int howManySpns() {
        int size = 0;
        if (spnGssCredentials != null) {
            return spnGssCredentials.size();
        }
        return size;
    }

    /**
     * @param spn
     * @return
     */
    protected String extractHostName(String spn) {
        String hostname = spn;

        if (spn.indexOf("@") != -1) { // remove the @ and realm name
            hostname = spn.substring(0, spn.indexOf("@"));
        }

        if (hostname.indexOf("/") != -1) {
            String[] hs = hostname.split("/");
            hostname = hs[1];
        }
        return hostname;
    }

    public GSSCredential getSpnGSSCredential(String hostName) {
        return (spnGssCredentials != null ? spnGssCredentials.get(hostName) : null);
    }

    protected GSSCredential createSpnGSSCredential(String servicePrincipalName) {
        GSSCredential spnGSSCred = null;
        GSSName gssName = null;
        try {

            GSSManager gssManager = GSSManager.getInstance();

            gssName = createGSSName(gssManager, servicePrincipalName);

            spnGSSCred = createGSSCredential(gssName, gssManager, servicePrincipalName);

        } catch (GSSException e) {
            Tr.error(tc, "SPNEGO_CAN_NOT_CREATE_GSSCRED_FOR_SPN",
                     (gssName != null ? gssName.toString() : servicePrincipalName), e);
        }
        return spnGSSCred;
    }

    /**
     * @param gssName
     * @param gssManager
     * @return
     * @throws GSSException
     */
    private GSSCredential createGSSCredential(GSSName gssName, GSSManager gssManager, String spn) throws GSSException {
        GSSCredential spnGSSCred;
        String previousSpn = null;

        if (Krb5Common.isIBMJdk18 || Krb5Common.isOtherSupportJDKs) {
            if (Krb5Common.isOtherSupportJDKs) {// We support multiple SPNs so switch to the right one
                previousSpn = Krb5Common.getSystemProperty(Krb5Common.KRB5_PRINCIPAL);
                Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, spn);
                Krb5Common.setPropertyAsNeeded(Krb5Common.KRB5_NAME, spn);
            }
            spnGSSCred = gssManager.createCredential(gssName,
                                                     GSSCredential.DEFAULT_LIFETIME,
                                                     Krb5Common.KRB5_MECH_OID,
                                                     GSSCredential.ACCEPT_ONLY);

            spnGSSCred.add(gssName,
                           GSSCredential.INDEFINITE_LIFETIME,
                           GSSCredential.INDEFINITE_LIFETIME,
                           Krb5Common.SPNEGO_MECH_OID,
                           GSSCredential.ACCEPT_ONLY);
            if (Krb5Common.isOtherSupportJDKs) {
                Krb5Common.restorePropertyAsNeeded(Krb5Common.KRB5_PRINCIPAL, previousSpn, spn);
                Krb5Common.restorePropertyAsNeeded(Krb5Common.KRB5_NAME, previousSpn, spn);
            }
        } else {
            spnGSSCred = gssManager.createCredential(gssName,
                                                     GSSCredential.INDEFINITE_LIFETIME,
                                                     Krb5Common.SPNEGO_MECH_OID,
                                                     GSSCredential.ACCEPT_ONLY);
            return spnGSSCred;
        }
        return spnGSSCred;
    }

    /**
     * The servicePrincipalName can be <hostName>@krb5Realm or just hostName
     * if the servicePrincipalName is just the host name, then we use the NT_HOSTBASE_SERVICE. Otherwise, use
     * the NT_USER_NAME.
     *
     * @param gssManager
     * @param servicePrincipalName
     * @return
     * @throws GSSException
     */
    private GSSName createGSSName(GSSManager gssManager, String servicePrincipalName) throws GSSException {
        GSSName gssName = null;
        gssName = gssManager.createName(servicePrincipalName, GSSName.NT_USER_NAME, Krb5Common.SPNEGO_MECH_OID);
        if (spnegoConfig.isCanonicalHostName() && !servicePrincipalName.startsWith(SpnegoConfigImpl.LOCAL_HOST)) {
            gssName = gssName.canonicalize(Krb5Common.KRB5_MECH_OID);
        }

        return gssName;
    }
}
