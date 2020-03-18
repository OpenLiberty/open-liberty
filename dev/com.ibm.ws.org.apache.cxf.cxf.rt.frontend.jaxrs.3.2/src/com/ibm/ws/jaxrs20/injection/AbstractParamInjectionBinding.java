/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs20.injection;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.model.BeanResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;
import com.ibm.ws.jaxrs20.injection.metadata.ParamInjectionMetadata;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionSimpleBinding;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

public abstract class AbstractParamInjectionBinding<A extends Annotation> extends InjectionSimpleBinding<A> {

    private static final TraceComponent tc = Tr.register(AbstractParamInjectionBinding.class);

    public AbstractParamInjectionBinding(A annotation,
                                         ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(annotation, nameSpaceConfig);
    }

    @Override
    public boolean isResolved() {
        /* allow injection without a java:comp binding */
        return true;
    }

    abstract protected Object getJAXRSValueFromContext(A annotation,
                                                       Class<?> classType,
                                                       Type genericType,
                                                       Annotation[] memberAnnotations,
                                                       Member member,
                                                       ParamInjectionMetadata paramInjectionMetadata) throws IOException;

    @Override
    public Object getInjectionObject(Object target, InjectionTargetContext targetContext) throws InjectionException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getInjectionObject");
        }

        InjectionTarget it = getInjectionTarget();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Injection target {0} for this {1} injection binding", new Object[] { it, this });
        }

        InjectionRuntimeContext irc = InjectionRuntimeContextHelper.getRuntimeContext();

        if (irc == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "Could not get Injection Runtime Context so not injecting for injection binding {0}",
                         new Object[] { this });
            }
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "getInjectionObject");
            }
            return null;
        }

        ParamInjectionMetadata pimd = (ParamInjectionMetadata) irc.getRuntimeCtxObject(ParamInjectionMetadata.class.getName());

        if (pimd == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "Could not get Param Injection Metadata so not injecting for injection binding {0}",
                         new Object[] { this });
            }
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "getInjectionObject");
            }
            return null;
        }

        Object value = null;

        Member itMember = it.getMember();
        Annotation[] annotations = new Annotation[0];
        Type t = null;

        if (itMember instanceof Field) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "itMember was a field instance");
            }
            Field f = (Field) itMember;
            annotations = f.getAnnotations();
            t = f.getGenericType();

        } else if (itMember instanceof Method) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "itMember was a method instance");
            }
            Method m = (Method) itMember;

            annotations = m.getParameterAnnotations()[0];

            if (annotations == null || annotations.length == 0) {
                annotations = m.getAnnotations();
            }

            t = m.getGenericParameterTypes()[0];

        }

        try {
            value = getJAXRSValueFromContext(getAnnotation(),
                                             it.getInjectionClassType(),
                                             t,
                                             annotations,
                                             it.getMember(),
                                             pimd);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "value from instance was {0}", new Object[] { value });
            }

            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "getInjectionObject", new Object[] { value });
            }
            return value;
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not inject into the member {0}, " + e.getMessage(), new Object[] { it
                                .getMember() });
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getInjectionObject", new Object[] { value });
        }
        return value;
    }

    /**
     * real create the paramter object based CXF implementation
     * 
     * @param classType
     * @param genericType
     * @param memberAnnotations
     * @param paramInjectionMetadata
     * @return
     */
    protected Object getInjectedObjectFromCXF(Class<?> classType,
                                              Type genericType,
                                              Annotation[] memberAnnotations,
                                              ParamInjectionMetadata paramInjectionMetadata) {
        Parameter p = ResourceUtils.getParameter(0, memberAnnotations, classType);
        Object injectedObject = null;

        Message message = paramInjectionMetadata.getInMessage();
        OperationResourceInfo ori = paramInjectionMetadata.getOperationResourceInfo();
        BeanResourceInfo cri = ori.getClassResourceInfo();
        MultivaluedMap<String, String> values = (MultivaluedMap<String, String>) ((MessageImpl) message).getTemplateParameters();
        if (p.getType() == ParameterType.BEAN && cri instanceof ClassResourceInfo) {
            injectedObject = JAXRSUtils.createBeanParamValue(message, classType, ori);
        } else {
            injectedObject = JAXRSUtils.createHttpParameterValue(p,
                                                                 classType,
                                                                 genericType,
                                                                 memberAnnotations,
                                                                 message,
                                                                 values,
                                                                 ori);
        }
        return injectedObject;
    }
}
