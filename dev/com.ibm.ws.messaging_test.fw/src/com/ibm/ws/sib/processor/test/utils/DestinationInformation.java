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
package com.ibm.ws.sib.processor.test.utils;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLocalizationDefinition;

// Remote destination definitions are tracked using the following structure
public class DestinationInformation {
    private final DestinationDefinition dDefinition; // Returned by the Transformer
    // during addTarget

    /**
     * The set of ME uuids which localize the queue point for this destination.
     * (including the local ME if necessary)
     */
    private final Set<String> _localisingQueuePointMEuuidSet; // set of stringified
    // SIBUuid8

    /**
     * The definition of the queue point localization. (contains settings specific
     * to this instance of the queue point)
     */
    private LocalizationDefinition _queuePointDef = null;
    private final MQLocalizationDefinition mqDef = null;

    /*
     * DestinationInformation( DestinationDefinition dDefinition, Set
     * remoteMEuuidSet) { this.dDefinition = dDefinition; this.remoteMEuuidSet =
     * remoteMEuuidSet; }
     */

    public DestinationInformation(DestinationDefinition dDefinition,
                                  Set<String> localisingQueuePointMEuuidSet,
                                  LocalizationDefinition queuePointDef) {
        this.dDefinition = dDefinition;
        this._localisingQueuePointMEuuidSet = new HashSet<String>(localisingQueuePointMEuuidSet);

        this._queuePointDef = queuePointDef;

    }

    /**
     * Returns the dDefinition.
     * 
     * @return DestinationDefinition
     */
    public DestinationDefinition getDDefinition() {
        return dDefinition;
    }

    /**
     * Returns the set of MEs which localise the queue point for this destination.
     * May include the local ME if necessary.
     * 
     * @return set of SIBUuid8's
     */
    public Set<String> getLocalisingQueuePointMEuuidSet() {
        return _localisingQueuePointMEuuidSet;
    }

    /**
     * Returns the definition of the particular instance of queue point local to
     * this ME, if the queue point is local to this ME. If not, null is returned.
     * 
     * @return
     */
    public LocalizationDefinition getQueuePointDefinition() {
        return _queuePointDef;
    }

    public MQLocalizationDefinition getMQDefinition() {
        return mqDef;
    }

}
