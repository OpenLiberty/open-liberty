/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.condition.Condition;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;

import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component
public class TMCheckpointService {

    private volatile ServiceReference<Condition> _runningCondition = null;

    @Activate
    void activate() {
    }

    @Reference(service = Condition.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.OPTIONAL,
               target = "(" + Condition.CONDITION_ID + "=" + CheckpointPhase.CONDITION_PROCESS_RUNNING_ID + ")")
    protected void setRunningCondition(ServiceReference<Condition> runningCondition) {
        _runningCondition = runningCondition;

        ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
        if (cp != null && cp instanceof JTMConfigurationProvider) {
            JTMConfigurationProvider jtmCP = (JTMConfigurationProvider) cp;

            // Start the tran manager after config updates
            jtmCP.setRunningCondition(_runningCondition);
        }
    }

    protected void unsetRunningCondition(ServiceReference<Condition> runningCondition) {
        _runningCondition = null;
    }

    private String getRunningCondition() {
        if (_runningCondition == null) {
            return "null";
        } else {
            return _runningCondition.getProperty(Condition.CONDITION_ID) + " " + _runningCondition.getProperty(CheckpointPhase.CHECKPOINT_PROPERTY);
        }
    }

}