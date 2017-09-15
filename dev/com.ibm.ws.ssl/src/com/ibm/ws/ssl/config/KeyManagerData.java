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
 * Configuration class of a key manager object.
 * <p>
 * This class handles the KeyManager information coming from the WCCM model. The
 * information is converted to this class so the model can be released.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class KeyManagerData {
    private final static TraceComponent tc = Tr.register(KeyManagerData.class,
                                                         "SSL",
                                                         "com.ibm.ws.ssl.resources.ssl");

    private String kmName = null;
    private String kmProvider = null;
    private String kmAlgorithm = null;
    private String kmCustomClass = null;
    private Properties kmCustomProps = null;

    /**
     * Constructor with a provided name and array of property values.
     * 
     * @param _name
     * @param properties
     */
    public KeyManagerData(String _name, Map<String, String> properties) {
        this.kmName = _name;
        for (Entry<String, String> current : properties.entrySet()) {
            final String key = current.getKey();
            final String value = current.getValue();
            if (key.equalsIgnoreCase("algorithm")) {
                this.kmAlgorithm = value;
            } else if (key.equalsIgnoreCase("provider")) {
                this.kmProvider = value;
            } else if (key.equalsIgnoreCase("keyManagerClass")) {
                this.kmCustomClass = value;
            } else {
                // custom property
                if (null == this.kmCustomProps) {
                    this.kmCustomProps = new Properties();
                }
                this.kmCustomProps.setProperty(key, value);
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
    public KeyManagerData(String name,
                          String provider,
                          String algorithm,
                          String customClass,
                          Properties customAttributes) {
        this.kmName = name;
        this.kmProvider = provider;
        this.kmAlgorithm = algorithm;
        this.kmCustomClass = customClass;
        this.kmCustomProps = customAttributes;
    }

    public String getName() {
        return this.kmName;
    }

    public void setName(String s) {
        this.kmName = s;
    }

    public String getProvider() {
        return this.kmProvider;
    }

    public void setProvider(String s) {
        this.kmProvider = s;
    }

    public String getAlgorithm() {
        return this.kmAlgorithm;
    }

    public void setAlgorithm(String s) {
        this.kmAlgorithm = s;
    }

    public String getKeyManagerClass() {
        return this.kmCustomClass;
    }

    public void setKeyManagerClass(String s) {
        this.kmCustomClass = s;
    }

    public Properties getAdditionalKeyManagerAttrs() {
        return this.kmCustomProps;
    }

    public void setAdditionalKeyManagerAttrs(Map<String, String> attributes) {
        if (null == this.kmCustomProps) {
            this.kmCustomProps = new Properties();
        }
        for (Entry<String, String> attr : attributes.entrySet()) {
            this.kmCustomProps.setProperty(attr.getKey(), attr.getValue());
        }
    }

    public String getKeyManagerString() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getKeyManagerString");

        String rc = null;

        if (kmCustomClass != null) {
            rc = kmCustomClass;
        } else if (kmAlgorithm != null && kmProvider != null) {
            rc = kmAlgorithm + "|" + kmProvider;
        } else if (kmAlgorithm != null) {
            rc = kmAlgorithm;
        } else {
            rc = JSSEProviderFactory.getKeyManagerFactoryAlgorithm();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getKeyManagerString -> " + rc);
        return rc;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("KeyManagerData: name=").append(this.kmName);
        sb.append(", algorithm=").append(this.kmAlgorithm);
        sb.append(", provider=").append(this.kmProvider);
        sb.append(", customClass=").append(this.kmCustomClass);

        return sb.toString();
    }

}
