/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache;

import java.util.List;

import com.ibm.ws.container.service.annocache.FragmentAnnotations;
import com.ibm.ws.container.service.annocache.ModuleAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Annotations data for web type modules.
 *
 * The annotation services type acts as a Future<ClassSource_Aggregate>,
 * as a Future<AnnotationTargets_Targets>, and as a Future<InfoStore>, with
 * the sharing of a single class source between the other two futures.
 * 
 * Current references are from:
 * 
 * com.ibm.ws.webcontainer.osgi.DeployedModImpl.adapt(Class<T>)
 * 
 * That adapt implementation provides three entries into the annotation
 * services:
 * 
 * <ol>
 * <li>DeployedModule adapt to ClassSource_Aggregate</li>
 * <li>DeployedModule adapt to AnnotationTargets_Targets</li>
 * <li>DeployedModule adapt to ClassSource</li>
 * </ol>
 * 
 * Notification plan:
 * 
 * Adaptation to annotation targets requires a possibly time consuming scan.
 * 
 * Informational messages are generated for the initiation of a scan, and for the
 * completion of a scan.
 * 
 * Note on fragment paths:
 * 
 * These are specified through a complete, ordered, list,
 * and through the partition of that list into <b>included</b>
 * <b>partial</b> and <b>excluded</b> locations. Seed locations
 * are fragment jars which are neither metadata-complete nor
 * were excluded because they were omitted from an absolute
 * ordering. Partial locations are those which are metadata-complete
 * and not omitted from an absolute ordering. Excluded locations
 * are those which were omitted because an absolute ordering is
 * specified, and no others element is specified. The excluded
 * locations are those which were not specified in the absolute
 * ordering explicit listing.
 * 
 * Note that exclusion has precedence over metadata-complete.
 * 
 * Fragment paths are relative to the "WEB-INF/lib" folder, not
 * to the rot web module container.
 * 
 * Included locations are scanned for annotations and for class
 * relationship information. Partial locations are scanned only for
 * class relationship information. Excluded locations are scanned only
 * for class relationship information, and only as required to complete
 * class relationship information for referenced classes.
 * 
 * Class relationship information is the class to superclass and
 * the class to implements relationships.
 */

// Used by:
//
// com.ibm.ws.container.service/src/com/ibm/ws/container/service/config/ServletConfigurator.java
// com.ibm.ws.ejbcontainer/src/com/ibm/ws/ejbcontainer/osgi/internal/ModuleInitDataAdapter.java
// com.ibm.ws.jaxrs.2.0.common/src/com/ibm/ws/jaxrs20/utils/JaxRsUtils.java
// com.ibm.ws.jaxrs.2.0.common/src/org/apache/cxf/jaxrs/utils/InjectionUtils.java
// com.ibm.ws.jaxrs.2.0.server/src/com/ibm/ws/jaxrs20/server/component/JaxRsExtensionFactory.java
// com.ibm.ws.jaxrs.2.0.server/src/com/ibm/ws/jaxrs20/server/component/JaxRsWebModuleInfoBuilder.java
// com.ibm.ws.jaxws.clientcontainer/src/com/ibm/ws/jaxws/utils/JaxWsUtils.java
// com.ibm.ws.jaxws.common/src/com/ibm/ws/jaxws/utils/JaxWsUtils.java
// com.ibm.ws.jaxws.ejb/src/com/ibm/ws/jaxws/ejb/EJBInWarJaxWsModuleInfoBuilderExtension.java
// com.ibm.ws.jaxws.web/src/com/ibm/ws/jaxws/web/WebJaxWsModuleInfoBuilder.java
// com.ibm.ws.jaxws.webcontainer/src/com/ibm/ws/jaxws/webcontainer/JaxWsExtensionFactory.java
// com.ibm.ws.jsf.2.2/src/com/ibm/ws/jsf/config/annotation/WASMyFacesAnnotationProvider.java
// com.ibm.ws.jsf.shared/src/com/ibm/ws/jsf/shared/util/JSFInjectionClassListCollaborator.java
// com.ibm.ws.microprofile.openapi/src/com/ibm/ws/microprofile/openapi/AnnotationScanner.java
// com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxrs.3.2/src/com/ibm/ws/jaxrs20/utils/JaxRsUtils.java
// com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxrs.3.2/src/org/apache/cxf/jaxrs/utils/InjectionUtils.java
// com.ibm.ws.org.apache.myfaces.2.3/src/com/ibm/ws/jsf/config/annotation/WASMyFacesAnnotationProvider.java
// com.ibm.ws.springboot.support_fat/fat/src/com/ibm/ws/springboot/support/fat/CommonWebServerTests15.java
// com.ibm.ws.springboot.support_fat/fat/src/com/ibm/ws/springboot/support/fat/WebAnnotationTests.java
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/container/config/WebAppConfigurationAdapter.java
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/container/config/WebAppConfigurator.java
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/container/config/WebAppConfiguratorHelper.java
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/webapp/WebApp.java
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/webapp/WebApp.java
// com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/metadata/SecurityServletConfiguratorHelper.java
// com.ibm.ws.webcontainer.security/test/com/ibm/ws/webcontainer/security/metadata/SecurityServletConfiguratorHelperTest.java
// com.ibm.ws.webcontainer.servlet.3.1/src/com/ibm/ws/webcontainer31/util/ServletInjectionClassListCollaborator.java
// com.ibm.ws.webcontainer.servlet.3.1.factories/test/com/ibm/ws/webcontainer/webapp/config/ServletConfigMock.java
// com.ibm.ws.wsoc.cdi.weld/src/com/ibm/ws/wsoc/cdi/weld/WebSocketInjectionClassListCollaborator.java

public interface WebAnnotations extends ModuleAnnotations, com.ibm.ws.container.service.annotations.WebAnnotations {

    /**
     * Override: The module information of web annotations is known
     * to be web module information.
     * 
     * @return The web module information of the web module annotations.
     */
    WebModuleInfo getModuleInfo();

    /**
     * Answer the name of the web module.
     * 
     * @return The name of the web module.
     */
    String getWebModuleName();

    /**
     * Answer all of the fragments of the module.
     * 
     * @return All of the fragments of the module.
     */
    WebFragmentsInfo getWebFragments();

    /**
     * Answer the ordered list of fragments of the module.
     *
     * @return The ordered list of fragments of the module.
     */
    List<WebFragmentInfo> getOrderedItems();

    /**
     * Answer the list of excluded fragments of the module.
     *
     * @return The list of excluded fragments of the module.
     */
    List<WebFragmentInfo> getExcludedItems();

    //

    /**
     * Answer annotation targets generated from the module class source. Scan only the
     * classes in the specified fragment.
     *
     * Use the common class source for this scan.
     *
     * @param fragment The fragment which is to be scanned.
     *
     * @return Class list specific annotation targets for the module.
     *
     * @throws UnableToAdaptException Thrown by an error processing fragment paths.
     */
    FragmentAnnotations getFragmentAnnotations(WebFragmentInfo fragment) throws UnableToAdaptException;
}
