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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import io.openliberty.checkpoint.spi.CheckpointHook;

/**
 *
 */
@Component(property = { CheckpointHook.MULTI_THREADED_HOOK + ":Boolean=true" },
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = { "checkpoint.pid.a", "checkpoint.pid.b" },
           immediate = true)
public class TestCheckpointHook implements CheckpointHook {
    ConcurrentHashMap<String, Object> config;

    @Activate
    public TestCheckpointHook(Map<String, Object> config) {
        this.config = new ConcurrentHashMap<String, Object>(config);
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
    }

    @Override
    public void restore() {
        System.out.println("TESTING - restore " + getConfig());
    }

    private String getConfig() {
        return "config: pida=" + config.get("pida") + " pidb=" + config.get("pidb");
    }
}
