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

package com.ibm.wsspi.sib.core;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.core.impl.SelectionCriteriaFactoryImpl;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * SelectionCriteriaFactory is used to create SelectionCriteria objects. It is
 * implemented by SIB.processor.
 * <p>
 * This class has no security implications.
 */

public interface SelectionCriteriaFactory {
    public static SelectionCriteriaFactory getInstance() {
        return SelectionCriteriaFactoryImpl.INSTANCE;
    }

    /**
     * Creates a new default SelectionCriteria, that can be used when creating a
     * ConsumerSession or BrowserSession, to support durable subscription creation
     * or when using the receive methods of SICoreConnection, to indicate that
     * messages are to be selected according to a selector expression and/or
     * discriminator that is to be applied to properties in the message.
     * <p>
     * The default SelectionCriteria has a null discriminator, a null selector, and
     * a selectorDomain of SIMESSAGE.
     *
     * @return a new SelectionCriteria
     *
     * @see com.ibm.wsspi.sib.core.SelectionCriteria
     */
    SelectionCriteria createSelectionCriteria();

    /**
     * Creates a new SelectionCriteria, that can be used when creating a
     * ConsumerSession or BrowserSession, to support durable subscription creation
     * or when using the receive methods of SICoreConnection, to indicate that
     * messages are to be selected according to a selector expression and/or
     * discriminator that is to be applied to properties in the message.
     *
     * @param discriminator  the discriminator
     * @param selectorString the string selector expression
     * @param selectorDomain the type of domain in which the selector is being
     *                       created
     *
     * @return a new SelectionCriteria
     *
     * @see com.ibm.wsspi.sib.core.SelectionCriteria
     */
    SelectionCriteria createSelectionCriteria(
            String discriminator,
            String selectorString,
            SelectorDomain selectorDomain);

}
