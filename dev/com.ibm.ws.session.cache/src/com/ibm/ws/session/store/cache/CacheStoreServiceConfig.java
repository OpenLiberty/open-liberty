package com.ibm.ws.session.store.cache;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.condition.Condition;

import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(configurationPid = "com.ibm.ws.session.cache", configurationPolicy = ConfigurationPolicy.OPTIONAL, service = CacheStoreServiceConfig.class)
public class CacheStoreServiceConfig {
    private final AtomicReference<ServiceRegistration<Condition>> conditionReg = new AtomicReference<>();
    private final AtomicInteger numUpdates = new AtomicInteger(0);
    private final BundleContext bc;
    private volatile Condition runningCondition = null;
    private volatile Map<String, Object> props;
    @Activate
    public CacheStoreServiceConfig(BundleContext bc, Map<String, Object> props) {
        this.bc = bc;
        this.props = props;
        refreshConfigCondition();
    }

    @Modified
    void modified(Map<String, Object> props) {
        this.props = props;
        if (runningCondition != null) {
            refreshConfigCondition();
        } else {
            updateConfigCondition();
        }
    }

    Map<String, Object> getConfig() {
        return props;
    }

    private void updateConfigCondition() {
        conditionReg.getAndUpdate((reg) -> {
            if (reg != null) {
                Dictionary<String, Object> props = new Hashtable<>();
                props.put(Condition.CONDITION_ID, "session.cache.config");
                props.put("numUpdates", numUpdates.incrementAndGet());
                reg.setProperties(props);
            }
            return reg;
        });
    }

    private void refreshConfigCondition() {
        conditionReg.getAndUpdate((reg) -> {
            if (reg != null) {
                reg.unregister();
            }
            return bc.registerService(Condition.class, Condition.INSTANCE, FrameworkUtil.asDictionary(Collections.singletonMap(Condition.CONDITION_ID, "session.cache.config")));
        });
    }

    @Reference(target = "(" + Condition.CONDITION_ID + "=" + CheckpointPhase.CONDITION_PROCESS_RUNNING_ID + ")",
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY,
                    cardinality = ReferenceCardinality.OPTIONAL)
    void setRunningCondition(Condition runningCondition) {
        this.runningCondition = runningCondition;
    }

    void unsetRunningCondition(Condition runningCondition) {
        this.runningCondition = null;
    }
}
