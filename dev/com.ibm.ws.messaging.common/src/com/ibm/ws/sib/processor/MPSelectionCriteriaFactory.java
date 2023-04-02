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
package com.ibm.ws.sib.processor;

import java.util.Map;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.sib.core.SelectorDomain;

public abstract class MPSelectionCriteriaFactory {
    private final static String MP_MESSAGE_SELECTOR_FACTORY_CLASS = "com.ibm.ws.sib.processor.matching.MPSelectionCriteriaFactoryImpl";

    /**
     * Get a MPSelectionCriteriaFactory which is to be used for creating
     * MPSelectionCriteria instances.
     *
     * @return The MPSelectionCriteriaFactory
     *
     * @exception SIErrorException The method propagates any Exception caught during
     *                             creation of the singleton factory.
     */
    public static <T extends MPSelectionCriteriaFactory> MPSelectionCriteriaFactory getInstance() {
        try {
            @SuppressWarnings("unchecked")
            Class<T> cls = (Class<T>) Class.forName(MP_MESSAGE_SELECTOR_FACTORY_CLASS);
            return cls.getConstructor().newInstance();
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.processor.MPSelectionCriteriaFactory.createFactoryInstance", "100");
            throw new SIErrorException(e);
        }
    }

    /**
     * Creates a new MPSelectionCriteria, an overload of the
     * SelectionCriteriaFactory method. Takes a Map parameter that can be used to
     * associate properties with the selector.
     *
     * @param discriminator      the discriminator
     * @param selectorString     the string selector expression
     * @param selectorDomain     the type of domain in which the selector is being
     *                           created
     * @param selectorProperties a map of additional properties associated with the
     *                           selector
     * @return a new SelectionCriteria
     *
     * @see com.ibm.wsspi.sib.core.SelectionCriteria
     */
    public abstract MPSelectionCriteria createSelectionCriteria(
            String discriminator,
            String selectorString,
            SelectorDomain selectorDomain,
            Map<String, Object> selectorProperties);

}
