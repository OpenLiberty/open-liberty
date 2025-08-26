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
package org.apache.myfaces.util;

import org.apache.myfaces.util.lang.ClassUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.context.ExternalContext;
import jakarta.validation.Validation;
import org.apache.myfaces.util.lang.Lazy;
import org.apache.myfaces.webapp.FacesInitializerImpl;

/**
 * <p>
 * Utility class for determining which specifications are available
 * in the current process. See JIRA issue: http://issues.apache.org/jira/browse/MYFACES-2386
 * </p>
 *
 * @author Jan-Kees van Andel
 * @author Jakob Korherr (latest modification by $Author$)
 * @version $Revision$ $Date$
 * @since 2.0
 */
public final class ExternalSpecifications
{
    private static final Logger log = Logger.getLogger(ExternalSpecifications.class.getName());

    private static Lazy<Boolean> beanValidationAvailable = new Lazy<>(() ->
    {
        boolean available;
        try
        {
            try
            {
                available = ClassUtils.classForName("jakarta.validation.Validation") != null;
            }
            catch(ClassNotFoundException e)
            {
                available = false;
            }

            if (available)
            {
                try
                {
                    // Trial-error approach to check for Bean Validation impl existence.
                    // If any Exception occurs here, we assume that Bean Validation is not available.
                    // The cause may be anything, i.e. NoClassDef, config error...
                    Validation.buildDefaultValidatorFactory().getValidator();
                }
                catch (Throwable t)
                {
                    //log.log(Level.FINE, "Error initializing Bean Validation (could be normal)", t);
                    available = false;
                }
            }
        }
        catch (Throwable t)
        {
            log.log(Level.FINE, "Error loading class (could be normal)", t);
            available = false;
        }

        log.info("MyFaces Core Bean Validation support " + (available ? "enabled" : "disabled"));

        return available;
    });
    
    private static Lazy<Boolean> cdiAvailable = new Lazy<>(() ->
    {
        boolean available;
        try
        {
            available = ClassUtils.classForName("jakarta.enterprise.inject.spi.BeanManager") != null;
        }
        catch (Throwable t)
        {
            //log.log(Level.FINE, "Error loading class (could be normal)", t);
            available = false;
        }

        log.info("MyFaces Core CDI support " + (available ? "enabled" : "disabled"));
 
        return available;
    });


    private static Lazy<Boolean> sevlet6Available = new Lazy<>(() ->
    {
        boolean available;
        try
        {
            available = jakarta.servlet.SessionCookieConfig.class.getMethod("getAttribute", String.class) != null;
        }
        catch (Throwable t)
        {
            available = false;
        }
        log.info("MyFaces Core Servlet 6.0 support " + (available ? "enabled" : "disabled"));

        return available;
    });
    
    /**
     * This method determines if Bean Validation is present.
     *
     * Eager initialization is used for performance. This means Bean Validation binaries
     * should not be added at runtime after this variable has been set.
     * @return true if Bean Validation is available, false otherwise.
     */
    public static boolean isBeanValidationAvailable()
    {
        return beanValidationAvailable.get();
    }
    
    public static boolean isCDIAvailable(ExternalContext externalContext)
    {
        return cdiAvailable.get() && 
                externalContext.getApplicationMap().containsKey(FacesInitializerImpl.CDI_BEAN_MANAGER_INSTANCE);
    }

    public static boolean isServlet6Available()
    {
        return sevlet6Available.get();
    }

    /**
     * this class should not be instantiated.
     */
    private ExternalSpecifications()
    {
    }

}
