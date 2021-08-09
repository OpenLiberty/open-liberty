/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ssl.JSSEProviderFactory;

/**
 * Configuration class for a trust manager definition.
 * <p>
 * This class holds the attributes of a TrustManager configured in the WCCM
 * model.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class TrustManagerData {
    private final static TraceComponent tc = Tr.register(TrustManagerData.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private String tmName;
    private String tmProvider;
    private String tmAlgorithm;
    private String tmCustomClass;
    private Properties tmCustomProps;

    /**
     * Constructor with a given name and list of configuration properties.
     * 
     * @param _name
     * @param properties
     */
    public TrustManagerData(String _name, Map<String, String> properties) {
        this.tmName = _name;
        for (Entry<String, String> prop : properties.entrySet()) {
            final String key = prop.getKey();
            final String value = prop.getValue();
            if (key.equalsIgnoreCase("algorithm")) {
                this.tmAlgorithm = value;
            } else if (key.equalsIgnoreCase("provider")) {
                this.tmProvider = value;
            } else if (key.equalsIgnoreCase("trustManagerClass")) {
                this.tmCustomClass = value;
            } else {
                // custom property
                if (null == this.tmCustomProps) {
                    this.tmCustomProps = new Properties();
                }
                this.tmCustomProps.setProperty(key, value);

            }
        }
    }

    /**
     * Constructor.
     * 
     * @param name
     * @param provider
     * @param algorithm
     * @param customClass
     * @param customAttributes
     */
    public TrustManagerData(String name, String provider, String algorithm, String customClass, Properties customAttributes) {
        this.tmName = name;
        this.tmProvider = provider;
        this.tmAlgorithm = algorithm;
        this.tmCustomClass = customClass;
        this.tmCustomProps = customAttributes;
    }

    public String getName() {
        return this.tmName;
    }

    public void setName(String s) {
        this.tmName = s;
    }

    public String getProvider() {
        return this.tmProvider;
    }

    public void setProvider(String s) {
        this.tmProvider = s;
    }

    public String getAlgorithm() {
        return this.tmAlgorithm;
    }

    public void setAlgorithm(String s) {
        this.tmAlgorithm = s;
    }

    public String getTrustManagerClass() {
        return this.tmCustomClass;
    }

    public void setTrustManagerClass(String s) {
        this.tmCustomClass = s;
    }

    public Properties getAdditionalTrustManagerAttrs() {
        return this.tmCustomProps;
    }

    public void setAdditionalTrustManagerAttrs(Map<String, String> attributes) {
        if (null == this.tmCustomProps) {
            this.tmCustomProps = new Properties();
        }
        for (Entry<String, String> attr : attributes.entrySet()) {
            this.tmCustomProps.setProperty(attr.getKey(), attr.getValue());
        }
    }

    public String getTrustManagerString() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getTrustManagerString");
        String rc = null;

        if (tmCustomClass != null) {
            rc = tmCustomClass;
        } else if (tmAlgorithm != null && tmProvider != null) {
            rc = tmAlgorithm + "|" + tmProvider;
        } else if (tmAlgorithm != null) {
            rc = tmAlgorithm;
        } else {
            rc = JSSEProviderFactory.getTrustManagerFactoryAlgorithm();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getTrustManagerString -> " + rc);
        return rc;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(128);

        sb.append("TrustManagerData: name=").append(this.tmName);
        sb.append(", algorithm=").append(this.tmAlgorithm);
        sb.append(", provider=").append(this.tmProvider);
        sb.append(", customClass=").append(this.tmCustomClass);

        return sb.toString();
    }
}
