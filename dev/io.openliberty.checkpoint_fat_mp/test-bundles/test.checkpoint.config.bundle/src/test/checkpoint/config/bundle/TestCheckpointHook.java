/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.condition.Condition;

import com.ibm.websphere.ras.Traceable;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@Component(property = { CheckpointHook.MULTI_THREADED_HOOK + ":Boolean=true" },
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = { "checkpoint.pid" },
           immediate = true)
public class TestCheckpointHook implements CheckpointHook {
    private final ConcurrentHashMap<String, Object> config;
    private final CheckpointPhase phase;
    private final ServiceReference<CheckpointPhase> phaseRef;
    private volatile ServiceReference<Condition> runningCondition = null;
    private volatile ServiceReference<Condition> beforeCheckpointCondition = null;

    @Activate
    public TestCheckpointHook(Map<String, Object> config,
                              @Reference CheckpointPhase phase,
                              @Reference(service = CheckpointPhase.class) ServiceReference<CheckpointPhase> phaseRef) {
        this.config = new ConcurrentHashMap<String, Object>(config);
        this.phase = phase;
        this.phaseRef = phaseRef;
        System.out.println("TESTING - initial " + getConfig());
        System.out.println("TESTING - ProtectedString prepare " + getPassword());
    }

    @Activate
    void activate() {
        System.out.println("TESTING - activate running condition: " + getRunningCondition());
        System.out.println("TESTING - activate before checkpoint condition: " + getBeforeCheckpointCondition());
    }

    @Reference(service = Condition.class, //
               policy = ReferencePolicy.DYNAMIC, //
               cardinality = ReferenceCardinality.OPTIONAL, //
               target = "(" + Condition.CONDITION_ID + "=" + CheckpointPhase.CONDITION_PROCESS_RUNNING_ID + ")")
    protected void setRunningCondition(ServiceReference<Condition> runningCondition) {
        this.runningCondition = runningCondition;
        System.out.println("TESTING - bind running condition: " + getRunningCondition());
    }

    protected void unsetRunningCondition(ServiceReference<Condition> runningCondition) {
        this.runningCondition = null;
    }

    @Reference(service = Condition.class, //
               policy = ReferencePolicy.DYNAMIC, //
               cardinality = ReferenceCardinality.OPTIONAL, //
               target = "(" + Condition.CONDITION_ID + "=" + CheckpointPhase.CONDITION_BEFORE_CHECKPOINT_ID + ")")
    protected void setBeforeCheckpointCondition(ServiceReference<Condition> beforeCheckpointCondition) {
        this.beforeCheckpointCondition = beforeCheckpointCondition;
        System.out.println("TESTING - bind before checkpoint condition: " + getBeforeCheckpointCondition());
    }

    protected void unsetBeforeCheckpointCondition(ServiceReference<Condition> runningCondition) {
        this.beforeCheckpointCondition = null;
    }

    /**
     * @return
     */
    private String getRunningCondition() {
        if (runningCondition == null) {
            return "null";
        } else {
            return runningCondition.getProperty(Condition.CONDITION_ID) + " " + runningCondition.getProperty(CheckpointPhase.CHECKPOINT_PROPERTY);
        }
    }

    private String getBeforeCheckpointCondition() {
        if (beforeCheckpointCondition == null) {
            return "null";
        } else {
            return (String) beforeCheckpointCondition.getProperty(Condition.CONDITION_ID);
        }
    }

    @Modified
    public void modifiedConfig(Map<String, Object> modified) {
        config.putAll(modified);
        System.out.println("TESTING - modified " + getConfig());
    }

    @Override
    public void prepare() {
        System.out.println("TESTING - prepare " + getConfig());
        System.out.println("TESTING - in prepare method RESTORED - " + phase.restored() + " -- " + phaseRef.getProperty(CheckpointPhase.CHECKPOINT_RESTORED_PROPERTY));
        System.out.println("TESTING - in prepare envs - " + //
                           System.getenv("TEST_IMMUTABLE_KEY1") + " - " + //
                           System.getenv("TEST_IMMUTABLE_KEY2") + " - " + //
                           System.getenv("TEST_MUTABLE_KEY3") + " - " + //
                           System.getenv("TEST_MUTABLE_KEY4"));
        System.out.println("TESTING - prepare running condition: " + getRunningCondition());
        if (config.get("fail.prepare") != null) {
            throw new IllegalStateException("TESTING - prepare hook fails.");
        }
        System.out.println("TESTING - ProtectedString prepare " + getPassword());
    }

    @Override
    public void restore() {
        System.out.println("TESTING - restore " + getConfig());
        System.out.println("TESTING - in restore method RESTORED - " + phase.restored() + " -- " + phaseRef.getProperty(CheckpointPhase.CHECKPOINT_RESTORED_PROPERTY));
        System.out.println("TESTING - in restore envs - " + //
                           System.getenv("TEST_IMMUTABLE_KEY1") + " - " + //
                           System.getenv("TEST_IMMUTABLE_KEY2") + " - " + //
                           System.getenv("TEST_MUTABLE_KEY3") + " - " + //
                           System.getenv("TEST_MUTABLE_KEY4"));
        System.out.println("TESTING - restore running condition: " + getRunningCondition());
        if (config.get("fail.restore") != null) {
            throw new IllegalStateException("TESTING - restore hook fails.");
        }
        System.out.println("TESTING - ProtectedString restore " + getPassword());
    }

    private String getConfig() {
        return "config: a=" + config.get("a") + " b=" + config.get("b") + " c=" + config.get("c") + " d=" + config.get("d");
    }

    private String getPassword() {
        return "password: " + ((Traceable) config.get("password")).toTraceString();
    }
}
