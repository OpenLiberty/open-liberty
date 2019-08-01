package com.ibm.ws.config.internal;

import com.ibm.ws.config.featuregen.internal.FeatureListComponentManager;
import com.ibm.ws.config.featuregen.internal.FeatureListMBeanImpl;
import com.ibm.ws.config.schemagen.internal.SchemaGeneratorComponentManager;
import com.ibm.ws.config.schemagen.internal.SchemaGeneratorImpl;
import com.ibm.ws.config.schemagen.internal.ServerSchemaGeneratorComponentManager;
import com.ibm.ws.config.schemagen.internal.ServerSchemaGeneratorImpl;
import com.ibm.ws.config.xml.internal.ConfigIntrospection;
import com.ibm.ws.config.xml.internal.ConfigIntrospectionComponentManager;
import com.ibm.ws.config.xml.internal.MetaTypeIntrospection;
import com.ibm.ws.config.xml.internal.MetaTypeIntrospectionComponentManager;
import com.ibm.ws.config.xml.internal.ServerConfigMBeanComponentManager;
import com.ibm.ws.config.xml.internal.ServerXMLConfigurationMBeanImpl;
import com.ibm.ws.config.xml.internal.metatype.MetaTypeFactoryComponentManager;
import com.ibm.ws.config.xml.internal.metatype.MetaTypeFactoryImpl;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class BundleHelper implements StaticBundleComponentFactory {

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (MetaTypeFactoryImpl.class.getName().equals(componentName)) {
            return new MetaTypeFactoryComponentManager();
        }
        if (FeatureListMBeanImpl.class.getName().equals(componentName)) {
            return new FeatureListComponentManager();
        }
        if (ServerSchemaGeneratorImpl.class.getName().equals(componentName)) {
            return new ServerSchemaGeneratorComponentManager();
        }
        if (SchemaGeneratorImpl.class.getName().equals(componentName)) {
            return new SchemaGeneratorComponentManager();
        }
        if (ConfigIntrospection.class.getName().equals(componentName)) {
            return new ConfigIntrospectionComponentManager();
        }
        if (MetaTypeIntrospection.class.getName().equals(componentName)) {
            return new MetaTypeIntrospectionComponentManager();
        }
        if (ServerXMLConfigurationMBeanImpl.class.getName().equals(componentName)) {
            return new ServerConfigMBeanComponentManager();
        }
        return null;
    }
}