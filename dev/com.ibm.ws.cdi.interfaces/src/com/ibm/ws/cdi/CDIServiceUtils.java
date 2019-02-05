/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class CDIServiceUtils {

    public static ApplicationMetaData getApplicationMetaData(Container moduleContainer) throws CDIException {
        ApplicationMetaData appMetaData = null;
        ModuleInfo moduleInfo = getModuleInfo(moduleContainer);
        if (moduleInfo != null) {
            ExtendedApplicationInfo applicationInfo = (ExtendedApplicationInfo) moduleInfo.getApplicationInfo();
            appMetaData = applicationInfo.getMetaData();
        }

        return appMetaData;
    }

    public static ModuleMetaData getModuleMetaData(Container moduleContainer) throws CDIException {
        ModuleMetaData moduleMetaData = null;
        ExtendedModuleInfo moduleInfo = (ExtendedModuleInfo) getModuleInfo(moduleContainer);
        if (moduleInfo != null) {
            moduleMetaData = moduleInfo.getMetaData();
        }

        return moduleMetaData;
    }

    private static ModuleInfo getModuleInfo(Container container) throws CDIException {
        ModuleInfo moduleInfo = null;

        try {
            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            moduleInfo = (ModuleInfo) cache.getFromCache(ModuleInfo.class);
        } catch (UnableToAdaptException e) {
            throw new CDIException(e);
        }
        return moduleInfo;
    }

    /**
     * Returns a unique identifying string for an annotated type. This method should be used when calling BeforeBeanIdentifer.addAnnotatedType()
     *
     * @param annotatedType the new annotated type you are createing. Null is an accepted value but should only be used if you know exactly what you are doing. 
     * @param extensionClass the CDI extension which is to call BeforeBeanIdentifer.addAnnotatedType()
     * @return a String that uniquely identifies this annotated type. 
     */
    public static String getAnnotatedTypeIdentifier(AnnotatedType annotatedType, Class<?> extensionClass) {
        //We use the symbolic name as a compromise between makeing the ID unique enough to allow multiple annotated types based on the same type 
        //to quote the javadoc: " This method allows multiple annotated types, based on the same underlying type, to be defined."
        //and allowing failover to work. Failover will fail if the BeanIdentifierIndex is not identical across all severs, and some of these
        //identifiers end up in the BeanIdentifierIndex. In particular problems hae been reported with ViewScopeBeanHolder
        Bundle bundle = FrameworkUtil.getBundle(extensionClass);
        String symbolicName = getSymbolicNameWithoutMinorOrMicroVersionPart(bundle.getSymbolicName());
        if (annotatedType != null) {
            return (annotatedType.getJavaClass().getCanonicalName() + "#" + extensionClass.getCanonicalName() + "#" + symbolicName);
        } else {
            return ("NULL" + "#" + extensionClass.getCanonicalName() + "#" + symbolicName);
        }        
    }


    //If BND names are different CDI failover will assume it's looking at two different applications and refuse to perform failover.
    //Since BND names include version numbers we strip them down, this method provides a central location to control how version numbers
    //are included in BND names.
    @Trivial
    public static String getOSGIVersionForBndName(Version version) {
        return String.valueOf(version.getMajor());
    }

    //This method looks for bnd symbolic names that end with the string <number>.<number>.<number>
    //And removes the last ".<number.<number>"
    @Trivial
    public static String getSymbolicNameWithoutMinorOrMicroVersionPart(String symbolicName) {
        if (symbolicName.matches(".*\\d+\\.\\d+\\.\\d+$")) {
            return symbolicName.replaceAll("\\.\\d+\\.\\d+$", "");
        } else {
            return symbolicName;
        }
    }

}
