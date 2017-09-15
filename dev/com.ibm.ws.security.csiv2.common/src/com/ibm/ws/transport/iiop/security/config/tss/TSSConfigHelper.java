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
package com.ibm.ws.transport.iiop.security.config.tss;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.omg.CSIIOP.TransportAddress;
import org.osgi.framework.Bundle;

import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.transport.iiop.security.util.HelperConstants;

/**
 * A property editor for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSConfig}.
 * 
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class TSSConfigHelper implements HelperConstants {

    /**  */
    private static final String CSIV2_CONFIGURATION = "csiv2Configuration";

    /**  */
//    private static final String ITTX509_CERT_CHAIN = "ITTX509CertChain";
    /**  */
//    private static final String ITT_DISTINGUISHED_NAME = "ITTDistinguishedName";
    /**  */
//    private static final String ITT_PRINCIPAL_NAME_GSSUP = "ITTPrincipalNameGSSUP";
    /**  */
    private static final String PRIVILEGE_AUTHORITY = "privilegeAuthority";
    /**  */
//    private static final String GSS_EXPORTED_NAME = "gssExportedName";
    /**  */
    private static final String GENERAL_NAME = "generalName";
    /**  */
    private static final String TARGET_NAME = "targetName";
    /**  */
    private static final String REQUIRED = "required";
    /**  */
    private static final String IDENTITY_TOKEN_TYPES = "identityTokenTypes";
    /**  */
    private static final String SERVICE_CONFIGURATION_LIST = "serviceConfigurationList";

//    private static final Object SSL_TRANSPORT_MECH = "com.ibm.ws.transport.iiop.tssSSLTransportMech";
//    private static final Object SECIOP_TRANSPORT_MECH = "com.ibm.ws.transport.iiop.tssSECIOPTransportMech";

    /**
     * Returns a TSSConfig object initialized with the input object
     * as an XML string.
     * 
     * @param addrMap TODO
     * 
     * @return a TSSConfig object
     * @throws org.apache.geronimo.common.propertyeditor.PropertyEditorException
     *             An IOException occured.
     */
    public static TSSConfig getTSSConfig(Map<String, Object> props, Map<OptionsKey, List<TransportAddress>> addrMap, Bundle bundle) throws Exception {
        TSSConfig tssConfig = new TSSConfig();

        List<Map<String, Object>> tssConfigs = Nester.nest(CSIV2_CONFIGURATION, props);
        if (!tssConfigs.isEmpty()) {
            Map<String, Object> properties = tssConfigs.get(0);
//            tssConfig.setInherit((Boolean) properties.get(INHERIT));
            List<Map<String, Object>> mechList = Nester.nest(COMPOUND_SEC_MECH_TYPE_LIST, properties);
            //TODO liberty get the ssl transport info from the corbabean?
//            List<Map<String, Object>> transportMechGroups = mechInfo.get(SSL_OPTIONS);
//            if (!transportMechGroups.isEmpty()) {
//                Map<String, Object> transportMechGroup = transportMechGroups.get(0);
//                String type = (String) transportMechGroup.get(CONFIG_REFERENCE_TYPE);
//                if (SSL_TRANSPORT_MECH.equals(type)) {
//                    tssConfig.setTransport_mech(extractSSL(transportMechGroup));
//                } else if (SECIOP_TRANSPORT_MECH.equals(type)) {
//                    throw new IllegalStateException("SECIOP processing not implemented");
//                } else {
//                    throw new IllegalStateException("Unrecognized transport mech type: " + type);
//                }
//            } else {
//            tssConfig.setTransport_mech(new TSSNULLTransportConfig());
//            }

//            List<Map<String, Object>> secmechlists = mechInfo.get(COMPOUND_SEC_MECH_TYPE_LIST);
//            if (!secmechlists.isEmpty()) {
//                Map<String, Object> secMechList = secmechlists.get(0);
            TSSCompoundSecMechListConfig mechListConfig = tssConfig.getMechListConfig();
            mechListConfig.setStateful((Boolean) properties.get(STATEFUL));

//                List<Map<String, Object>> mechList = Nester.nest(COMPOUND_SECH_MECH_LIST, secMechList);
            for (Map<String, Object> mech : mechList) {
                TSSCompoundSecMechConfig cMech = extractCompoundSecMech(mech, addrMap, bundle);
//                cMech.setTransport_mech(tssConfig.getTransport_mech());
                mechListConfig.add(cMech);
            }
//            }
        }
        return tssConfig;
    }

    protected static TSSTransportMechConfig extractSSL(Map<String, Object> sslConfigProps, Map<OptionsKey, List<TransportAddress>> addrMap) {
        TSSSSLTransportConfig sslConfig = new TSSSSLTransportConfig();

        sslConfig.setSupports(extractAssociationOptions((String[]) sslConfigProps.get(SUPPORTS)));
        sslConfig.setRequires(extractAssociationOptions((String[]) sslConfigProps.get(REQUIRES)));
        //TODO trustGroup???

        OptionsKey key = new OptionsKey(sslConfig.getSupports(), sslConfig.getRequires());
        List<TransportAddress> addresses = addrMap.get(key);
        if (addresses == null) {
            throw new IllegalStateException("No transport addressses configured for supports: " + sslConfigProps.get(SUPPORTS)
                                            + " and requires: " + sslConfigProps.get(REQUIRES));
        }
        sslConfig.setTransportAddresses(addresses);
        return sslConfig;
    }

    public static Map<OptionsKey, List<TransportAddress>> extractTransportAddresses(Map<String, Object> properties) {
        List<Map<String, Object>> listOfAddrProps = Nester.nest("transportAddress", properties);
        return extractTransportAddresses(listOfAddrProps);
    }

    protected static Map<OptionsKey, List<TransportAddress>> extractTransportAddresses(List<Map<String, Object>> listOfAddrProps) {
        Map<OptionsKey, List<TransportAddress>> mapOfAddr = new HashMap<OptionsKey, List<TransportAddress>>();
        for (Map<String, Object> addrProps : listOfAddrProps) {
            short supports = extractAssociationOptions((String[]) addrProps.get(SUPPORTS));
            short requires = extractAssociationOptions((String[]) addrProps.get(REQUIRES));
            OptionsKey key = new OptionsKey(supports, requires);
            List<TransportAddress> addrs = mapOfAddr.get(key);
            if (addrs == null) {
                addrs = new ArrayList<TransportAddress>();
                mapOfAddr.put(key, addrs);
            }
            addrs.add(new TransportAddress((String) addrProps.get("host"), (Short) addrProps.get("port")));
        }
//        List<TLS_SEC_TRANS> result = new ArrayList<TLS_SEC_TRANS>();
//        for (Map.Entry<OptionsKey, List<TransportAddress>> entry : mapOfAddr.entrySet()) {
//            TransportAddress[] addrs = entry.getValue().toArray(new TransportAddress[entry.getValue().size()]);
//            TLS_SEC_TRANS trans = new TLS_SEC_TRANS(entry.getKey().supports, entry.getKey().requires, addrs);
//            result.add(trans);
//        }
        return mapOfAddr;
    }

    protected static TSSCompoundSecMechConfig extractCompoundSecMech(Map<String, Object> mech, Map<OptionsKey, List<TransportAddress>> addrMap, Bundle bundle) throws Exception {

        Map<String, List<Map<String, Object>>> mechInfo = Nester.nest(mech, SSL_OPTIONS, GSSUP, SAS_MECH);
        TSSCompoundSecMechConfig result = new TSSCompoundSecMechConfig();

        List<Map<String, Object>> transportMechGroups = mechInfo.get(SSL_OPTIONS);
        if (!transportMechGroups.isEmpty()) {
            Map<String, Object> transportMechGroup = transportMechGroups.get(0);
            result.setTransport_mech(extractSSL(transportMechGroup, addrMap));
        } else {
            result.setTransport_mech(new TSSNULLTransportConfig());
        }
        List<Map<String, Object>> gssups = mechInfo.get(GSSUP);
        if (!gssups.isEmpty()) {
            result.setAs_mech(extractASMech(gssups.get(0)));
        } else {
            result.setAs_mech(new TSSNULLASMechConfig());
        }

        List<Map<String, Object>> sasMechs = mechInfo.get(SAS_MECH);
        if (!sasMechs.isEmpty()) {
            result.setSas_mech(extractSASMech(sasMechs.get(0), bundle));
        }

        return result;
    }

    protected static TSSASMechConfig extractASMech(Map<String, Object> gssupMech) {

        TSSGSSUPMechConfig gssupConfig = new TSSGSSUPMechConfig();

        gssupConfig.setTargetName((String) gssupMech.get(TARGET_NAME));
        gssupConfig.setRequired((Boolean) gssupMech.get(REQUIRED));

        return gssupConfig;
    }

    protected static TSSSASMechConfig extractSASMech(Map<String, Object> sasMech, Bundle bundle) throws Exception {

        Map<String, List<Map<String, Object>>> mechInfo = Nester.nest(sasMech, SERVICE_CONFIGURATION_LIST, IDENTITY_TOKEN_TYPES);
        TSSSASMechConfig sasMechConfig = new TSSSASMechConfig();

        List<Map<String, Object>> serviceConfigurationLists = mechInfo.get(SERVICE_CONFIGURATION_LIST);
        if (!serviceConfigurationLists.isEmpty()) {
            Map<String, Object> serviceConfigurationList = serviceConfigurationLists.get(0);
            sasMechConfig.setRequired((Boolean) serviceConfigurationList.get(REQUIRED));

            Map<String, List<Map<String, Object>>> namePropsMap = Nester.nest(serviceConfigurationList, GENERAL_NAME);
            for (Map<String, Object> generalName : namePropsMap.get(GENERAL_NAME)) {
                String type = (String) generalName.get(CONFIG_REFERENCE_TYPE);
                if ("com.ibm.ws.transport.iiop.tssGeneralName".equals(type)) {
                    //TODO is byte[] reasonable? what's the difference between String and byte[] here?
                    sasMechConfig.addServiceConfigurationConfig(new TSSGeneralNameConfig((String) generalName.get(PRIVILEGE_AUTHORITY)));
                } else if ("com.ibm.ws.transport.iiop.tssGSSExportedName".equals(type)) {
                    sasMechConfig.addServiceConfigurationConfig(new TSSGSSExportedNameConfig((String) generalName.get(PRIVILEGE_AUTHORITY), (String) generalName.get(OID)));
                } else {
                    throw new IllegalStateException("Unrecognized service configuration: " + type);
                }
            }
        }

        List<Map<String, Object>> identityTokenTypesList = mechInfo.get(IDENTITY_TOKEN_TYPES);
        if (identityTokenTypesList.isEmpty()) {
            sasMechConfig.addIdentityToken(new TSSITTAbsent());
        } else {
            Map<String, Object> identitytokenTypes = identityTokenTypesList.get(0);
            List<Map<String, Object>> sasInfos = Nester.nest("ittGroup", identitytokenTypes);
            if (sasInfos.isEmpty()) {
                sasMechConfig.addIdentityToken(new TSSITTAbsent());
            } else {
                for (Map<String, Object> sasInfo : sasInfos) {
                    String type = (String) sasInfo.get(CONFIG_REFERENCE_TYPE);
                    if ("com.ibm.ws.transport.iiop.tssIttAbsent".equals(type)) {
                        sasMechConfig.addIdentityToken(new TSSITTAbsent());
                    } else if ("com.ibm.ws.transport.iiop.tssIttAnonymous".equals(type)) {
                        sasMechConfig.addIdentityToken(new TSSITTAnonymous());
                    } else if ("com.ibm.ws.transport.iiop.tssIttPrincipalNameGSSUP".equals(type)) {
                        String principalClassName = (String) sasInfo.get(PRINCIPAL_CLASS);
                        Class<?> principalClass;
                        try {
                            principalClass = bundle.loadClass(principalClassName);
                        } catch (ClassNotFoundException e) {
                            throw new Exception("Could not load principal class", e);
                        }
                        //TODO domain and realm are probably geronimo specific
                        String domainName = (String) sasInfo.get(DOMAIN);
                        String realmName = null;
                        if (domainName != null) {
                            realmName = (String) sasInfo.get(REALM);
                        }

                        try {
                            sasMechConfig.addIdentityToken(new TSSITTPrincipalNameGSSUP(principalClass, realmName, domainName));
                        } catch (NoSuchMethodException e) {
                            throw new Exception("Could not find principal class constructor", e);
                        }
                    } else if ("com.ibm.ws.transport.iiop.tssIttDistinguishedName".equals(type)) {
                        //TODO domain and realm are probably geronimo specific
                        sasMechConfig.addIdentityToken(new TSSITTDistinguishedName(null, null));
                    } else if ("com.ibm.ws.transport.iiop.tssIttX509CertChain".equals(type)) {
                        //TODO domain and realm are probably geronimo specific
                        sasMechConfig.addIdentityToken(new TSSITTX509CertChain(null, null));
                    }
                }
            }

        }

        return sasMechConfig;
    }

    protected static short extractAssociationOptions(String[] list) {
        short result = 0;

        if (list != null) {
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
        }
        return result;
    }

}
