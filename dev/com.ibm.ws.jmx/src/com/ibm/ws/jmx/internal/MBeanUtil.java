/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import java.util.Arrays;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.MXBean;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationEmitter;
import javax.management.StandardEmitterMBean;
import javax.management.StandardMBean;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.service.cm.ConfigurationAdminMBean;

/**
 * JMX MBean registration utility.
 */
final class MBeanUtil {
    // @formatter:off
    
    /**
     * Register an object as a managed bean.  Wrapper the object if necessary.
     * 
     * Registration has several steps:
     * 
     * A dynamic mbean ({@link DynamicMBean}) is returned immediately.
     * 
     * A configuration mbean ({@link ConfigurationAdminMBean}) is wrapped as a
     * read-only mbean ({@link ReadConfigurationAdmin}) and that is returned.
     *
     * Otherwise, the managed bean is tested against the published interfaces
     * defined by the service.  The service specified published interfaces
     * using {@link Constants#OBJECTCLASS}.  For each interface of the managed
     * bean which is a published interface, these tests are performed:
     * 
     * If the interface matches the managed bean class name plus "MBean", return
     * the managed bean immediately.
     * 
     * If the interface matches the managed bean class name plus "MXBean", and is
     * not annotated with <code>@MXBean(false)</code>, return the managed bean
     * immediately.
     * 
     * If the the interface is annotated with <code>@MXBean(true)</code>, return
     * the managed bean immediately.
     *
     * If the interface name ends with "MBean", select it as the managed bean type.
     * However, keep checking for an immediate case, which would take precedence.
     *
     * After checking all interfaces, uses the last selected interface name which
     * is not an immediate case, and create a standard managed bean wrapping the
     * managed bean, either as {@link StandardMBean} or as {@link StandardEmiterMBean}.
     *
     * Finally, if no interface is selected, throw an exception.
     *
     * @param serviceReference The service reference providing the list of published
     *     interface names.
     * @param mBean The managed bean.
     *
     * @return A registerable managed bean, either the managed bean itself, or a wrapper
     *     of the managed bean.
     *
     * @throws NotCompliantMBeanException It the managed bean is not registerable.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getRegisterableMBean(ServiceReference<?> serviceReference, Object mBean)
                    throws NotCompliantMBeanException {

        // String methodName = "getRegisterableMBean";
        // System.out.println(methodName + ": ENTER: MBean [ " + mBean + " ]");

        // Follows the algorithm used by Apache Aries for registering MBeans.

        // REVISIT: The read-only restriction might be relaxed if and when security
        // support is added to JMX.
        if ( mBean instanceof DynamicMBean ) {
            // System.out.println(methodName + ": RETURN : MBean [ " + mBean + " ] (DynamicMBean)");
            return mBean;
        } else if ( mBean instanceof ConfigurationAdminMBean ) {
            Object readOnlyMBean = new ReadOnlyConfigurationAdmin((ConfigurationAdminMBean) mBean);
            // System.out.println(methodName + ": RETURN : MBean [ " + readOnlyMBean + " ] (ConfigurationAdminBean)");            
            return readOnlyMBean;
        }

        Class<?> fallbackInterface = null;

        Class<?> mBeanClass = mBean.getClass();
        String directInterfaceName = mBeanClass.getName() + "MBean";

        List<String> publishedInterfaceNames =
            Arrays.asList((String[]) serviceReference.getProperty(Constants.OBJECTCLASS));

        // for ( String publishedName : publishedInterfaceNames ) {
        //     System.out.println(methodName + ": Published [ " + publishedName + " ]");
        // }
        
        for ( Class<?> nextInterface : mBeanClass.getInterfaces() ) {
            String nextInterfaceName = nextInterface.getName();

            // Skip any interface which is not published.
            if ( !publishedInterfaceNames.contains(nextInterfaceName) ) {
                // System.out.println(methodName + ": Candidate interface [ " + nextInterfaceName + " ]: Skip, not published");
                continue;
            }

            // Immediate case: Match on the bean class name and ends with "MBean".            
            if ( nextInterfaceName.equals(directInterfaceName) ) {
                // System.out.println(methodName + ": RETURN [ " + mBean + " ] Direct match on interface [ " + nextInterfaceName + " ]");                
                return mBean;
            }

            // Immediate case: Match on interface tagged with "@MXBean(true)".
            // Immediate case: Match on an interface not tagged with "@MXBean(false)"
            // and which ends with "MXBean".
            MXBean mxbean = nextInterface.getAnnotation(MXBean.class);
            if ( mxbean != null ) {
                if ( mxbean.value() ) {
                    // System.out.println(methodName + ": RETURN [ " + mBean + " ] Direct match on @MXBean [ " + nextInterfaceName + " ]");                                    
                    return mBean;
                } else {
                    // System.out.println(methodName + ": Ignoring interface [ " + nextInterfaceName + " ] with @MXBean(false)");                                                        
                    // "@MXBean(false)"
                }
            } else {
                if ( nextInterfaceName.endsWith("MXBean") ) {
                    // System.out.println(methodName + ": RETURN [ " + mBean + " ] Direct match on MXBean [ " + nextInterfaceName + " ]");
                    return mBean;
                }
            }

            // Secondary case: Not a match on bean class name but ending with
            // "MBean".  Use as a fall back if the none of the immediate cases
            // triggers.
            if ( nextInterfaceName.endsWith("MBean") ) {
                // System.out.println(methodName + ": Partial match on interface [ " + nextInterfaceName + " ]");                                                    
                fallbackInterface = nextInterface; // Do NOT return immediately.
            }
        }

        // REVISIT: The object wasn't of the form we were expecting though
        // might still be an MBean.  For now let's reject it. We can support
        // more types of MBeans later.

        if ( fallbackInterface == null ) {
            // System.out.println(methodName + ": Failing with NonCompliantMBeanException");

            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Unregisterable MBean: [ " + mBeanClass.getName() + " ]");
            for ( Class<?> nextInterface : mBeanClass.getInterfaces() ) {
                errorMessage.append(" implements [ " + nextInterface.getName() + " ]");
            }
            for ( String publishedName : (String[]) serviceReference.getProperty(Constants.OBJECTCLASS) ) {
                errorMessage.append(" published [ " + publishedName + " ]");
            }
            throw new NotCompliantMBeanException(errorMessage.toString());
        }

        Object standardMBean;
        if ( mBean instanceof NotificationEmitter ) {
            // System.out.println(methodName + ": RETURN [ " + mBean + " ] (Standard Emitter MBean)");
            standardMBean = new StandardEmitterMBean(mBean, (Class) fallbackInterface, (NotificationEmitter) mBean);
        } else {
            // System.out.println(methodName + ": RETURN [ " + mBean + " ] (Standard MBean)");            
            standardMBean = new StandardMBean(mBean, (Class) fallbackInterface);
        }
        return standardMBean;
    }

    // @formatter:on
}
