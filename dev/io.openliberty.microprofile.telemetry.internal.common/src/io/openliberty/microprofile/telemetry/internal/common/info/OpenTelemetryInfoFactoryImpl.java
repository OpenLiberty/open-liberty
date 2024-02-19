/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.common.info;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
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

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

// We want this to start before CDI so the meta data slot is ready before anyone triggers the CDI producer.
@Component(service = { ApplicationStateListener.class, OpenTelemetryInfoFactory.class }, property = { "service.vendor=IBM", "service.ranking:Integer=150" })
public class OpenTelemetryInfoFactoryImpl implements ApplicationStateListener, OpenTelemetryInfoFactory {

    /*
     * Class summary.
     *
     * This class consists of three parts
     * 1: Methods for producing instances of OpenTelemetryInfo and its wrapped OpenTelemetry which are used by
     * suppliers (see below)
     *
     * 2. An ApplicationStateListener which controls the state of OpenTelemetryInfo in a lazy fashion.
     *
     * On startup the ASL populates ApplicationMetaData with a lazy supplier of OpenTelemetryInfo.
     *
     * On shutdown the ASL replaces the supplier in ApplicationMetaData with a supplier that will return a DisposedOpenTelemetryInfo
     * (DisposedOpenTelemetryInfo logs warnings and no-ops when it is used).
     * The ASL also OpenTelemetryInfo.dispose() which will clean up an open telemetry object.
     *
     * To ensure threads do not get the wrong OpenTelemetryInfo from the application metadata, it is stored as an AtomicReference
     */

    private static final TraceComponent tc = Tr.register(OpenTelemetryInfoFactoryImpl.class);

    private final MetaDataSlot slotForOpenTelemetryInfoHolder;

    //This contains API calls that change between the upstream open telemetry version.
    //We get a partially configued SDK Builder from OSGi becase we are in a static context
    //and do not know which version of mpTelemetry will be in use.
    @Reference
    private OpenTelemetryVersionedConfiguration openTelemetryVersionedConfiguration;

    @Activate
    public OpenTelemetryInfoFactoryImpl(@Reference MetaDataSlotService slotService) {
        slotForOpenTelemetryInfoHolder = slotService.reserveMetaDataSlot(ApplicationMetaData.class);
    }

    @Override
    public OpenTelemetryInfo getOpenTelemetryInfo() {
        try {
            ApplicationMetaData metaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData().getApplicationMetaData();

            return getOpenTelemetryInfo(metaData);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    //A shortcut method to avoid fetching metadata more than we need to.
    @Override
    public OpenTelemetryInfo getOpenTelemetryInfo(ApplicationMetaData metaData) {
        try {
            OpenTelemetryInfoReference atomicRef = (OpenTelemetryInfoReference) metaData.getMetaData(slotForOpenTelemetryInfoHolder);
            if (atomicRef == null) {
                //If this is triggered by internal code that isn't supposed to call ApplicationStateListener.applicationStarting() don't throw an error
                String j2EEName = metaData.getJ2EEName().toString();
                if (j2EEName.startsWith("io.openliberty") || j2EEName.startsWith("com.ibm.ws")) {
                    Tr.info(tc, "CWMOT5100.tracing.is.disabled", j2EEName);
                    return new DisabledOpenTelemetryInfo();
                }
                //If it isn't throw something nicer than an NPE.
                throw new IllegalStateException("Attempted to create openTelemetaryInfo for application " + j2EEName + " which has not gone through ApplicationStarting");
            }
            OpenTelemetryInfoWrappedSupplier supplier = atomicRef.get();
            return supplier.get();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    private OpenTelemetryInfo createOpenTelemetryInfo() {
        try {
            String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();

            if (AgentDetection.isAgentActive()) {
                // If we're using the agent, it will have set GlobalOpenTelemetry and we must use its instance
                // all config is handled by the agent in this case
                return new EnabledOpenTelemetryInfo(true, GlobalOpenTelemetry.get(), appName);
            }

            final Map<String, String> telemetryProperties = getTelemetryProperties();

            //Builds tracer provider if user has enabled tracing aspects with config properties
            if (!checkDisabled(telemetryProperties)) {
                OpenTelemetry openTelemetry = AccessController.doPrivileged((PrivilegedAction<OpenTelemetry>) () -> {
                    return openTelemetryVersionedConfiguration.getPartiallyConfiguredOpenTelemetrySDKBuilder().addPropertiesCustomizer(x -> telemetryProperties) //Overrides OpenTelemetry's property order
                                                          .addResourceCustomizer(OpenTelemetryInfoFactoryImpl::customizeResource) //Defaults service name to application name
                                                          .setServiceClassLoader(Thread.currentThread().getContextClassLoader()).build().getOpenTelemetrySdk();
                });

                if (openTelemetry != null) {
                    return new EnabledOpenTelemetryInfo(true, openTelemetry, appName);
                }
            }

            //By default, MicroProfile Telemetry tracing is off.
            //The absence of an installed SDK is a “no-op” API
            //Operations on a Tracer, or on Spans have no side effects and do nothing
            Tr.info(tc, "CWMOT5100.tracing.is.disabled", appName);

            return new DisabledOpenTelemetryInfo();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }

    }

    private static OpenTelemetryInfo createDisposedOpenTelemetryInfo() {
        try {
            String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();
            return new DisposedOpenTelemetryInfo(appName); //Getting appName like this was easier than writing a functional interface

        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    private static boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(OpenTelemetryConstants.ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(OpenTelemetryConstants.ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY));
        }
        return true;
    }

    private HashMap<String, String> getTelemetryProperties() {
        try {
            Config config = ConfigProvider.getConfig();
            HashMap<String, String> telemetryProperties = new HashMap<>();
            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith("otel") || propertyName.startsWith("OTEL")) {
                    String normalizedName = propertyName.toLowerCase().replace('_', '.');
                    config.getOptionalValue(normalizedName, String.class)
                        .ifPresent(value -> telemetryProperties.put(normalizedName, value));
                }
            }

            telemetryProperties.putAll(openTelemetryVersionedConfiguration.getTelemetryPropertyDefaults());
            return telemetryProperties;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new HashMap<String, String>();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        //We do not actually initilize on application starting, we do that lazily if this is needed.

        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        OpenTelemetryInfoReference oTelRef = (OpenTelemetryInfoReference) extAppInfo.getMetaData().getMetaData(slotForOpenTelemetryInfoHolder);

        OpenTelemetryInfoWrappedSupplier newSupplier = new OpenTelemetryInfoWrappedSupplier(this::createOpenTelemetryInfo, openTelemetryInfo -> openTelemetryInfo.dispose());

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
        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        OpenTelemetryInfoReference oTelRef = (OpenTelemetryInfoReference) extAppInfo.getMetaData().getMetaData(slotForOpenTelemetryInfoHolder);

        OpenTelemetryInfoWrappedSupplier newSupplier = new OpenTelemetryInfoWrappedSupplier(OpenTelemetryInfoFactoryImpl::createDisposedOpenTelemetryInfo, openTelemetryInfo -> {
        });

        OpenTelemetryInfoWrappedSupplier oldSupplier = oTelRef.getAndSet(newSupplier);

        try {
            oldSupplier.closeAndDisposeIfCreated();
        } catch (Exception e) {
            Tr.warning(tc, "applicationStopped", "failed to dispose of OpenTelemetry");//TODO better message
        }

    }

    //Adds the service name to the resource attributes
    private static Resource customizeResource(Resource resource, ConfigProperties c) {
        ResourceBuilder builder = resource.toBuilder();
        builder.put(AttributeKey.stringKey("service.name"), getServiceName(c));
        return builder.build();
    }

    //Uses application name if the user has not given configured service.name resource attribute
    private static String getServiceName(ConfigProperties c) {
        String appName = c.getString(OpenTelemetryConstants.SERVICE_NAME_PROPERTY);
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (appName == null) {
            if (cmd != null) {
                appName = cmd.getModuleMetaData().getApplicationMetaData().getName();
            }
        }
        return appName;
    }

    //Interfaces and private classes only relevent to this factory.

    public interface OpenTelemetryVersionedConfiguration {
        public AutoConfiguredOpenTelemetrySdkBuilder getPartiallyConfiguredOpenTelemetrySDKBuilder();
        public Map<String, String> getTelemetryPropertyDefaults();
    }

    /*
     * There are two race conditions we need to protect against.
     *
     * On app startup/shutdown we need to swap the holder associated with the application.
     * We protect against race conditions here by using an AtomicReference.
     *
     * Within the context of an application's lifecycle we need to ensure OpenTelemetryInfo
     * is only created once. OpenTelemetryInfoReference handles this.
     */
    private class OpenTelemetryInfoReference extends AtomicReference<OpenTelemetryInfoWrappedSupplier> {

        private static final long serialVersionUID = -4884222080590544495L;
    }

    private class OpenTelemetryInfoWrappedSupplier {
        private final Consumer<OpenTelemetryInfo> disposer;
        private Supplier<OpenTelemetryInfo> supplier;
        private volatile OpenTelemetryInfo openTelemetryInfo = null;

        public OpenTelemetryInfoWrappedSupplier(Supplier<OpenTelemetryInfo> supplier, Consumer<OpenTelemetryInfo> disposer) {
            this.disposer = disposer;
            this.supplier = supplier;
        }

        public OpenTelemetryInfo get() throws InterruptedException, ExecutionException {
            if (openTelemetryInfo != null) {
                return openTelemetryInfo;
            }

            synchronized (this) {
                if (openTelemetryInfo != null) {
                    return openTelemetryInfo;
                }

                openTelemetryInfo = supplier.get();
                return openTelemetryInfo;
            }
        }

        /**
         * Cleans up the contained OpenTelemetryInfo, prevents this from creating new ones.
         *
         * @return true if an OpenTelemetryInfo instance has previously been created and now disposed. False if not OpenTelemetryInfo instance has previously been created.
         */
        public boolean closeAndDisposeIfCreated() {
            synchronized (this) {
                supplier = OpenTelemetryInfoFactoryImpl::createDisposedOpenTelemetryInfo;
                if (openTelemetryInfo != null) {
                    disposer.accept(openTelemetryInfo);
                    return true;
                }
                return false;
            }
        }

    }
}
