/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.library.Library;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.jndi.objectFactory",
           service = JNDIObjectFactory.class)
public class JNDIObjectFactory {
    private static final TraceComponent tc = Tr.register(JNDIObjectFactory.class);

    private static final String REFERENCE_LIBRARY = "library";

    private final AtomicServiceReference<Library> libraryRef = new AtomicServiceReference<Library>(REFERENCE_LIBRARY);
    private String className;
    private String objectClassName;
    private ServiceRegistration<?> registration;

    @Override
    public String toString() {
        return super.toString() +
               "[className=" + className +
               ", objectClassName=" + objectClassName +
               ", libraryRef=" + libraryRef +
               ']';
    }

    // The reference target is specified via metatype
    @Reference(name = REFERENCE_LIBRARY, service = Library.class)
    protected void setLibrary(ServiceReference<Library> ref) {
        libraryRef.setReference(ref);
    }

    protected void unsetLibrary(ServiceReference<Library> ref) {
        libraryRef.unsetReference(ref);
    }

    protected void activate(ComponentContext context, Map<String, Object> props) {
        libraryRef.activate(context);

        className = (String) props.get("className");
        objectClassName = (String) props.get("objectClassName");

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        // Prevent ObjectFactory from being called without a Reference, which
        // can otherwise happen per the OSGi JNDI spec.
        properties.put("aries.object.factory.requires.reference", true);

        ObjectFactoryServiceFactoryImpl service = new ObjectFactoryServiceFactoryImpl(libraryRef.getServiceWithException(), className);
        String[] classes = new String[] { ObjectFactory.class.getName(), className };
        this.registration = context.getBundleContext().registerService(classes, service, properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "registration=" + registration);
    }

    protected void deactivate(ComponentContext context) {
        libraryRef.deactivate(context);

        if (this.registration != null) {
            this.registration.unregister();
        }
    }

    @Trivial
    public String getClassName() {
        return className;
    }

    @Trivial
    public String getObjectClassName() {
        return objectClassName;
    }

    private class ObjectFactoryServiceFactoryImpl implements ServiceFactory<ObjectFactory> {
        private final Library library;
        private final String className;
        private ObjectFactory factory;

        @Trivial
        ObjectFactoryServiceFactoryImpl(Library library, String className) {
            this.library = library;
            this.className = className;
        }

        @Override
        public synchronized ObjectFactory getService(Bundle bundle, ServiceRegistration<ObjectFactory> registration) {
            // ServiceFactory allows the ObjectFactory to be created lazily.
            if (factory == null) {
                try {
                    Class<? extends ObjectFactory> klass = library.getClassLoader().loadClass(className).asSubclass(ObjectFactory.class);
                    factory = klass.getConstructor().newInstance();
                } catch (Exception e) {
                    Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getCause() : e;
                    Tr.error(tc, "jndi.objectfactory.create.exception", className, cause);
                }
            }
            return factory;
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration<ObjectFactory> registration, ObjectFactory service) {}
    }
}
