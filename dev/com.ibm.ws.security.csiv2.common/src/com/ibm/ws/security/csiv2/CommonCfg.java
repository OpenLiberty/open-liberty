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
package com.ibm.ws.security.csiv2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.transport.iiop.security.util.HelperConstants.AssociationOptions;

/**
 * config helper class reads the iiopClientPolicy configuration from server.xml and maps to CSSConfig.
 */
public class CommonCfg {

    private static TraceComponent tc = Tr.register(CommonCfg.class);
    public static final String TYPE_KEY = "config.referenceType";
    public static final String KEY_STATEFUL = "stateful";
    public static final String KEY_LAYERS = "layers";
    public static final String KEY_LAYER_AUTHENTICATION = "authenticationLayer";
    public static final String KEY_LAYER_ATTRIBUTE = "attributeLayer";
    public static final String KEY_LAYER_TRANSPORT = "transportLayer";
    public static final String KEY_SSL_REF = "sslRef";
    public static final String KEY_SSL_ENABLED = "sslEnabled";
    public static final String KEY_ESTABLISH_TRUST_IN_CLIENT = "establishTrustInClient";
//    public static final String KEY_ESTABLISH_TRUST_IN_TARGET = "establishTrustInTarget";
//    public static final String KEY_ATTRIBUTE_INTEGRITY = "integrity";
//    public static final String KEY_ATTRIBUTE_CONFIDENTIALITY = "confidentiality";
    public static final String KEY_IDENTITY_ASSERTION_ENABLED = "identityAssertionEnabled";
    public static final String KEY_IDENTITY_ASSERTION_TYPES = "identityAssertionTypes";
    public static final String OPTION_SUPPORTED = "Supported";
    public static final String OPTION_REQUIRED = "Required";
    public static final String OPTION_NEVER = "Never";
    public static final String AUTHENTICATION_MECHANISM_GSSUP = "GSSUP";
    public static final String AUTHENTICATION_MECHANISM_LTPA = "LTPA";

    protected final String defaultAlias;

    /**
     * @param defaultAlias
     */
    public CommonCfg(String defaultAlias) {
        super();
        this.defaultAlias = defaultAlias;
    }

    protected static class LayersData {
        public Map<String, Object> attributeLayer;
        public Map<String, Object> authenticationLayer;
        public Map<String, Object> transportLayer;

        /**
         * @param attributeLayer
         * @param authenticationLayer
         * @param transportLayer
         */
        public LayersData(Map<String, Object> attributeLayer, Map<String, Object> authenticationLayer, Map<String, Object> transportLayer) {
            super();
            this.attributeLayer = attributeLayer;
            this.authenticationLayer = authenticationLayer;
            this.transportLayer = transportLayer;
        }

        /**
         *
         */
        public LayersData() {
            super();
        }

    }

    protected static class PolicyData {
        public boolean stateful;
        public List<LayersData> layersData;

        /**
         * @param stateful
         * @param layersData
         */
        public PolicyData(boolean stateful, List<LayersData> layersData) {
            super();
            this.stateful = stateful;
            this.layersData = layersData;
        }

    }

    protected Set<String> extractSslRefs(Map<String, Object> properties, String keyPolicy, String policyType) {
        Set<String> sslRefs = new HashSet<String>();
        PolicyData policyData = extractPolicyData(properties, keyPolicy, policyType);
        if (policyData != null) {
            for (LayersData layers : policyData.layersData) {
                //TODO really? I thought this meant NO_PROTECTION
                if (layers.transportLayer == null) {
                    sslRefs.add(defaultAlias);
                } else if (!Boolean.FALSE.equals(layers.transportLayer.get("sslEnabled"))) {
                    if (layers.transportLayer.get(KEY_SSL_REF) != null) {
                        sslRefs.add((String) layers.transportLayer.get(KEY_SSL_REF));
                    } else {
                        sslRefs.add(defaultAlias);
                    }
                }
            }
        }
        return sslRefs;
    }

    protected PolicyData extractPolicyData(Map<String, Object> properties, String keyPolicy, String policyType) {
        List<Map<String, Object>> compoundSecurityMechLists = Nester.nest(keyPolicy, properties);
        Map<String, Object> compoundSecurityMechList = null;
        /*
         * There will be multiple policies if there is a configuration in addition to the defaultInstances.xml.
         * Let us iterate through the list of compound security mechanism list to find the last policy corresponding to the overriding policy.
         */
        Map<String, Object> configFromDefaultInstances = null;
        for (Map<String, Object> tempCompoundSecurityMechList : compoundSecurityMechLists) {
            if (policyType.equals(tempCompoundSecurityMechList.get(TYPE_KEY))) {
                if (configFromDefaultInstances == null) {
                    configFromDefaultInstances = tempCompoundSecurityMechList;
                }
                compoundSecurityMechList = tempCompoundSecurityMechList;
            }
        }
        if (compoundSecurityMechList == null)
            return null;
        boolean stateful = (Boolean) compoundSecurityMechList.get(KEY_STATEFUL);
        List<LayersData> layersList = gatherLayersWithDefaultsIfNeeded(compoundSecurityMechList, configFromDefaultInstances);
        return new PolicyData(stateful, layersList);
    }

    protected List<LayersData> gatherLayersWithDefaultsIfNeeded(Map<String, Object> compoundSecurityMechList,
                                                                Map<String, Object> configFromDefaultInstances) {
        List<LayersData> layersList = new ArrayList<LayersData>();
        List<Map<String, Object>> mechs = Nester.nest(KEY_LAYERS, configFromDefaultInstances);

        LayersData layersFromDefaultInstancesXML = getLayers(first(mechs));

        if (compoundSecurityMechList != configFromDefaultInstances) {
            mechs = Nester.nest(KEY_LAYERS, compoundSecurityMechList);
            for (Map<String, Object> mech : mechs) {
                LayersData layersFromServerXML = getLayers(mech);
                LayersData merged = mergeLayers(layersFromDefaultInstancesXML, layersFromServerXML);
                layersList.add(merged);
            }
        }

        if (layersList.isEmpty()) {
            layersList.add(layersFromDefaultInstancesXML);
        }
        return layersList;
    }

    private LayersData mergeLayers(LayersData result,
                                   LayersData toMerge) {
        if (toMerge.attributeLayer != null)
            result.attributeLayer = toMerge.attributeLayer;
        if (toMerge.authenticationLayer != null)
            result.authenticationLayer = toMerge.authenticationLayer;
        if (toMerge.transportLayer != null)
            result.transportLayer = toMerge.transportLayer;
        return result;
    }

    private LayersData getLayers(Map<String, Object> config) {
        Map<String, List<Map<String, Object>>> layers = Nester.nest(config, KEY_LAYER_TRANSPORT, KEY_LAYER_AUTHENTICATION, KEY_LAYER_ATTRIBUTE);
        return new LayersData(first(layers.get(KEY_LAYER_ATTRIBUTE)), first(layers.get(KEY_LAYER_AUTHENTICATION)), first(layers.get(KEY_LAYER_TRANSPORT)));
    }

    private Map<String, Object> first(List<Map<String, Object>> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    public List<String> getAsMechanisms(Map<String, Object> authenticationLayerProperties) {
        List<String> mechs = new ArrayList<String>();
        String[] mechanisms = (String[]) authenticationLayerProperties.get("mechanisms");
        if (mechanisms != null) {
            for (String mech : mechanisms) {
                if (mech != null && mech.length() > 1)
                    mechs.add(mech);
            }
        }
        return mechs;
    }

    public static void preProcessAssociationOptions(List<AssociationOptions> supported, List<AssociationOptions> required, String value, AssociationOptions option) {
        if (OPTION_SUPPORTED.equals(value)) {
            supported.add(option);
        } else if (OPTION_REQUIRED.equals(value)) {
            required.add(option);
        }
    }

    public static short extractAssociationOptions(List<AssociationOptions> options) {
        short result = 0;

        for (AssociationOptions option : options) {
            switch (option) {
                case NoProtection:
                    result |= NoProtection.value;
                    break;
                case Integrity:
                    result |= Integrity.value;
                    break;
                case Confidentiality:
                    result |= Confidentiality.value;
                    break;
                case DetectReplay:
                    result |= DetectReplay.value;
                    break;
                case DetectMisordering:
                    result |= DetectMisordering.value;
                    break;
                case EstablishTrustInTarget:
                    result |= EstablishTrustInTarget.value;
                    break;
                case EstablishTrustInClient:
                    result |= EstablishTrustInClient.value;
                    break;
                case NoDelegation:
                    result |= NoDelegation.value;
                    break;
                case SimpleDelegation:
                    result |= SimpleDelegation.value;
                    break;

                case CompositeDelegation:
                    result |= CompositeDelegation.value;
                    break;
                default:
                    break; // do nothing
            }
        }
        return result;
    }

    /**
     * printTrace: This method print the messages to the trace file.
     *
     * @param key
     * @param value
     * @param tabLevel
     */
    @Trivial
    public static void printTrace(String key, Object value, int tabLevel) {
        if (tc.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();
            for (int count = 0; count < tabLevel; count++) {
                msg.append("\t");
            }
            if (value != null) {
                msg.append(key);
                msg.append(":");
                msg.append(value);
            } else {
                msg.append(key);
            }
            Tr.debug(tc, msg.toString());
        }
    }

}