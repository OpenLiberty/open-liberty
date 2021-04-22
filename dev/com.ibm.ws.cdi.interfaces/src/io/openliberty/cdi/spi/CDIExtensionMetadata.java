/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi.spi;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

/**
 * This is a interface for CDI Runtime extensions. Liberty features that wish to extend CDI will need to
 * register a service under this interface.
 * <p>
 * To use this class you must implement at least one of the three methods. If you implement {@link #getBeanClasses()} all classes returned by that
 * method will be registered with CDI and may be used normally by application code. If you implement {@link #getBeanDefiningAnnotationClasses()} 
 * all annotations returned by that method will become bean defining annotations as per the CDI specifications. If you implement {@link #getExtensions()} then
 * all classes returned by that method will be treated as CDI extensions any observer methods for container lifecycle events will be called
 * when creating the CDI container for each application. All three methods can be implemented in the same class.
 * <p>
 * Classes returned from {@code getExtensions()} must implement {@link javax.enterprise.inject.spi.Extension}. They do <b>not</b> and should not be listed in a
 * {@code META-INF/services file}. It is best practice to not put CDIExtensionMetaData and {@code javax.enterprise.inject.spi.Extension} on the same class
 * as that would result in CDI and OSGI independently instantiating the class. Even though it is unlikely for the two instances to conflict,
 * it is best to keep the OSGI service and CDI extension separate.
 * <p>
 * The class that implements this interface should be registered as an OSGi service, for example by annotating it with
 * {@code @Component(service = CDIExtensionMetadata.class, configurationPolicy=IGNORE)}.
 * <p>
 * Here is a worked example of a complete CDIExtensionMetadata implementation.
 * <p>
 *
 * <pre>
 * &#64;Component(service = CDIExtensionMetaData.class, configurationPolicy = IGNORE)
 * public class SPIMetaData implements CDIExtensionMetaData {
 *
 *     &#64;Override
 *     public Set&lt;Class&lt;?&gt;&gt; getBeanClasses() {
 *         Set&lt;Class&lt;?&gt;&gt; beans = new HashSet&lt;Class&lt;?&gt;&gt;();
 *         //This will register a producer class and expose it's produced beans to applications
 *         beans.add(ClassSPIRegisteredProducer.class);
 *     }
 * }
 * </pre>
 */

/*
 * A complete bundle that uses CDIExtensionMetadata to register CDI beans can be found at:
 * com.ibm.ws.cdi.extension_fat/test-bundles/cdi.spi.extension/ <p>
 *
 * Note the files com.ibm.ws.cdi.extension_fat/cdi_spi_extension.bnd and com.ibm.ws.cdi.extension_fat/publish/features/cdi.spi.extension.mf
 * both of which are needed to include a bundle into liberty. com.ibm.ws.cdi.extension_fat/test-applications/SPIExtension.war/ contains a
 * simple app that injects the beans provided by cdi.spi.extension.
 */

public interface CDIExtensionMetadata {

    /**
     * All classes returned by this method will be will be found by CDI during type discovery
     * so that they can then be used as beans (or interceptors etc. if annotated as such) by the application.
     * All classes must be in the same archive as your CDIExtensionMetadata.
     */
    default public Set<Class<?>> getBeanClasses() {
        return Collections.emptySet();
    }

    /**
     * All classes returned by this method will be will be treated as bean defining annotations when CDI
     * performs annotation scanning during application startup.
     * All classes must be in the same archive as your CDIExtensionMetadata.
     */
    default public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        return Collections.emptySet();
    }

    /**
     * All classes returned by this method will be treated as CDI extensions. Override this method if you need to observe CDI container
     * lifecycle events to do something more advanced that just providing additional bean classes.
     * All extensions must be in the same archive as your CDIExtensionMetadata.
     */
    public default Set<Class<? extends Extension>> getExtensions() {
        return Collections.emptySet();
    }

}
