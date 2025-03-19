/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.messaging.security.internal;

import static org.osgi.framework.FrameworkUtil.asDictionary;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.condition.Condition;

import com.ibm.ws.security.token.ltpa.LTPAConfiguration;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(service = {}, immediate = true)
public class LTPACondition implements Condition{
	static final String LTPA_CONDITION_ID = "com.ibm.ws.security.token.ltpa.LTPAConfiguration";
	static final String LTPA_CONDITION_FILTER = "(" + CONDITION_ID + "=" + LTPA_CONDITION_ID + ")";

	private final AtomicReference<ServiceRegistration<Condition>> ltpaConditionReg = new AtomicReference<>();
	private final boolean waitForLTPAConfiguration;
	private final BundleContext context;
	private final CountDownLatch ltpaOnRestore = new CountDownLatch(1);
	@Activate
	public LTPACondition(BundleContext context) {
		this.context = context;
		boolean beforeCheckpoint = CheckpointPhase.getPhase().addMultiThreadedHook(Integer.MAX_VALUE, new CheckpointHook() {
			@Override
			public void restore() {
				try {
					// wait for ltpa to register before continuing on restore
					ltpaOnRestore.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		// If the hook got registered above then we are on the checkpoint side;
		// If not then we are on the restore side and need to wait for LTPAConfiguration before registering the condition
		waitForLTPAConfiguration = !beforeCheckpoint;
		if (!waitForLTPAConfiguration) {
			// on the checkpoint side; register the condition now to avoid delaying application start on checkpoint side
			ltpaConditionReg.set(context.registerService(Condition.class, LTPACondition.this, asDictionary(singletonMap(CONDITION_ID, LTPA_CONDITION_ID))));
		}
	}

	@Deactivate
	protected void deactivate() {
		unregisterLTPACondition();
	}

	private void unregisterLTPACondition() {
		ServiceRegistration<Condition> reg = ltpaConditionReg.getAndSet(null);
		if (reg != null) {
			reg.unregister();
		}
	}

	@Reference(service = LTPAConfiguration.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	void setLTPAConfiguration(ServiceReference<LTPAConfiguration> ref) {
		if (waitForLTPAConfiguration) {
			// time to register the condition now
			ltpaConditionReg.set(context.registerService(Condition.class, LTPACondition.this, asDictionary(singletonMap(CONDITION_ID, LTPA_CONDITION_ID))));
		}
		// signal to the possible restore hook that LTPAConfigruation is now ready
		ltpaOnRestore.countDown();
	}

	void unsetLTPAConfiguration(ServiceReference<LTPAConfiguration> ref) {
		if (waitForLTPAConfiguration) {
			unregisterLTPACondition();
		}
	}
}
