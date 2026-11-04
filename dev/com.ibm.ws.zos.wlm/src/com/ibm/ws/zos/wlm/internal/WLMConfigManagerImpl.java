/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.wlm.WLMConfigManager;

/**
 * Provides access to the Authorized WLM interfaces relating to WLM Enclaves
 */
public class WLMConfigManagerImpl implements WLMConfigManager {
    /**
     * TraceComponent for this class.
     */
    private static final TraceComponent tc = Tr.register(WLMConfigManagerImpl.class);

    protected static final String DEFAULT_CLASSIFYCOLLECTIONNAME = "BBGZSRV";
    protected static final int WLM_MAXIMUM_COLLECTIONNAME_LENGTH = 18;

    protected final String SUBSYSTEM = "CB  ";
    protected final String SUBSYSTEMNAME = "BBGZSRV ";
    protected final String CREATE_FUNCTIONNAME = "BOSS    ";

    protected String classifyCollectionName;

    private final Map<String, Integer> serviceContextTokensMap = new ConcurrentHashMap<String, Integer>();

    /**
     * The attribute used to specify the WLM collection name in the <zosWorkloadManager>
     * config element.
     */
    final protected static String COLLECTIONNAME_KEY = "collectionName";

    /**
     * This method is called when the config is known.
     */
    protected void updateConfig(Map<String, Object> props) {
        String collectionNameCfg = (String) props.get(COLLECTIONNAME_KEY);

        if ((collectionNameCfg == null) ||
            (collectionNameCfg.isEmpty())) {
            // Issue message stating no collectionName and we are using the default value.
            Tr.warning(tc,
                       "CONFIG_NO_COLLECTIONNAME",
                       DEFAULT_CLASSIFYCOLLECTIONNAME);

            classifyCollectionName = DEFAULT_CLASSIFYCOLLECTIONNAME;
        } else {
            // Make sure the collection name does not exceed the Unauthorized limit.
            if (collectionNameCfg.length() > WLM_MAXIMUM_COLLECTIONNAME_LENGTH) {
                // Issue message stating that we truncated the supplied value.
                Tr.warning(tc,
                           "CONFIG_INVALID_COLLECTIONNAME",
                           collectionNameCfg);

                // Only use the first 18 characters for Unauthorized WLM
                collectionNameCfg = collectionNameCfg.substring(0, WLM_MAXIMUM_COLLECTIONNAME_LENGTH);
            }

            classifyCollectionName = collectionNameCfg;
        }

        // Blank-Pad name
        classifyCollectionName = classifyCollectionName + "                  ".substring(0, WLM_MAXIMUM_COLLECTIONNAME_LENGTH - classifyCollectionName.length());

        // Removed uppercase of transaction class.  We want to pass whatever the config
        // set.  WLM Panels can support mixed case.  tWAS folds it to uppercase, but we
        // think that may be a future APAR.  So, in Liberty just take what they gave us
        // for now and if they want a switch to uppercase it, we will entertain that
        // later.
    }

    /**
     * DS method to activate this component.
     *
     * @param properties
     *
     * @throws Exception
     */
    protected void activate(Map<String, Object> properties) throws Exception {

        updateConfig(properties);

    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason
     *                   int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {

    }

    /**
     * Subsystem to use on WLM Native service invocations
     */
    @Override
    public String getSubSystem() {
        return SUBSYSTEM;
    }

    /**
     * Subsystem name to use on WLM native services
     */
    @Override
    public String getSubSystemName() {
        return SUBSYSTEMNAME;
    }

    /**
     * WLM Create function name used on native services
     */
    @Override
    public String getCreateFunctionName() {
        return CREATE_FUNCTIONNAME;
    }

    /**
     * Collection name to use for WLM native service invocations
     */
    @Override
    public String getClassifyCollectionName() {
        return classifyCollectionName;
    }

    /**
     * Format and write diagnostic information to the supplied OutputStream
     *
     * @parms out OutputStream to write diagnostics
     */
    @Override
    public void writeDiagnostics(OutputStream out) throws IOException {
        StringBuilder sbuilder = new StringBuilder();

        sbuilder.append("\nWLM Configuration Information:" +
                        "\n\tWLM Create Name: '" + this.getCreateFunctionName() + "\'" +
                        "\n\tWLM Classify Collection Name: '" + this.getClassifyCollectionName() + "\'" +
                        "\n\tWLM Subsystem: '" + this.getSubSystem() + "\'" +
                        "\n\tWLM Subsystem Name: '" + this.getSubSystemName() + "\'\n");

        out.write(sbuilder.toString().getBytes());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.wlm.WLMConfigManager#putServiceClassToken(java.lang.String, int)
     */
    @Override
    public void putServiceClassToken(String serviceClassTokenKey, int serviceClassToken) {

        serviceContextTokensMap.put(serviceClassTokenKey, serviceClassToken);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.zos.wlm.WLMConfigManager#getServiceClassToken(java.lang.String)
     */
    @Override
    public int getServiceClassToken(String serviceClassTokenKey) {
        int outputServiceClassToken = 0;
        if (!serviceContextTokensMap.isEmpty()) {
            Integer serviceClassToken = serviceContextTokensMap.get(serviceClassTokenKey);
            if (serviceClassToken != null) {
                outputServiceClassToken = serviceClassToken.intValue();
            }
        }
        return outputServiceClassToken;
    }
}
