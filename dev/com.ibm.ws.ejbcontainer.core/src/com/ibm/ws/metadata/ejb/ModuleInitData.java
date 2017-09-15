/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.javaee.dd.ejb.EJBJar;

/**
 * The data needed to initialize a bean module. This object is discarded after
 * all bean's in the module have been fully initialized.
 */
public class ModuleInitData
{
    /**
     * The module URI. For example, "MyEJB.jar"
     */
    public final String ivName;

    /**
     * The module-name specified in ejb-jar.xml, or ivName without a suffix.
     */
    public String ivLogicalName;

    /**
     * The application name.
     */
    public final String ivAppName;

    /**
     * The application and module name.
     */
    public J2EEName ivJ2EEName;

    /**
     * The deployment descriptor, or null if not included in the module.
     */
    public EJBJar ivEJBJar;

    /**
     * True if metadata should not be obtained from annotations.
     */
    public boolean ivMetadataComplete;

    /**
     * The beans in the module. Use {@link #addBean} to add to this field.
     */
    public final List<BeanInitData> ivBeans = new ArrayList<BeanInitData>();

    /**
     * True if the module contains EJBs with timer callback methods, false if
     * the module does not, or null if unknown.
     */
    public Boolean ivHasTimers;

    /**
     * The module class loader.
     */
    public ClassLoader ivClassLoader;

    /**
     * The class loader to set as the thread context class loader when calling
     * any user code.
     */
    // F85059
    public ClassLoader ivContextClassLoader;

    public ModuleInitData(String name, String appName)
    {
        ivName = name;
        ivAppName = appName;
    }

    /**
     * Add a BeanInitData for an EJB or ManagedBean located in the module
     */
    public void addBean(BeanInitData bid) {
        ivBeans.add(bid);
    }

    @Override
    public String toString()
    {
        return super.toString() + '[' + ivJ2EEName + ']';
    }

    /**
     * Clear data that is not needed past module start.
     */
    public void unload()
    {
        ivEJBJar = null;
    }

    public EJBJar getMergedEJBJar()
    {
        return null;
    }

    public EJBModuleMetaDataImpl createModuleMetaData(EJBApplicationMetaData ejbAMD)
    {
        return new EJBModuleMetaDataImpl(0, ejbAMD);
    }

    /**
     * Returns the class loader to set as the thread context class loader when
     * calling any user code.
     */
    // F85059
    public ClassLoader getContextClassLoader()
    {
        return ivContextClassLoader != null ? ivContextClassLoader : ivClassLoader;
    }
}
