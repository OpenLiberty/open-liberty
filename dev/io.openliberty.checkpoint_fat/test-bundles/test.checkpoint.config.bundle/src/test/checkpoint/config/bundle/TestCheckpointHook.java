/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.checkpoint.config.bundle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@Component(property = { CheckpointHook.MULTI_THREADED_HOOK + ":Boolean=true" },
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = { "checkpoint.pid.a", "checkpoint.pid.b" },
           immediate = true)
public class TestCheckpointHook implements CheckpointHook {
    private final ConcurrentHashMap<String, Object> config;
    private final CheckpointPhase phase;
    private final ServiceReference<CheckpointPhase> phaseRef;

    @Activate
    public TestCheckpointHook(Map<String, Object> config,
                              @Reference(cardinality = ReferenceCardinality.OPTIONAL) CheckpointPhase phase,
                              @Reference(cardinality = ReferenceCardinality.OPTIONAL, service = CheckpointPhase.class) ServiceReference<CheckpointPhase> phaseRef) {
        this.config = new ConcurrentHashMap<String, Object>(config);
        this.phase = phase;
        this.phaseRef = phaseRef;
        System.out.println("TESTING - initial " + getConfig());
    }

    @Modified
    public void modifiedConfig(Map<String, Object> modified) {
        config.clear();
        config.putAll(modified);
        System.out.println("TESTING - modified " + getConfig());
    }

    @Override
    public void prepare() {
        System.out.println("TESTING - prepare " + getConfig());
        System.out.println("TESTING - in prepare method RESTORED - " + phase.restored() + " -- " + phaseRef.getProperty(CheckpointPhase.CHECKPOINT_RESTORED_PROPERTY));
    }

    @Override
    public void restore() {
        System.out.println("TESTING - restore " + getConfig());
        System.out.println("TESTING - in restore method RESTORED - " + phase.restored() + " -- " + phaseRef.getProperty(CheckpointPhase.CHECKPOINT_RESTORED_PROPERTY));
    }

    private String getConfig() {
        return "config: pida=" + config.get("pida") + " pidb=" + config.get("pidb");
    }
}
