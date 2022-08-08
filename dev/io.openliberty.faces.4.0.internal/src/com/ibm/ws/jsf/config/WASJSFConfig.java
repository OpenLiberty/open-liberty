/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.config;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * Holds all the IBM specific configuration init parameters (from web.xml) that
 * are independent from the core implementation. The parameters in this class are available to
 * all shared, component and implementation classes.
 *
 */
public class WASJSFConfig {

    private static final String APPLICATION_MAP_WAS_JSF_CONFIG = WASJSFConfig.class.getName();

    @JSFWebConfigParam(expectedValues = "true, false", defaultValue = "true")
    public final static String DELAY_POST_CONSTRUCT = "com.ibm.ws.jsf.delayManagedBeanPostConstruct";
    public final static boolean DELAY_POST_CONSTRUCT_DEFAULT = true;

    private boolean _delayManagedBeanPostConstruct;

    public WASJSFConfig() {
        setDelayManagedBeanPostConstruct(DELAY_POST_CONSTRUCT_DEFAULT);
    }

    public static WASJSFConfig getCurrentInstance(ExternalContext extCtx) {
        WASJSFConfig wasJSFConfig = (WASJSFConfig) extCtx.getApplicationMap().get(APPLICATION_MAP_WAS_JSF_CONFIG);

        if (wasJSFConfig == null) {
            wasJSFConfig = createAndInitializeWASJSFConfig(extCtx);
            extCtx.getApplicationMap().put(APPLICATION_MAP_WAS_JSF_CONFIG, wasJSFConfig);
        }

        return wasJSFConfig;
    }

    private static WASJSFConfig createAndInitializeWASJSFConfig(ExternalContext extCtx) {
        WASJSFConfig wasJSFConfig = new WASJSFConfig();

        wasJSFConfig.setDelayManagedBeanPostConstruct(WebConfigParamUtils.getBooleanInitParameter(extCtx,
                                                                                                  DELAY_POST_CONSTRUCT, DELAY_POST_CONSTRUCT_DEFAULT));

        return wasJSFConfig;
    }

    public boolean isDelayManagedBeanPostConstruct() {
        return _delayManagedBeanPostConstruct;
    }

    public void setDelayManagedBeanPostConstruct(boolean delayManagedBeanPostConstruct) {
        this._delayManagedBeanPostConstruct = delayManagedBeanPostConstruct;
    }

}
