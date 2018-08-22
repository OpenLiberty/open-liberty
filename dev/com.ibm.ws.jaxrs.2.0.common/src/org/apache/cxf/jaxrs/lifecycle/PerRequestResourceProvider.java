/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;

import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.injection.InjectionRuntimeContextHelper;

/**
 * The default per-request resource provider which creates
 * a new resource instance per every request
 */
public class PerRequestResourceProvider implements ResourceProvider {
    private final Constructor<?> c;
    private final Method postConstructMethod;
    private final Method preDestroyMethod;
    private final Class<?>[] params;
    private final Annotation[][] anns;
    private final Type[] genericTypes;

    public PerRequestResourceProvider(Class<?> clazz) {
        c = ResourceUtils.findResourceConstructor(clazz, true);
        if (c == null) {
            throw new RuntimeException("Resource class " + clazz
                                       + " has no valid constructor");
        }
        params = c.getParameterTypes();
        anns = c.getParameterAnnotations();
        genericTypes = c.getGenericParameterTypes();
        postConstructMethod = ResourceUtils.findPostConstructMethod(clazz);
        preDestroyMethod = ResourceUtils.findPreDestroyMethod(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getInstance(Message m) {
        return createInstance(m);
    }

    protected Object createInstance(Message m) {
        ProviderInfo<?> application =
                        (ProviderInfo<?>) m.getExchange().getEndpoint().get(Application.class.getName());
        Map<Class<?>, Object> mapValues = CastUtils.cast(application == null ? null
                        : Collections.singletonMap(Application.class, application.getProvider()));
        Object[] values = ResourceUtils.createConstructorArguments(c, m, true, mapValues, params, anns, genericTypes);
        try {
            Object instance = values.length > 0 ? c.newInstance(values) : c.newInstance(new Object[] {});
//Liberty Change for CXF Begin
            //do not call postConstruct here as no context injection happens
//            InjectionUtils.invokeLifeCycleMethod(instance, postConstructMethod);
//Liberty Change for CXF End
            return instance;
        } catch (InstantiationException ex) {
            String msg = "Resource class " + c.getDeclaringClass().getName() + " can not be instantiated";
            throw ExceptionUtils.toInternalServerErrorException(null, serverError(msg));
        } catch (IllegalAccessException ex) {
            String msg = "Resource class " + c.getDeclaringClass().getName() + " can not be instantiated"
                         + " due to IllegalAccessException";
            throw ExceptionUtils.toInternalServerErrorException(null, serverError(msg));
        } catch (InvocationTargetException ex) {
            Response r = JAXRSUtils.convertFaultToResponse(ex.getCause(), m);
            if (r != null) {
                m.getExchange().put(Response.class, r);
                throw new WebApplicationException();
            }
            String msg = "Resource class "
                         + c.getDeclaringClass().getName() + " can not be instantiated"
                         + " due to InvocationTargetException";
            throw ExceptionUtils.toInternalServerErrorException(null, serverError(msg));
        }

    }

    private Response serverError(String msg) {
        return JAXRSUtils.toResponseBuilder(500).entity(msg).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseInstance(Message m, Object o) {
//Liberty Change for CXF Begain
        //if not managed by CDI or EJB, then call preDestory by ourself.
        //otherwise, perDestory has already been called by CDI/EJB
        JaxRsFactoryBeanCustomizer beanCustomizer = null;
        if (o != null)
        {
            Bus bus = m.getExchange().getBus();
            beanCustomizer = InjectionRuntimeContextHelper.findBeanCustomizer(o.getClass(), bus);
        }
        if (beanCustomizer == null)
        {
            InjectionUtils.invokeLifeCycleMethod(o, preDestroyMethod);
        }
//Liberty Change for CXF End
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getResourceClass() {
        return c.getDeclaringClass();
    }

}
