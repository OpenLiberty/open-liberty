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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.webapp.AbstractFacesInitializer;

/**
 * <p>
 * Utility class for determining which specifications are available
 * in the current process. See JIRA issue: http://issues.apache.org/jira/browse/MYFACES-2386
 * </p>
 *
 * @author Jan-Kees van Andel
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1526587 $ $Date: 2013-09-26 15:57:08 +0000 (Thu, 26 Sep 2013) $
 * @since 2.0
 */
public final class ExternalSpecifications
{

    //private static final Log log = LogFactory.getLog(BeanValidator.class);
    private static final Logger log = Logger.getLogger(ExternalSpecifications.class.getName());

    private static volatile Boolean beanValidationAvailable;
    private static volatile Boolean unifiedELAvailable;
    private static volatile Boolean cdiAvailable;
    private static volatile Boolean el3Available;

    /**
     * This method determines if Bean Validation is present.
     *
     * Eager initialization is used for performance. This means Bean Validation binaries
     * should not be added at runtime after this variable has been set.
     * @return true if Bean Validation is available, false otherwise.
     */
    public static boolean isBeanValidationAvailable()
    {
        if (beanValidationAvailable == null)
        {
            try
            {
                try
                {
                    beanValidationAvailable = (Class.forName("javax.validation.Validation") != null);
                }
                catch(ClassNotFoundException e)
                {
                    beanValidationAvailable = Boolean.FALSE;
                }

                if (beanValidationAvailable)
                {
                    try
                    {
                        // Trial-error approach to check for Bean Validation impl existence.
                        // If any Exception occurs here, we assume that Bean Validation is not available.
                        // The cause may be anything, i.e. NoClassDef, config error...
                        _ValidationUtils.tryBuildDefaultValidatorFactory();
                    }
                    catch (Throwable t)
                    {
                        //log.log(Level.FINE, "Error initializing Bean Validation (could be normal)", t);
                        beanValidationAvailable = false;
                    }
                }
            }
            catch (Throwable t)
            {
                log.log(Level.FINE, "Error loading class (could be normal)", t);
                beanValidationAvailable = false;
            }

            log.info("MyFaces Bean Validation support " + (beanValidationAvailable ? "enabled" : "disabled"));
        }
        return beanValidationAvailable;
    }

    /**
     * This method determines if Unified EL is present.
     *
     * Eager initialization is used for performance. This means Unified EL binaries
     * should not be added at runtime after this variable has been set.
     * @return true if UEL is available, false otherwise.
     */
    public static boolean isUnifiedELAvailable()
    {
        if (unifiedELAvailable == null)
        {
            try
            {
                // Check if the UEL classes are available.
                // If the JSP EL classes are loaded first, UEL will not work
                // properly, hence it will be disabled.
                unifiedELAvailable = (
                        Class.forName("javax.el.ValueReference") != null
                     && Class.forName("javax.el.ValueExpression")
                                .getMethod("getValueReference", ELContext.class) != null
                );
            }
            catch (Throwable t)
            {
                //log.log(Level.FINE, "Error loading class (could be normal)", t);
                unifiedELAvailable = false;
            }

            log.info("MyFaces Unified EL support " + (unifiedELAvailable ? "enabled" : "disabled"));
        }
        return unifiedELAvailable;
    }
    
    public static boolean isCDIAvailable(ExternalContext externalContext)
    {
        if (cdiAvailable == null)
        {
            try
            {
                cdiAvailable = Class.forName("javax.enterprise.inject.spi.BeanManager") != null;
            }
            catch (Throwable t)
            {
                //log.log(Level.FINE, "Error loading class (could be normal)", t);
                cdiAvailable = false;
            }

            log.info("MyFaces CDI support " + (cdiAvailable ? "enabled" : "disabled"));
        }
        
        return cdiAvailable && 
                externalContext.getApplicationMap().containsKey(AbstractFacesInitializer.CDI_BEAN_MANAGER_INSTANCE);
    }
    
    public static boolean isEL3Available()
    {
        if (el3Available == null)
        {
            try
            {
                el3Available = Class.forName("javax.el.StaticFieldELResolver") != null ;
            }
            catch (Throwable t)
            {
                el3Available = false;
            }
            log.info("MyFaces EL 3.0 support " + (el3Available ? "enabled" : "disabled"));
        }
        return el3Available;
    }

    /**
     * this class should not be instantiated.
     */
    private ExternalSpecifications()
    {
    }

}
