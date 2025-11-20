/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.server.internal;

import static com.ibm.wsspi.kernel.service.condition.StartPhaseCondition.StartPhase.getByActiveLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.condition.Condition;

import com.ibm.wsspi.kernel.service.condition.StartPhaseCondition;
import com.ibm.wsspi.kernel.service.condition.StartPhaseCondition.StartPhase;

/**
 *
 */
@Component
public class StartPhaseConditionManager {
    private final ServiceRegistration<Condition> startPhaseConditionReg;
    private final FrameworkStartLevel frameworkStartLevel;
    private final AtomicReference<StartPhaseCondition.StartPhase> current = new AtomicReference<>(StartPhaseCondition.StartPhase.PREPARE);
    private final SynchronousBundleListener bundleListener;

    @Activate
    public StartPhaseConditionManager(BundleContext context) {
        frameworkStartLevel = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkStartLevel.class);
        startPhaseConditionReg = context.registerService(Condition.class, Condition.INSTANCE,
                                                         FrameworkUtil.asDictionary(getConditionProps(current.get())));

        BinaryOperator<StartPhase> phaseUpdater = new BinaryOperator<StartPhase>() {
            @Override
            public StartPhase apply(final StartPhase c, final StartPhase n) {
                if (c == n) {
                    // do nothing
                    return n;
                }
                StartPhase current = c;
                while (current != n) {
                    if (current.level() < n.level()) {
                        current = current.next();
                    } else {
                        current = current.previous();
                    }
                    if (c == null) {
                        // should never happen
                        throw new IllegalStateException();
                    }
                    startPhaseConditionReg.setProperties(FrameworkUtil.asDictionary(getConditionProps(current)));
                }
                return n;
            }
        };

        bundleListener = new SynchronousBundleListener() {
            @Override
            public void bundleChanged(BundleEvent e) {
                if ((e.getType() & (BundleEvent.STARTING | BundleEvent.STOPPING)) != 0) {
                    StartPhaseCondition.StartPhase newPhase = getByActiveLevel(frameworkStartLevel.getStartLevel());
                    StartPhase oldPhase = current.getAndSet(newPhase);
                    if (oldPhase != newPhase) {
                        phaseUpdater.apply(oldPhase, newPhase);
                    }
                }
            }
        };
        context.addBundleListener(bundleListener);
    }

    static Map<String, Object> getConditionProps(StartPhase phase) {
        Map<String, Object> result = new HashMap<>();
        result.put(Condition.CONDITION_ID, StartPhaseCondition.ID_NAME);
        result.put(StartPhaseCondition.ID_NAME, phase);
        return result;
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        context.removeBundleListener(bundleListener);
        startPhaseConditionReg.unregister();
    }
}
