/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.internal;

import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.MQLocalizationDefinition;
import com.ibm.wsspi.sib.core.DestinationType;

public final class JsAdminFactoryImpl extends JsAdminFactory {

    //----------------------------------------------------------------------------
    // BaseDestinationDefinition
    //----------------------------------------------------------------------------

    @Override
    public BaseDestinationDefinition createBaseDestinationDefinition(DestinationType type, String name) {
        return new BaseDestinationDefinitionImpl(type, name);
    }

    @Override
    public BaseDestinationDefinition createBaseDestinationDefinition(LWMConfig d) {
        return new BaseDestinationDefinitionImpl(d);
    }

    //----------------------------------------------------------------------------
    // DestinationDefinition
    //----------------------------------------------------------------------------

    @Override
    public DestinationDefinition createDestinationDefinition(DestinationType type, String name) {
        return new DestinationDefinitionImpl(type, name);
    }

    @Override
    public DestinationDefinition createDestinationDefinition(LWMConfig d) {
        return new DestinationDefinitionImpl(d);
    }

    //----------------------------------------------------------------------------
    // DestinationAliasDefinition
    //----------------------------------------------------------------------------

    @Override
    public DestinationAliasDefinition createDestinationAliasDefinition(DestinationType type, String name) {
        return new DestinationAliasDefinitionImpl(type, name);
    }

    @Override
    public DestinationAliasDefinition createDestinationAliasDefinition(LWMConfig d) {
        return new DestinationAliasDefinitionImpl(d);
    }

    //----------------------------------------------------------------------------
    // DestinationForeignDefinition
    //----------------------------------------------------------------------------

    @Override
//    public DestinationForeignDefinition createDestinationForeignDefinition(DestinationType type, String name) {
//        return new DestinationForeignDefinitionImpl(type, name);
//    }
//
//    public DestinationForeignDefinition createDestinationForeignDefinition(ConfigObject d) {
//        return new DestinationForeignDefinitionImpl(d);
//    }
    //----------------------------------------------------------------------------
    // LocalizationDefinition
    //----------------------------------------------------------------------------
    public LocalizationDefinition createLocalizationDefinition(String name) {
        return new LocalizationDefinitionImpl(name);
    }

    @Override
    public LocalizationDefinition createLocalizationDefinition(LWMConfig lp) {
        return new LocalizationDefinitionImpl(lp);
    }

    /** {@inheritDoc} */
    @Override
    public MQLinkDefinition createMQLinkDefinition(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MQLocalizationDefinition createMQLocalizationDefinition(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MQLocalizationDefinition createMQLocalizationDefinition(LWMConfig mqs, LWMConfig bm, LWMConfig lpp) {
        // TODO Auto-generated method stub
        return null;
    }

}