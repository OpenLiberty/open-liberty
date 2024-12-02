package com.ibm.ws.container.service.annocache.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.annocache.targets.cache.internal.ApplicationKeyService;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;

@Component
public class ApplicationKeyServiceImpl implements ApplicationKeyService, ApplicationMetaDataListener {

    private final Map<String, AppKey> keys = new HashMap<String, AppKey>();

    public ApplicationKeyServiceImpl() {
        keys.put(ClassSource_Factory.UNNAMED_APP, new AppKey(ClassSource_Factory.UNNAMED_APP));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApplicationKeyService.AppKey getKeyForApp(String appName) {
        return keys.get(appName);
    }

    @Override
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        //We need to ensure that the keys match the inputs to
        //com.ibm.ws.container.service.annocache.internal.AnnotationsImpl.setAppName()

        // That is called by from AnnotationsImpl.createRootClassSource()

        // Which in turn gets the appName from AnnotationsImpl.setAppName()
        // There are two paths into that method.
        // CDIArchiveImpl calls it inside getAnnotatedClassesPostBeta()

        // CDIArchiveImpl gets it from ApplicationInfo.getDeploymentName()

        // app manager calls it in EARDeployedAppInfo.hasAnnotationsPostBeta()
        // That class goes through a chain of getters ending in com.ibm.ws.app.manager.internal.ApplicationConfig
        // which reads the server.xml.
        // This should match ApplicationInfo.getDeploymentName()

        String appDeploymentName = event.getMetaData().getJ2EEName().getApplication();
        keys.put(appDeploymentName, new AppKey(appDeploymentName));
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        String appDeploymentName = event.getMetaData().getJ2EEName().getApplication();
        keys.remove(appDeploymentName);
    }
}