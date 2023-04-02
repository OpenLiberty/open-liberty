/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.core.impl;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static com.ibm.ws.sib.utils.ras.SibTr.exit;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * @author Neil Young
 */
public enum SelectionCriteriaFactoryImpl implements SelectionCriteriaFactory {
    INSTANCE;

    private static final TraceComponent tc =    SibTr.register(SelectionCriteriaFactoryImpl.class, SICoreConstants.CORE_TRACE_GROUP, SICoreConstants.RESOURCE_BUNDLE);

    public SelectionCriteria createSelectionCriteria() throws SIErrorException {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, "createSelectionCriteria" );

        SelectionCriteria criteria = new SelectionCriteriaImpl();

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(tc, "createSelectionCriteria", criteria);

        return criteria;
    }

    public SelectionCriteria createSelectionCriteria(String discriminator, String selectorString, SelectorDomain selectorDomain) throws SIErrorException {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, "createSelectionCriteria", new Object[]{discriminator, selectorString, selectorDomain});

        SelectionCriteria criteria = new SelectionCriteriaImpl(discriminator, selectorString, selectorDomain);

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(tc, "createSelectionCriteria", criteria);

        return criteria;
    }
}
