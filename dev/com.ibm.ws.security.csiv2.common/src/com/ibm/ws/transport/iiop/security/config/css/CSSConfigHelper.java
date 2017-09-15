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

import java.util.List;
import java.util.Map;

import org.omg.CSIIOP.CompositeDelegation;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.DetectMisordering;
import org.omg.CSIIOP.DetectReplay;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.NoDelegation;
import org.omg.CSIIOP.NoProtection;
import org.omg.CSIIOP.SimpleDelegation;
import org.osgi.framework.Bundle;

import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.transport.iiop.security.util.HelperConstants;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Converts flattened config tree to CSS object tree. Note that the xml is intended to support all possibilities whether or not implemented.
 * 
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class CSSConfigHelper implements HelperConstants {

    /**  */
    private static final String AS_GSSUP_DYNAMIC = "com.ibm.ws.transport.iiop.cssGSSUPDynamic";
    /**  */
    private static final String AS_GSSUP_STATIC = "com.ibm.ws.transport.iiop.cssGSSUPStatic";
    /**  */
    private static final String AS_MECH_GROUP = "asMechGroup";

    public static CSSConfig getCSSConfig(Map<String, Object> properties, Bundle bundle) throws Exception {

        CSSConfig cssConfig = new CSSConfig();

        List<Map<String, Object>> compoundSecurityMechLists = Nester.nest("csiv2Configuration", properties);
//        Map<String, Object> cssConfigProperties = cssConfigs.isEmpty() ? null : cssConfigs.get(0);
//        if (cssConfigProperties != null) {
//            List<Map<String, Object>> compoundSecurityMechLists = Nester.nest(COMPOUND_SEC_MECH_TYPE_LIST, cssConfigProperties);
        if (!compoundSecurityMechLists.isEmpty()) {
            Map<String, Object> compoundSecurityMechList = compoundSecurityMechLists.get(0);
            CSSCompoundSecMechListConfig mechListConfig = cssConfig.getMechList();
            mechListConfig.setStateful((Boolean) compoundSecurityMechList.get(STATEFUL));

            List<Map<String, Object>> mechList = Nester.nest(COMPOUND_SEC_MECH_TYPE_LIST, compoundSecurityMechList);
            for (Map<String, Object> mech : mechList) {
                mechListConfig.add(extractCompoundSecMech(mech, bundle));
            }
        }
//        }
        return cssConfig;
    }

    protected static CSSCompoundSecMechConfig extractCompoundSecMech(Map<String, Object> mechType, Bundle bundle) throws Exception {

        Map<String, List<Map<String, Object>>> mechInfo = Nester.nest(mechType, SSL_OPTIONS, AS_MECH_GROUP, SAS_MECH);
        CSSCompoundSecMechConfig result = new CSSCompoundSecMechConfig();

        List<Map<String, Object>> sslOptionsList = mechInfo.get(SSL_OPTIONS);
        if (!sslOptionsList.isEmpty()) {
            Map<String, Object> sslOptions = sslOptionsList.get(0);
            result.setTransport_mech(extractSSLTransport(sslOptions));
        } else {
            result.setTransport_mech(new CSSNULLTransportConfig());
        }

        List<Map<String, Object>> gssups = mechInfo.get(AS_MECH_GROUP);
        if (!gssups.isEmpty()) {
            Map<String, Object> gssup = gssups.get(0);
            String type = (String) gssup.get(CONFIG_REFERENCE_TYPE);
            if (AS_GSSUP_STATIC.equals(type)) {
                result.setAs_mech(extractGSSUPStatic(gssup));
            } else if (AS_GSSUP_DYNAMIC.equals(type)) {
                result.setAs_mech(extractGSSUPDynamic(gssup));
            } else {
                throw new IllegalStateException("Unrecognized gssup type " + type);
            }
        } else {
            result.setAs_mech(new CSSNULLASMechConfig());
        }

        List<Map<String, Object>> sasMechs = mechInfo.get(SAS_MECH);
        result.setSas_mech(extractSASMech(sasMechs, bundle));

        return result;
    }

    protected static CSSTransportMechConfig extractSSLTransport(Map<String, Object> sslConfig) {
        CSSSSLTransportConfig result = new CSSSSLTransportConfig();

        result.setSupports(extractAssociationOptions((String[]) sslConfig.get(SUPPORTS)));
        result.setRequires(extractAssociationOptions((String[]) sslConfig.get(REQUIRES)));
//TODO trustGroup???
        return result;
    }

    protected static CSSASMechConfig extractGSSUPStatic(Map<String, Object> gssupType) {
        SerializableProtectedString password = (SerializableProtectedString) gssupType.get(PASSWORD);
        return new CSSGSSUPMechConfigStatic((String) gssupType.get(USERNAME), password, (String) gssupType.get(DOMAIN));
    }

    protected static CSSASMechConfig extractGSSUPDynamic(Map<String, Object> gssupType) {
        return new CSSGSSUPMechConfigDynamic((String) gssupType.get(DOMAIN));
    }

    protected static CSSSASMechConfig extractSASMech(List<Map<String, Object>> sasMechTypes, Bundle bundle) throws Exception {
        CSSSASMechConfig result = new CSSSASMechConfig();

        if (sasMechTypes.isEmpty()) {
            result.setIdentityToken(new CSSSASITTAbsent());
        } else {
            Map<String, Object> sasMechType = sasMechTypes.get(0);
            List<Map<String, Object>> sasInfos = Nester.nest("ittGroup", sasMechType);
            if (sasInfos.isEmpty()) {
                result.setIdentityToken(new CSSSASITTAbsent());
            } else {
                Map<String, Object> sasInfo = sasInfos.get(0);
                String type = (String) sasInfo.get(CONFIG_REFERENCE_TYPE);
                if ("com.ibm.ws.transport.iiop.cssIttAbsent".equals(type)) {
                    result.setIdentityToken(new CSSSASITTAbsent());
                } else if ("com.ibm.ws.transport.iiop.cssIttAnonymous".equals(type)) {
                    result.setIdentityToken(new CSSSASITTAnonymous());
                } else if ("com.ibm.ws.transport.iiop.cssIttPrincipalNameStatic".equals(type)) {
                    result.setIdentityToken(new CSSSASITTPrincipalNameStatic((String) sasInfo.get(OID), (String) sasInfo.get(NAME)));
                } else if ("com.ibm.ws.transport.iiop.cssIttPrincipalNameDynamic".equals(type)) {
                    String principalClassName = (String) sasInfo.get(PRINCIPAL_CLASS);
                    Class principalClass = null;
                    try {
                        principalClass = bundle.loadClass(principalClassName);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Could not load principal class", e);
                    }
                    //TODO domain and realm are probably geronimo specific
                    String domainName = (String) sasInfo.get(DOMAIN);
                    String realmName = null;
                    if (domainName != null) {
                        realmName = (String) sasInfo.get(REALM);
                    }
                    result.setIdentityToken(new CSSSASITTPrincipalNameDynamic((String) sasInfo.get(OID), principalClass, domainName, realmName));
                }
            }
        }
        return result;
    }

    protected static short extractAssociationOptions(String[] list) {
        short result = 0;

        for (String string : list) {
            //TODO switch statement
            //TODO invalid value handling (although metatype should prevent this)
            AssociationOptions obj = AssociationOptions.valueOf(string);
            if (AssociationOptions.NoProtection.equals(obj)) {
                result |= NoProtection.value;
            } else if (AssociationOptions.Integrity.equals(obj)) {
                result |= Integrity.value;
            } else if (AssociationOptions.Confidentiality.equals(obj)) {
                result |= Confidentiality.value;
            } else if (AssociationOptions.DetectReplay.equals(obj)) {
                result |= DetectReplay.value;
            } else if (AssociationOptions.DetectMisordering.equals(obj)) {
                result |= DetectMisordering.value;
            } else if (AssociationOptions.EstablishTrustInTarget.equals(obj)) {
                result |= EstablishTrustInTarget.value;
            } else if (AssociationOptions.EstablishTrustInClient.equals(obj)) {
                result |= EstablishTrustInClient.value;
            } else if (AssociationOptions.NoDelegation.equals(obj)) {
                result |= NoDelegation.value;
            } else if (AssociationOptions.SimpleDelegation.equals(obj)) {
                result |= SimpleDelegation.value;
            } else if (AssociationOptions.CompositeDelegation.equals(obj)) {
                result |= CompositeDelegation.value;
            }
        }
        return result;
    }

}
