/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.wlm.WLMConfigManager;
import com.ibm.ws.zos.wlm.WLMHealthConstants;

/**
 * Service that represents the <zosWlmHealth> configuration element.
 * Updates to the config element are pushed to ZosHealthAPI
 * and MessageRouterConfigListener services.
 */
@Component(name = "com.ibm.ws.zos.wlm.WLMHealth",
           service = WLMHealthImpl.class,
           immediate = true,
           property = { "service.vendor = IBM" })
public class WLMHealthImpl {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(WLMHealthImpl.class, "zOSWLMServices", "com.ibm.ws.zos.wlm.internal.resources.ZWLMMessages");

    /**
     * Native method manager DS injected reference.
     */
    protected NativeMethodManager nativeMethodManager = null;

    /**
     * WLM configuration manager DS injected reference.
     */
    protected WLMConfigManager wlmConfigManager = null;

    /**
     * The latest values of the <zosWlmHealth> config parameters.
     */
    protected volatile static Hashtable<String, Object> zosHealthConfig = null;

    /**
     * ScheduleExecutorService - for scheduling the recurring task to increase the WLM health.
     */
    private static ScheduledExecutorService executor;

    /**
     * ScheduledFuture - Current scheduled task to update WLM health.
     */
    private volatile static ScheduledFuture<?> futureTask = null;

    /**
     * Current WLM health tracker
     */
    protected static volatile AtomicInteger currentHealth = new AtomicInteger(0);

    /**
     * Use to check only if both interval & increment were set by default value, disable issuing message
     */
    private static volatile boolean isUpdateByDefault = false;

    /**
     * Activate and register with native WLM health update method,
     * then set configuration, then start increasing WLM health
     *
     * @param config Configuration set by <zosWLMHealth> property
     */
    @Activate
    protected void activate(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this + " with configuration:");

            for (Entry<String, Object> entry : config.entrySet()) {
                Tr.event(tc, "", "Key: " + entry.getKey().toString() + " Value: " + entry.getValue().toString());
            }
        }

        try {
            // Attempt to load native code via the method manager.
            nativeMethodManager.registerNatives(WLMHealthImpl.class);
            zosHealthConfig = new Hashtable<String, Object>(config);
            updateConfig();
            resetHealthLevel();
            increaseWlmHealth();
        } catch (Exception ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception in Activating", ex);
            }
        }
    }

    /**
     * Deactivate by clearing saved config and current health level, then cancel any new schedule
     *
     * @param config
     * @param reason
     */
    @Deactivate
    protected void deactivate(Map<String, Object> config, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
        resetHealthLevel();
        zosHealthConfig = null;
        if (futureTask != null) {
            futureTask.cancel(true);
            futureTask = null;
        }
    }

    /**
     * Store new config values and update health update parameters.
     *
     * @param config
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Modified " + this);
        }
        if (futureTask != null) {
            futureTask.cancel(true);
        }
        zosHealthConfig = new Hashtable<String, Object>(config);
        updateConfig();
        resetHealthLevel();
        increaseWlmHealth();
    }

    /**
     * Valid and set the current config values based on applied configuration.
     */
    private void updateConfig() {
        if (zosHealthConfig != null) {
            if (zosHealthConfig.containsKey(WLMHealthConstants.zosHealthInterval)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Updating " + WLMHealthConstants.zosHealthInterval);
                }
                long intervalL = WLMHealthConstants.INTERVAL_DEFAULT;
                try {
                    intervalL = Long.valueOf(zosHealthConfig.get(WLMHealthConstants.zosHealthInterval).toString());
                    intervalL = (intervalL < WLMHealthConstants.INTERVAL_DEFAULT) ? WLMHealthConstants.INTERVAL_DEFAULT : intervalL;
                } catch (NumberFormatException ex) {
                    intervalL = WLMHealthConstants.INTERVAL_DEFAULT;
                    throw new NumberFormatException("Invaild health monitor property <interval> received, expecting a numerical input for <interval>.");
                } catch (Exception ex) {
                    intervalL = WLMHealthConstants.INTERVAL_DEFAULT;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid WLM Health configuration interval received, overide by default: " + WLMHealthConstants.INTERVAL_DEFAULT);
                    }
                } finally {
                    if (intervalL == WLMHealthConstants.INTERVAL_DEFAULT) {
                        isUpdateByDefault = true;
                    } else {
                        isUpdateByDefault = false;
                    }
                    zosHealthConfig.put(WLMHealthConstants.zosHealthInterval, intervalL);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, WLMHealthConstants.zosHealthInterval + " is set to " + intervalL);
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "No interval key detacted in the configuration, setting by default value.");
                }
                zosHealthConfig.put(WLMHealthConstants.zosHealthInterval, WLMHealthConstants.INTERVAL_DEFAULT);
            }

            if (zosHealthConfig.containsKey(WLMHealthConstants.zosHealthIncrement)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Updating " + WLMHealthConstants.zosHealthIncrement);
                }
                Integer incrementInt = WLMHealthConstants.INCREMENT_DEFAULT;
                try {
                    incrementInt = Integer.valueOf(zosHealthConfig.get(WLMHealthConstants.zosHealthIncrement).toString());
                } catch (Exception ex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "", ex);
                    }
                } finally {
                    if (incrementInt < WLMHealthConstants.INCREMENT_MIN
                        || incrementInt > WLMHealthConstants.INCREMENT_DEFAULT) {
                        incrementInt = WLMHealthConstants.INCREMENT_DEFAULT;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "WLM Health configuration increment value is out of range, set by default value of " + WLMHealthConstants.INCREMENT_DEFAULT);
                        }
                        // if intervalL == WLMHealthConstants.INTERVAL_DEFAULT &&
                        //      incrementInt == WLMHealthConstants.INCREMENT_DEFAULT
                        if (isUpdateByDefault && (incrementInt.equals(WLMHealthConstants.INCREMENT_DEFAULT))) {
                            isUpdateByDefault = true;
                        } else {
                            isUpdateByDefault = false;
                        }
                    }
                    zosHealthConfig.put(WLMHealthConstants.zosHealthIncrement, incrementInt);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, WLMHealthConstants.zosHealthIncrement + " is set to " + incrementInt);
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "No increment key detacted in the configuration, setting by default value.");
                }
                zosHealthConfig.put(WLMHealthConstants.zosHealthIncrement, WLMHealthConstants.INCREMENT_DEFAULT);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                for (Entry<String, Object> entry : zosHealthConfig.entrySet()) {
                    Tr.event(tc, "", "Key: " + entry.getKey().toString() + " Value: " + entry.getValue().toString());
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "zosHealthConfig is null, failed to update.");
            }
        }
    }

    @Reference(service = ScheduledExecutorService.class)
    protected void setExecutor(ScheduledExecutorService executor) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting ScheduledExecutorService.");
        }
        WLMHealthImpl.executor = executor;
    }

    protected void unsetExecutor(ScheduledExecutorService executor) {
        if (WLMHealthImpl.executor == executor) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unsetting ScheduledExecutorService.");
            }
            executor = null;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Mismatched ScheduledExecutorService failed to unset it.");
            }
        }
    }

    /**
     * Start a schedule with no delay, calls the HealthIncrementer to update health once.
     */
    private void increaseWlmHealth() {
        try {
            if (executor != null) {
                futureTask = executor.schedule(new HealthIncrementer(), 0, TimeUnit.SECONDS);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ScheduledExecutorService is not set.");
                }
            }
        } catch (RejectedExecutionException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "", e);
            }
        } catch (NullPointerException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "", e);
            }
        }
    }

    class HealthIncrementer implements Runnable {
        private final TraceComponent tc = Tr.register(HealthIncrementer.class, "zOSWLMServices", "com.ibm.ws.zos.wlm.internal.resources.ZWLMMessages");

//        public HealthIncrementer() {
//            super();
//        }

        @Override
        public synchronized void run() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Start to increase WLM Health.");
            }
            if (currentHealth.get() < WLMHealthConstants.HEALTH_MAX) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Current health is " + currentHealth.get());
                }
                try {
                    // get next update value
                    Integer newHealthInt = Integer.valueOf(zosHealthConfig.get(WLMHealthConstants.zosHealthIncrement).toString());
                    int newHealth = currentHealth.addAndGet(newHealthInt);
                    // set to max if overflow
                    if (newHealth > WLMHealthConstants.HEALTH_MAX) {
                        newHealth = WLMHealthConstants.HEALTH_MAX;
                    }
                    currentHealth.set(newHealth);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Trying increase to " + currentHealth.get());
                    }

                    try {
                        int rc = ntv_updateHealth(currentHealth.get());

                        if (rc == 0) {
                            // When config was set with default values, we do issue any message
                            if (!isUpdateByDefault) {
                                Tr.info(tc, "WLM_CURRENT_HEALTH_PERCENTAGE", currentHealth.get());
                            }

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, "Health increased by : " + zosHealthConfig.get(WLMHealthConstants.zosHealthIncrement) + ", Current health : " + currentHealth);
                            }
                            // start schedule next update with delay of interval if updated health is still less than max
                            if (currentHealth.get() < WLMHealthConstants.HEALTH_MAX) {
                                if (executor != null) {
                                    try {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                            Tr.event(tc, "Scheduling next health update in "
                                                         + (long) (Double.parseDouble(zosHealthConfig.get(WLMHealthConstants.zosHealthInterval).toString())) + "ms");
                                        }
                                        futureTask = executor.schedule(new HealthIncrementer(),
                                                                       (long) (Double.parseDouble(zosHealthConfig.get(WLMHealthConstants.zosHealthInterval).toString())),
                                                                       TimeUnit.MILLISECONDS);
                                    } catch (Exception e) {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                            Tr.event(tc, "", e);
                                        }
                                    }
                                } else {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                        Tr.event(tc, " Failed schedule future WLM Health update because executor is null.");
                                    }
                                }
                            }
//                            else {
//                                currentHealth.set(WLMHealthConstants.HEALTH_MAX);
//                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, " Failed to increase WLM health via ntv method, reason : " + rc);
                            }
                        }
                    } catch (Exception ex) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception catched in ntv_updateHealth: ", ex);
                        }
                    }
                } catch (Exception ex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "", ex);
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, " WLM Health is already at " + WLMHealthConstants.HEALTH_MAX);
                }
            }
        }
    };

    /**
     * Reset WLM health level to 0
     */
    private void resetHealthLevel() {
        currentHealth.set(0);
    }

    /**
     * Sets the reference to a NativeMethodManager instance. Called by DS.
     *
     * @param nativeMethodManager The NativeMethodManager instance to unset.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Unsets the reference to a NativeMethodManager instance. Called by DS.
     *
     * @param nativeMethodManager The NativeMethodManager instance to unset.
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * Call to native code to increas WLM Health.
     *
     * @return 0 on success; non-zero on error
     */
    protected native static int ntv_updateHealth(int currentHealth);
}
