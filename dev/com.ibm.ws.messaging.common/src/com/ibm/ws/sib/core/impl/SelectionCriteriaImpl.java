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
package com.ibm.ws.sib.core.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * This class contains the implementation of the SelectionCriteria from the CoreSPI.
 */
public class SelectionCriteriaImpl implements SelectionCriteria
{
    //trace
    private static final TraceComponent tc =
                    SibTr.register(
                                   SelectionCriteriaImpl.class,
                                   SICoreConstants.CORE_TRACE_GROUP,
                                   SICoreConstants.RESOURCE_BUNDLE);

    private String discriminator = null;
    private String selectorString = null;
    private SelectorDomain domain = null;

    /**
     * Create a new SelectionCriteriaImpl object
     */
    public SelectionCriteriaImpl(String discriminator,
                                 String selectorString,
                                 SelectorDomain selectorDomain)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "SelectionCriteriaImpl",
                        new Object[] { discriminator, selectorString, selectorDomain });

        this.discriminator = discriminator;

        if (selectorString == null || selectorString.trim().length() == 0)
            this.selectorString = null;
        else
            this.selectorString = selectorString;

        domain = selectorDomain;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "SelectionCriteriaImpl");
    }

    /**
     * @return
     */
    @Override
    public SelectorDomain getSelectorDomain() {
        return domain;
    }

    /**
     * @return
     */
    @Override
    public String getSelectorString() {
        return selectorString;
    }

    /**
     * @param domain
     */
    @Override
    public void setSelectorDomain(SelectorDomain domain) {
        this.domain = domain;
    }

    /**
     * @param string
     */
    @Override
    public void setSelectorString(String string) {
        selectorString = string;
    }

    /**
     * @return
     */
    @Override
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * @param string
     */
    @Override
    public void setDiscriminator(String string) {
        discriminator = string;
    }

    /**
     * equals method.
     */
    @Override
    public boolean equals(Object other)
    {
        boolean res = false;
        if (other instanceof SelectionCriteriaImpl)
        {
            SelectionCriteriaImpl otherCriteria = (SelectionCriteriaImpl) other;
            if (otherCriteria.discriminator == null)
            {
                if (discriminator == null)
                {
                    if (otherCriteria.selectorString == null)
                    {
                        if (selectorString == null)
                            res = true; // Both are null
                    }
                    else
                    {
                        // Non null other selector
                        if (selectorString != null && selectorString.equals(otherCriteria.selectorString))
                        {
                            // Go on to check selector domain
                            if (domain.equals(otherCriteria.domain))
                                res = true; // Null discriminator, nonnull string        
                        }
                    }
                }
            }
            else // other discriminator is non null
            {
                if (discriminator != null && discriminator.equals(otherCriteria.discriminator))
                {
                    // Discriminators are equal
                    if (otherCriteria.selectorString == null)
                    {
                        if (selectorString == null)
                            res = true; // Both selectors are null
                    }
                    else
                    {
                        // Non null other selector
                        if (selectorString != null && selectorString.equals(otherCriteria.selectorString))
                        {
                            // Go on to check selector domain
                            if (domain.equals(otherCriteria.domain))
                                res = true; // nonnull discriminator, nonnull string        
                        }
                    }
                }
            }
        }
        return res;
    }

    @Override
    public String toString()
    {
        String retString = discriminator + " : " + selectorString + " : " + domain;

        return retString;
    }
}
