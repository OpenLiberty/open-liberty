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

package com.ibm.ws.jaxws.client.injection;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.ws.Service;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.jaxws.utils.StringUtils;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * This class will contain static methods that are used by various
 * classes in the integration layer's resource injection plugin code.
 * 
 */
public class InjectionHelper {

    private static final TraceComponent tc = Tr.register(InjectionHelper.class);

    /**
     * This returns the type of the injection being requested based on either the
     * annotated field or annotated method.
     */
    static Class<?> getTypeFromMember(Member member) throws InjectionException {
        Class<?> memberType = null;
        if (member instanceof Field) {
            memberType = ((Field) member).getType();
        } else if (member instanceof Method) {
            Method method = (Method) member;
            if (method.getParameterTypes() == null || method.getParameterTypes().length != 1) {
                String msg = Tr.formatMessage(tc, "error.service.ref.member.level.annotation.wrong.method.name",
                                              method.getName(), method.getDeclaringClass().getName());
                throw new InjectionException(msg);
            }
            memberType = method.getParameterTypes()[0];
        }
        return memberType;
    }

    /**
     * This method will consult a list of service refs that were found in an application module and return only those service refs which
     * are utilizing the JAX-WS programming model.
     * This will be determined by the <service-interface> element within the <service-ref>.
     */
    static List<ServiceRef> normalizeJaxWsServiceRefs(List<ServiceRef> allServiceRefs, ClassLoader classLoader) {

        if (allServiceRefs == null || allServiceRefs.size() == 0) {
            return Collections.emptyList();
        }

        List<ServiceRef> jaxwsServiceRefs = new ArrayList<ServiceRef>(allServiceRefs.size());

        for (ServiceRef serviceRef : allServiceRefs) {

            // A service-ref might have just the "service-ref-name" and "lookup-name" elements specified.
            // In that case no other elements should be present.
            if (serviceRef.getLookupName() != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The service ref: " + serviceRef.getName() + " specifies lookup-name: " + serviceRef.getLookupName());
                }
                jaxwsServiceRefs.add(serviceRef);

            } else {
                String srClassName = serviceRef.getServiceInterfaceName();
                if (StringUtils.isEmpty(srClassName)) {
                    Tr.warning(tc, "warn.service.ref.dd.service.interface.not.set", serviceRef.getName());
                    continue;
                }

                Class<?> srClass;
                try {
                    srClass = Class.forName(srClassName, false, classLoader);
                } catch (ClassNotFoundException e) {
                    Tr.warning(tc, "warn.service.ref.dd.service.interface.class.not.found", serviceRef.getName(), srClassName);
                    continue;
                }

                if (!Service.class.isAssignableFrom(srClass)) {
                    Tr.warning(tc, "warn.service.ref.dd.service.interface.wrong.value", serviceRef.getName(), srClassName);
                    continue;
                }

                jaxwsServiceRefs.add(serviceRef);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The service ref: " + serviceRef.getName() + " refers to the service: " + srClassName);
                }
            }

        }
        return jaxwsServiceRefs;
    }
}
