/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.lang.reflect.Method;

import javax.ejb.Asynchronous;
import javax.ejb.StatefulTimeout;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Session;

/**
 * 
 * Helper class for validating proper metadata configuration for an EJB-based
 * application. This class is intended to be invoked from the EJBMDOrchestrator
 * class during metadata processing. It is intended to include various app
 * validation only if it will be logged. The <code>isValidationLoggable</code>
 * method can be used to determine if logging will occur.
 * 
 */
final class AppConfigChecker
{
    private AppConfigChecker()
    {
        // Do not allow instances to be created
    }

    /**
     * Logs a warning message (via the passed-in trace component) if any of the
     * passed-in interfaces or their methods contain an <code>@Asynchronous</code>
     * annotation. It will only log one message per interface no matter how many
     * annotations may exist in interface methods.
     * Note: This method will always log the message - make sure to gate this
     * method by calling {@link isValidationLoggable}.
     * 
     * @param ifaces - array of interfaces to scan for async annotations
     * @param tc - the trace component to log if any async annotations are detected - must be non-null
     */
    //F743-13921
    static void validateAsyncOnInterfaces(Class<?>[] ifaces, TraceComponent tc) {
        if (ifaces != null) {
            for (Class<?> iface : ifaces) {
                // d645943 - Modified the fix integrated for d618337 to include both
                // the class-level and method-level checks.  Since no-interface view
                // beans are the interface, we need to check that the business
                // "interface" is really a Java interface.
                if (iface.isInterface()) {
                    if (iface.getAnnotation(Asynchronous.class) != null) {
                        //interface contains class-level @Asynchronous annotation
                        Tr.warning(tc, "ASYNC_ON_INTERFACE_CNTR0305W", iface.getName());
                        continue;
                    }

                    for (Method m : iface.getMethods()) {
                        if (m.getAnnotation(Asynchronous.class) != null) {
                            //interface method contains @Asynchronous annotation
                            Tr.warning(tc, "ASYNC_ON_INTERFACE_CNTR0305W", iface.getName());
                            break; // break out of the method for-loop - continue on to the next interface
                        }
                    }
                }
            }
        }
    }

    /**
     * Logs a warning message (via the passed-in trace component) if any of the
     * passed-in interfaces contain the <code>@StatefulTimeout</code>
     * annotation. It will only log one message per interface no matter how many
     * annotations may exist in interface methods.
     * Note: This method will always log the message - make sure to gate this
     * method by calling {@link isValidationLoggable}.
     * 
     * @param ifaces - array of interfaces to scan for StatefulTimeout annotations
     * @param tc - the trace component to log if any StatefulTimeout annotations are detected - must be non-null
     */
    //F743-6605
    static void validateStatefulTimeoutOnInterfaces(Class<?>[] ifaces, TraceComponent tc) {

        if (ifaces != null) {

            for (Class<?> iface : ifaces) {
                //d618337
                if (iface.isInterface() && iface.getAnnotation(StatefulTimeout.class) != null) {
                    //interface contains class-level @StatefulTimeout annotation
                    Tr.warning(tc, "STATEFUL_TIMEOUT_ON_INTERFACE_CNTR0306W", iface.getName());
                    continue;
                }

            }

        }

    }

    /**
     * Logs a warning message (via the passed-in trace component) if the
     * passed-in bean is not a SFSB, yet contains a <code>@StatefulTimeout</code>
     * annotation or a stateful-timeout DD element.
     * 
     * Logs a warning message (via the passed-in trace component) if the
     * passed-in bean is a SFSB and contains a stateful-timeout DD element
     * lacking a timeout element.
     * 
     * 
     * Note: make sure to gate this method by calling {@link isValidationLoggable}.
     * 
     * 
     * @param cdo - component data object, containing XML info about the stateful-timeout element, if present
     * @param bmd - metadata for the bean to check for the StatefulTimeout annotation
     * @param tc - the trace component to log if any StatefulTimeout annotations are detected - must be non-null
     */
    //F743-6605
    static void validateStatefulTimeoutOnSFSB(BeanMetaData bmd, TraceComponent tc) {

        StatefulTimeout statefulTimeout = bmd.enterpriseBeanClass.getAnnotation(javax.ejb.StatefulTimeout.class);
        if (statefulTimeout != null) {

            if (bmd.type != InternalConstants.TYPE_STATEFUL_SESSION) {
                Tr.warning(tc, "STATEFUL_TIMEOUT_ON_NON_SFSB_CNTR0304W", new Object[] { bmd.getName(), bmd.getModuleMetaData().getName(),
                                                                                       bmd.getModuleMetaData().getApplicationMetaData().getName() }); // F743-6605.1, d641570
            }

        }

        // begin F743-6605.1
        EnterpriseBean eb = bmd.wccm.enterpriseBean;
        if (eb instanceof Session) {
            Session session = (Session) eb;
            com.ibm.ws.javaee.dd.ejb.StatefulTimeout statefulTimeoutXML = session.getStatefulTimeout(); // F743-6605.1
            if (statefulTimeoutXML != null) {
                if (bmd.type != InternalConstants.TYPE_STATEFUL_SESSION) {
                    Object[] parms = new Object[] { bmd.getName(), bmd.getModuleMetaData().getName(), bmd.getModuleMetaData().getApplicationMetaData().getName() };
                    Tr.warning(tc, "STATEFUL_TIMEOUT_ON_NON_SFSB_CNTR0310W", parms);
                }
            }
        }
        // end F743-6605.1
    }

}
