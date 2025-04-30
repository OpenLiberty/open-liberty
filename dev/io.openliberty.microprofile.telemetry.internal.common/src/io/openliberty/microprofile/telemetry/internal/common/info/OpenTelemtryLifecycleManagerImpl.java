/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.info;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.function.FailableSupplier;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.feature.LibertyFeature;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;

@Component(service = { ApplicationStateListener.class, OpenTelemetryLifecycleManager.class }, property = { "service.vendor=IBM", "service.ranking:Integer=1500" })
public class OpenTelemtryLifecycleManagerImpl implements ApplicationStateListener, OpenTelemetryLifecycleManager {

    private static final TraceComponent tc = Tr.register(OpenTelemtryLifecycleManagerImpl.class);

    private final MetaDataSlot slotForOpenTelemetryInfoHolder;
    private final OpenTelemetryInfoFactory openTelemetryInfoFactory;

    private final Set<String> warningsEmittedForInternalApps = ConcurrentHashMap.newKeySet();

    boolean telemetry2OrLater;

    //These three are set during activation, and refreshed on checkpoint restore. (runtimeInstance is only set if we need one)
    private volatile boolean waitingForCheckpointRestore = true;
    private volatile boolean isRuntimeEnabled = false; //Always false before a checkpoint restore, so we allways prepare what we need if post-checkpoint we're now in app mode.
    private volatile LazyInitializer<OpenTelemetryInfoInternal> runtimeInstance = null;

    @Activate
    public OpenTelemtryLifecycleManagerImpl(@Reference MetaDataSlotService slotService, @Reference OpenTelemetryInfoFactory openTelemetryInfoFactory,
                                            @Reference(service = LibertyFeature.class, target = "(ibm.featureName=mpTelemetry-*)") ServiceReference<LibertyFeature> featureRef) {

        this.openTelemetryInfoFactory = openTelemetryInfoFactory;
        slotForOpenTelemetryInfoHolder = slotService.reserveMetaDataSlot(ApplicationMetaData.class);

        String shortName = (String) featureRef.getProperty("ibm.featureName");
        //the runtimeMode was added in 2.0
        telemetry2OrLater = shortName.startsWith("mpTelemetry-1") ? false : true;

        checkThenSetRuntimeFields();
    }

    private void checkThenSetRuntimeFields() {
        if (waitingForCheckpointRestore) {
            synchronized (this) {
                //restored() true if checkpoint has been restored or was never active
                if (waitingForCheckpointRestore && CheckpointPhase.getPhase().restored()) {
                    HashMap<String, String> propreties = OpenTelemetryPropertiesReader.getRuntimeInstanceTelemetryProperties();
                    isRuntimeEnabled = telemetry2OrLater && OpenTelemetryPropertiesReader.isEnabled(propreties);

                    if (isRuntimeEnabled) {
                        runtimeInstance = LazyInitializer.<OpenTelemetryInfoInternal> builder().setInitializer(curryInfoFactory(isRuntimeEnabled)).get();
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Configured runtime mode as ", isRuntimeEnabled);
                    }

                    waitingForCheckpointRestore = false;
                }
            }
        }
    }

    private FailableSupplier<OpenTelemetryInfoInternal, ? extends Exception> curryInfoFactory(final boolean runtimeEnabled) {
        return () -> {
            return openTelemetryInfoFactory.createOpenTelemetryInfo(runtimeEnabled);
        };
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        checkThenSetRuntimeFields();

        //We do not actually initialize on application starting, we do that lazily if this is needed.

        //We don't use app scoped OpenTelemetry objects if the server scoped object exists
        if (isRuntimeEnabled) {

            //Log a warning if we're in runtime mode but app mode would think we should disable Open Telemetry
            //the only way app mode can runtime mode can disagree is if a value is set in server.xml
            //otherwise the only other config source that is not shared between them is lower priority
            //than config sources that are shared between them.

            //We check the inverse condition inside the InfoFactory to avoid reading the properties twice

            HashMap<String, String> propreties = OpenTelemetryPropertiesReader.getTelemetryProperties();
            if (OpenTelemetryPropertiesReader.checkExplicitlyDisabled(propreties)) {

                String appName = appInfo.getName();
                Tr.warning(tc, "CWMOT5006.tel.enabled.conflict", appName);
            }

            return;
        }

        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        OpenTelemetryInfoReference oTelRef = (OpenTelemetryInfoReference) extAppInfo.getMetaData().getMetaData(slotForOpenTelemetryInfoHolder);

        LazyInitializer<OpenTelemetryInfoInternal> newSupplier = LazyInitializer.<OpenTelemetryInfoInternal> builder().setInitializer(curryInfoFactory(isRuntimeEnabled))
                                                                                .setCloser(info -> info.dispose()).get();

        if (oTelRef == null) {
            oTelRef = new OpenTelemetryInfoReference();
        }

        oTelRef.set(newSupplier);
        extAppInfo.getMetaData().setMetaData(slotForOpenTelemetryInfoHolder, oTelRef);
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {} //no-op

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {} //no-op

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        checkThenSetRuntimeFields();

        //We don't use app scoped OpenTelemetry objects if the server scoped object exists
        if (isRuntimeEnabled) {
            return;
        }

        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        OpenTelemetryInfoReference oTelRef = (OpenTelemetryInfoReference) extAppInfo.getMetaData().getMetaData(slotForOpenTelemetryInfoHolder);

        LazyInitializer<OpenTelemetryInfoInternal> newSupplier = LazyInitializer.<OpenTelemetryInfoInternal> builder()
                                                                                .setInitializer(openTelemetryInfoFactory::createDisposedOpenTelemetryInfo)
                                                                                .setCloser(info -> info.dispose()).get();

        LazyInitializer<OpenTelemetryInfoInternal> oldSupplier = oTelRef.getAndSet(newSupplier);

        try {
            oldSupplier.close();
        } catch (Exception e) {
            Tr.warning(tc, "applicationStopped", "failed to dispose of OpenTelemetry");//TODO better message
        }

    }

    /*
     * There are two race conditions we need to protect against.
     *
     * On app startup/shutdown we need to swap the holder associated with the application.
     * We protect against race conditions here by using an AtomicReference.
     *
     * Within the context of an application's lifecycle we need to ensure OpenTelemetryInfo
     * is only created once. LazySupplier handles this.
     */
    private class OpenTelemetryInfoReference extends AtomicReference<LazyInitializer<OpenTelemetryInfoInternal>> {

        private static final long serialVersionUID = -4884222080590544495L;
    }

    /** {@inheritDoc} */
    @Override
    public OpenTelemetryInfoInternal getOpenTelemetryInfo() {
        try {
            ApplicationMetaData metaData = null;
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cmd != null) {
                metaData = cmd.getModuleMetaData().getApplicationMetaData();
            }
            return getOpenTelemetryInfo(metaData);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OpenTelemetryInfoInternal getOpenTelemetryInfo(ApplicationMetaData metaData) {
        checkThenSetRuntimeFields();

        if (waitingForCheckpointRestore) {
            //We always return a no-op when waiting for checkpoint
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a disabled OTEL instance because we are waiting for a checkpoint restore .");
            }
            return openTelemetryInfoFactory.createDisabledOpenTelemetryInfo();
        }

        //Return runtime instance if it exists, otherwise return the app instance.
        if (isRuntimeEnabled) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning {0} OTEL instance.", OpenTelemetryConstants.OTEL_RUNTIME_INSTANCE_NAME);
            }
            try {
                return runtimeInstance.get();
            } catch (Exception e) {
                Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
                return new ErrorOpenTelemetryInfo();
            }
        }

        //If the runtime is disabled things that expect the runtime instance like the logging may try to acquire an
        //OTel anyway - they shouldn't, they should check if its enabled first and not grab an OpenTelemetry if its not
        //for performance - but they might so return a null.
        if (metaData == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a disabled OTEL instance because metadata was null");
            }
            return new DisabledOpenTelemetryInfo();
        }

        try {
            OpenTelemetryInfoReference atomicRef = (OpenTelemetryInfoReference) metaData.getMetaData(slotForOpenTelemetryInfoHolder);
            if (atomicRef == null) {
                //If this is triggered by a WAB or internal code that doesn't have an associated app, and we're in app mode, return a no-op
                //(in runtime mode we do want to have OpenTelemetry enabled for internal code, e.g., for tracing)
                //Issue CWMOT5100 only once for WAB or internal code.
                String j2EEName = metaData.getJ2EEName().toString();

                if (j2EEName != null && warningsEmittedForInternalApps.add(j2EEName)) {
                    Tr.info(tc, "CWMOT5100.tracing.is.disabled", j2EEName);
                }

                return new DisabledOpenTelemetryInfo();

            }
            LazyInitializer<OpenTelemetryInfoInternal> supplier = atomicRef.get();
            return supplier.get();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    @Override
    public boolean isRuntimeEnabled() {
        checkThenSetRuntimeFields();

        return isRuntimeEnabled;
    }
}
