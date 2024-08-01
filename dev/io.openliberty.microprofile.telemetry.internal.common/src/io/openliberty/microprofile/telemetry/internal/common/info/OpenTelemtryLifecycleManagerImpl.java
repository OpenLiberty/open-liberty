/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.info;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.concurrent.LazyInitializer;
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

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;

@Component(service = { ApplicationStateListener.class, OpenTelemetryLifecycleManager.class }, property = { "service.vendor=IBM", "service.ranking:Integer=1500" })
public class OpenTelemtryLifecycleManagerImpl implements ApplicationStateListener, OpenTelemetryLifecycleManager {

    private static final TraceComponent tc = Tr.register(OpenTelemtryLifecycleManagerImpl.class);

    private final MetaDataSlot slotForOpenTelemetryInfoHolder;
    private final EnabledOpenTelemetryInfo otelRuntimeInstance = null;

    LazyInitializer<OpenTelemetryInfo> runtimeInstance = null;

    @Reference
    private OpenTelemetryInfoFactory openTelemetryInfoFactory;

    @Activate
    public OpenTelemtryLifecycleManagerImpl(@Reference MetaDataSlotService slotService) {

        slotForOpenTelemetryInfoHolder = slotService.reserveMetaDataSlot(ApplicationMetaData.class);

        CheckpointHook checkpointHook = new CheckpointHook() {

            @Override
            public void restore() {
                if (openTelemetryInfoFactory.isRuntimeEnabled()) {
                    runtimeInstance = LazyInitializer.<OpenTelemetryInfo> builder().setInitializer(openTelemetryInfoFactory::createOpenTelemetryInfo).get();
                }
            }
        };

        //Create the runtime instance anyway because we do not know if the configuration will enable it later after a checkpoint restore.
        //This does mean we must be very careful about not returning it in app mode because it will silently fail without warning to the customer
        //And we cannot check for this generally because returning it is correct if we're in runtime instance mode and we're pre-checkpoint

        //If checkpoint is enabled this will be replaced by the restore() method in the hook above.
        //If checkpoint is disabled this will be replaced immediately below
        runtimeInstance = LazyInitializer.<OpenTelemetryInfo> builder().setInitializer(openTelemetryInfoFactory::createDisabledOpenTelemetryInfo).get();

        // If checkpoint is enabled this if statement will register the hook we created above, so runtimeInstance will be set after restore. Then it returns true.
        // If checkpoint is disabled this if statement will return false, so runtimeInstance will be set right away.
        if (!CheckpointPhase.getPhase().addMultiThreadedHook(checkpointHook)) {
            if (openTelemetryInfoFactory.isRuntimeEnabled()) {
                runtimeInstance = LazyInitializer.<OpenTelemetryInfo> builder().setInitializer(openTelemetryInfoFactory::createOpenTelemetryInfo).get();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        //We do not actually initialize on application starting, we do that lazily if this is needed.

        //We don't use app scoped OpenTelemetry objects if the server scoped object exists
        if (otelRuntimeInstance != null) {
            return;
        }

        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        OpenTelemetryInfoReference oTelRef = (OpenTelemetryInfoReference) extAppInfo.getMetaData().getMetaData(slotForOpenTelemetryInfoHolder);

        LazyInitializer<OpenTelemetryInfo> newSupplier = LazyInitializer.<OpenTelemetryInfo> builder().setInitializer(openTelemetryInfoFactory::createOpenTelemetryInfo)
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
        //We don't use app scoped OpenTelemetry objects if the server scoped object exists
        if (otelRuntimeInstance != null) {
            return;
        }

        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        OpenTelemetryInfoReference oTelRef = (OpenTelemetryInfoReference) extAppInfo.getMetaData().getMetaData(slotForOpenTelemetryInfoHolder);

        LazyInitializer<OpenTelemetryInfo> newSupplier = LazyInitializer.<OpenTelemetryInfo> builder().setInitializer(openTelemetryInfoFactory::createDisposedOpenTelemetryInfo)
                                                                        .setCloser(info -> info.dispose()).get();

        LazyInitializer<OpenTelemetryInfo> oldSupplier = oTelRef.getAndSet(newSupplier);

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
    private class OpenTelemetryInfoReference extends AtomicReference<LazyInitializer<OpenTelemetryInfo>> {

        private static final long serialVersionUID = -4884222080590544495L;
    }

    /** {@inheritDoc} */
    @Override
    public OpenTelemetryInfo getOpenTelemetryInfo() {
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
    public OpenTelemetryInfo getOpenTelemetryInfo(ApplicationMetaData metaData) {
        //Return runtime instance if it exists, otherwise return the app instance.

        if (openTelemetryInfoFactory.isRuntimeEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning {0} OTEL instance.", OpenTelemetryConstants.OTEL_RUNTIME_INSTANCE_NAME);
            }
            return otelRuntimeInstance;
        }

        //If the runtime is disabled things that expect the runtime instance like the logging may try to acquire an
        //OTel anyway - they shouldn't, they should check if its enabled first and not grab an OpenTelemetry if its not
        //for performance - but they might so return a null.
        if (metaData == null) {
            //TODO logging here
            return new DisabledOpenTelemetryInfo();
        }

        try {
            OpenTelemetryInfoReference atomicRef = (OpenTelemetryInfoReference) metaData.getMetaData(slotForOpenTelemetryInfoHolder);
            if (atomicRef == null) {
                //If this is triggered by internal code that isn't supposed to call ApplicationStateListener.applicationStarting() don't throw an error
                String j2EEName = metaData.getJ2EEName().toString();
                if (j2EEName.startsWith("io.openliberty") || j2EEName.startsWith("com.ibm.ws")
                    || j2EEName.startsWith("arquillian-liberty-support")) {
                    Tr.info(tc, "CWMOT5100.tracing.is.disabled", j2EEName);
                    return new DisabledOpenTelemetryInfo();
                }
                //If it isn't throw something nicer than an NPE.
                throw new IllegalStateException("Attempted to create openTelemetaryInfo for application " + j2EEName + " which has not gone through ApplicationStarting");
            }
            LazyInitializer<OpenTelemetryInfo> supplier = atomicRef.get();
            return supplier.get();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }
}
