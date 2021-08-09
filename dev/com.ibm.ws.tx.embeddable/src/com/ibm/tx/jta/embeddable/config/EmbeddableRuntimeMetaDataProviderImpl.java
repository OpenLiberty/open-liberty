/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.jta.embeddable.config;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.RuntimeMetaDataProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class EmbeddableRuntimeMetaDataProviderImpl implements RuntimeMetaDataProvider
{
    private static final TraceComponent tc = Tr.register(EmbeddableRuntimeMetaDataProviderImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final ConfigurationProvider _cp;

    public EmbeddableRuntimeMetaDataProviderImpl(ConfigurationProvider configurationProvider)
    {
        _cp = configurationProvider;
    }

    @Override
    public int getTransactionTimeout()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionTimeout", 0);
        return 0;
    }

    @Override
    public boolean isClientSideJTADemarcationAllowed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isClientSideJTADemarcationAllowed", Boolean.FALSE);
        return false;
    }

    @Override
    public boolean isHeuristicHazardAccepted()
    {
        // Don't support per module LPS enablement in embeddable container
        // so just use configuration provider setting derived from properties
        final boolean ret = _cp.isAcceptHeuristicHazard();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isHeuristicHazardAccepted", ret);
        return ret;
    }

    @Override
    public boolean isUserTransactionLookupPermitted(String name)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isUserTransactionLookupPermitted", Boolean.TRUE);
        return true;
    }
}