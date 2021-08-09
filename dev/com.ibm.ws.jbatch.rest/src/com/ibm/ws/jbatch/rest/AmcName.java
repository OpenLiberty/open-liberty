/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest;

import java.util.Arrays;

import com.ibm.ws.jbatch.rest.utils.StringUtils;

/**
 * Wrapper around application / module / component name, which is used
 * to identify the app component meta data to use when dispatching a batch
 * job from BatchManager.
 * 
 * Note: the "appname" field in the batch DB is the serialized AmcName.
 * 
 */
public class AmcName {

    private String applicationName;
    
    private String moduleName;
    
    private String componentName;
    
    /**
     * CTOR.
     */
    public AmcName(String applicationName,
                   String moduleName,
                   String componentName) {
        
        if ( StringUtils.isEmpty(applicationName) && StringUtils.isEmpty(moduleName) ) {
            throw new IllegalArgumentException("At least one of applicationName, moduleName must be non-empty: " 
                                               + applicationName + ", " + moduleName + ", " + componentName);
        }
        
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.componentName = componentName;
    }
    
    /**
     * @return applicationName
     */
    public String getApplicationName() {
        return ( !StringUtils.isEmpty(applicationName) ) 
                    ? applicationName
                    : StringUtils.trimSuffix(moduleName, ".war");
    }
    
    /**
     * @return moduleName
     */
    public String getModuleName() {
        return (!StringUtils.isEmpty(moduleName)) ? moduleName : applicationName + ".war";
    }
    
    /**
     * @return componentName
     */
    public String getComponentName() {
        return ( componentName != null) ? componentName : "";
    }
    
    /**
     * @return in string form: "{app}#{module}#{comp}"
     */
    public String toString() {
        return ( StringUtils.isEmpty(getComponentName()) ) 
                    ? StringUtils.join( Arrays.asList(getApplicationName(), getModuleName()), "#" )
                    : StringUtils.join( Arrays.asList(getApplicationName(), getModuleName(), getComponentName()), "#" );
    }
    
    /**
     * @param s The result of AmcName.toString()
     * 
     * @return an AmcName object parsed from the given string form
     */
    public static AmcName parse(String s) {
        
        String[] amc = s.split("#");
        return new AmcName(amc[0], amc[1], (amc.length > 2) ? amc[2] : null);
    }    
 
}
