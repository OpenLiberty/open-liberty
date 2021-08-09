/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.webapp;

import javax.faces.FacesException;
import javax.servlet.ServletContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.util.ContainerUtils;

/**
 * Simple Factory to get a FacesInitializer implementation either from a web.xml
 * parameter or from a list of default implementations.
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1296049 $ $Date: 2012-03-02 04:48:16 +0000 (Fri, 02 Mar 2012) $
 */
public class FacesInitializerFactory
{
 
    /**
     * Indicate the class implementing FacesInitializer interface that will
     * be used to setup MyFaces Core contexts.
     * <p>This is used when some custom task must be done specifically when
     * a myfaces web context is initialized or destroyed, or when MyFaces should
     * be initialized in some custom environment. 
     * </p>
     */
    @JSFWebConfigParam(since = "2.0.1", desc = "Class name of a custom FacesInitializer implementation.")
    private static final String FACES_INITIALIZER_PARAM = "org.apache.myfaces.FACES_INITIALIZER";
    
    /**
     * Gets the FacesInitializer for the system.
     * @param context
     * @return
     */
    public static FacesInitializer getFacesInitializer(ServletContext context)
    {
        FacesInitializer initializer = _getFacesInitializerFromInitParam(context);
        if (initializer == null)
        {
            initializer = _getDefaultFacesInitializer(context);
        }
        return initializer;
    }
    
    /**
     * Gets a FacesInitializer from the web.xml config param.
     * @param context
     * @return
     */
    private static FacesInitializer _getFacesInitializerFromInitParam(ServletContext context)
    {
        String initializerClassName = context.getInitParameter(FACES_INITIALIZER_PARAM);
        if (initializerClassName != null)
        {
            try
            {
                // get Class object
                Class<?> clazz = ClassUtils.classForName(initializerClassName);
                if (!FacesInitializer.class.isAssignableFrom(clazz))
                {
                    throw new FacesException("Class " + clazz 
                            + " does not implement FacesInitializer");
                }
                
                // create instance and return it
                return (FacesInitializer) ClassUtils.newInstance(clazz);
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new FacesException("Could not find class of specified FacesInitializer", cnfe);
            }
        }
        return null;
    }
    
    /**
     * Returns a FacesInitializer that fits for the current environment (JSP 2.0 or 2.1).
     * @param context
     * @return
     */
    private static FacesInitializer _getDefaultFacesInitializer(ServletContext context)
    {
        // No MyfacesConfig available yet, we must read the parameter directly:
        String initParameter = context.getInitParameter(MyfacesConfig.INIT_PARAM_SUPPORT_JSP_AND_FACES_EL);
        if (Boolean.FALSE.toString().equals(initParameter))
        {
            return new FaceletsInitilializer();
        } 
        else if (ContainerUtils.isJsp21(context)) 
        {
            return new Jsp21FacesInitializer();
        } 
        else 
        {
            return new Jsp20FacesInitializer();
        }
    }
    
}
