/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Annotations data for a non-web module.
 */

// Used by:
//
// com.ibm.ws.ejbcontainer/src/com/ibm/ws/ejbcontainer/osgi/internal/ModuleInitDataAdapter.java
// com.ibm.ws.ejbcontainer/src/com/ibm/ws/ejbcontainer/osgi/internal/ModuleMergeData.java
// com.ibm.ws.jaxrs.2.0.common/src/com/ibm/ws/jaxrs20/utils/JaxRsUtils.java
// com.ibm.ws.jaxws.clientcontainer/src/com/ibm/ws/jaxws/utils/JaxWsUtils.java
// com.ibm.ws.jaxws.common/src/com/ibm/ws/jaxws/utils/JaxWsUtils.java
// com.ibm.ws.jaxws.ejb/src/com/ibm/ws/jaxws/ejb/EJBJaxWsModuleInfoBuilder.java
// com.ibm.ws.jca.utils/src/com/ibm/ws/jca/utils/metagen/MetatypeGenerator.java
// com.ibm.ws.jca.utils/src/com/ibm/ws/jca/utils/metagen/RAAnnotationProcessor.java
// com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxrs.3.2/src/com/ibm/ws/jaxrs20/utils/JaxRsUtils.java
//
// com.ibm.ws.ejbcontainer_test/test/com/ibm/ws/ejbcontainer/osgi/internal/AnnoMockery.java
// com.ibm.ws.ejbcontainer_test/test/com/ibm/ws/ejbcontainer/osgi/internal/ModuleInitDataFactoryTest.java
//
// Module annotations have module information, 

public interface ModuleAnnotations extends Annotations {
    // Not currently in use.

	/**
	 * Set the external references class loader of the module.
	 *
	 * Deprecated.  Use {@link Annotations#setClassLoader(ClassLoader)}.
	 *
	 * @param classLoader The external references class loader of the module.
	 */
	@Deprecated
    void addAppClassLoader(ClassLoader classLoader);

	//

	/**
	 * Answer the module of the annotations information.
	 * 
	 * @return The module of the annotations information.
	 */
	ModuleInfo getModuleInfo();

    //

    /**
     * Answer the application of the associated module.
     * 
     * @return The application of the associated module.
     */
	ApplicationInfo getAppInfo();

    /**
     * Answer the container of the associated application.
     * 
     * @return The container of the associated application.
     */
	Container getAppContainer();	
}
