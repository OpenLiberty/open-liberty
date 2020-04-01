/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.injection.InjectionRuntimeContextHelper;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;
import com.ibm.ws.jaxrs20.injection.metadata.ParamInjectionMetadata;
import com.ibm.ws.jaxrs20.server.component.JaxRsBeanValidation;

/**
 * LibertyJaxRsInvoker helps to finish call for POJO, EJB & CDI resource
 */
public class LibertyJaxRsInvoker extends JAXRSInvoker {

    private final static TraceComponent tc = Tr.register(LibertyJaxRsInvoker.class);

    private final Class cxfBeanValidationProviderClass;
    private final Map<String, Method> cxfBeanValidationProviderMethodsMap = new HashMap<String, Method>();

    private final boolean isEnableBeanValidation;
    private final LibertyJaxRsServerFactoryBean libertyJaxRsServerFactoryBean;

    private final BeanValidationFaultListener beanValidationFaultListener;

    /**
     * as there may be no any javax.validation, in case of class unresolved issue, better to reflect call.
     */
    private volatile Object beanValidationProvider = null;
    private static final String cxfBeanValidationProviderClassName = "org.apache.cxf.validation.BeanValidationProvider";//"com.ibm.ws.jaxrs20.beanvalidation.component.BeanValidationProviderLocal";
    private boolean validateServiceObject = true;

    //private ClassLoader commonBundleClassLoader = null;

    private static class BeanValidationFaultListener implements FaultListener {

        final Class<? extends RuntimeException> cve;

        BeanValidationFaultListener(Class<? extends RuntimeException> cve) {
            this.cve = cve;
        }

        @Override
        public boolean faultOccurred(Exception exception, String description, Message message) {
            return !cve.isInstance(exception);
        }
    }

    @FFDCIgnore({ ClassNotFoundException.class })
    public LibertyJaxRsInvoker(LibertyJaxRsServerFactoryBean libertyJaxRsServerFactoryBean, boolean isEnableBeanValidation) {
        super();
        this.libertyJaxRsServerFactoryBean = libertyJaxRsServerFactoryBean;
        this.isEnableBeanValidation = isEnableBeanValidation;
        //this.commonBundleClassLoader = commonBundleClassLoader;

        if (!isEnableBeanValidation) {
            cxfBeanValidationProviderClass = null;
            beanValidationFaultListener = null;
        } else {
            //get BeanValidationProviderClass from jaxrs-2.0 and beanValidation auto feature
            //instead of from common bundle as common bundle can not see the javax.validation.* any more
            //cxfBeanValidationProviderClass = loadCXFBeanValidationProviderClass();
            cxfBeanValidationProviderClass = JaxRsBeanValidation.getBeanValidationProviderClass();

            if (cxfBeanValidationProviderClass == null) {
                beanValidationFaultListener = null;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {

                    Tr.debug(tc, "Bean Validation Provider Class not found");
                }

                return;
            }

            cacheValidationMethod("validateBean", new Class[] { Object.class });
            cacheValidationMethod("validateParameters", new Class[] { Object.class, Method.class, Object[].class });
            cacheValidationMethod("validateReturnValue", new Class[] { Object.class, Method.class, Object.class });

            // Since BeanValidation is enabled, if an exception is a ConstraintViolationException
            // then we will want to put a FaultListener on the message so that
            // when the exception bubbles up to PhaseInterceptorChain that we do not
            // use default logging which will log this exception.  BeanValidation is
            // supposed to block logging these messages.
            ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                @Override
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });

            BeanValidationFaultListener listener = null;
            try {
                @SuppressWarnings("unchecked")
                final Class<? extends RuntimeException> cve = (Class<? extends RuntimeException>) loader.loadClass("javax.validation.ConstraintViolationException");
                listener = new BeanValidationFaultListener(cve);
            } catch (ClassNotFoundException e) {
                // If this exception cannot be loaded then we are not doing bean validation
            }
            beanValidationFaultListener = listener;
        }
    }

    /**
     * @param methodName
     * @param paramTypes
     */
    private void cacheValidationMethod(String methodName, Class[] paramTypes) {
        try {
            Method m = cxfBeanValidationProviderClass.getMethod(methodName, paramTypes);
            cxfBeanValidationProviderMethodsMap.put(methodName, m);
        } catch (NoSuchMethodException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Can't find method \"" + methodName + "\" for Bean Validation Provider. " + e.getMessage());
            }
        } catch (SecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Can't access method \"" + methodName + "\" for Bean Validation Provider due to security issue. " + e.getMessage());
            }
        }
    }

    /**
     * using LibertyJaxRsServerFactoryBean.performInvocation to support POJO, EJB, CDI resource
     */
    @Override
    protected Object performInvocation(Exchange exchange, Object serviceObject, Method m, Object[] paramArray) throws Exception {
        paramArray = insertExchange(m, paramArray, exchange);
        return this.libertyJaxRsServerFactoryBean.performInvocation(exchange, serviceObject, m, paramArray);
    }

//    /**
//     * using LibertyJaxRsServerFactoryBean.getActualServiceObject to support POJO, EJB, CDI resource
//     */
//    @Override
//    protected Object getActualServiceObject(Exchange exchange, Object rootInstance) {
//        Object serviceObject = super.getActualServiceObject(exchange, rootInstance);
//        return this.LibertyJaxRsServerFactoryBean.getActualServiceObject(exchange, serviceObject);
//    }

    //per-request resource without context injection is replaced here.
//    @Override
//    public Object getServiceObject(Exchange exchange) {
//
//        Object root = exchange.remove(JAXRSUtils.ROOT_INSTANCE);
//        if (root != null) {
//            return root;
//        }
//
//        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
//        ClassResourceInfo cri = ori.getClassResourceInfo();
//
//        Object serviceObject = cri.getResourceProvider().getInstance(exchange.getInMessage());
//        if ((!cri.contextsAvailable() && !cri.paramsAvailable()) && !cri.isSingleton()) {
//            Object o = this.libertyJaxRsServerFactoryBean.getServiceObject(exchange, serviceObject);
//            if (o != null) {
//                return o;
//            }
//        }
//        return serviceObject;
//    }

    @Override
    @FFDCIgnore({ RuntimeException.class })
    public Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params) {

        //bean customizer....
        final Object realServiceObject;

        final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        final ClassResourceInfo cri = ori.getClassResourceInfo();
        //replace with CDI or EJB for per-request resource classes, in this place InjectionUtil has already
        // put related context object into ThreadLocal
        //SingleTon resources's replacement is put in InjectionUtil.injectContextProxiesAndApplication() method

        if (!cri.isSingleton()) {
            Class<?> clazz = serviceObject.getClass();
            JaxRsFactoryBeanCustomizer beanCustomizer = libertyJaxRsServerFactoryBean.findBeanCustomizer(clazz);
            if (beanCustomizer != null) {

                realServiceObject = beanCustomizer.beforeServiceInvoke(serviceObject,
                                                                       cri.isSingleton(),
                                                                       libertyJaxRsServerFactoryBean.getBeanCustomizerContext(beanCustomizer));
                if (realServiceObject == serviceObject && !beanCustomizer.getClass().getName().equalsIgnoreCase("com.ibm.ws.jaxrs20.ejb.JaxRsFactoryBeanEJBCustomizer")
                    && !cri.contextsAvailable() && !cri.paramsAvailable()) {
                    //call postConstruct method if it has not been repleaced with EJB/CDI for per-request resources
                    Method postConstructMethod = ResourceUtils.findPostConstructMethod(realServiceObject.getClass());
                    InjectionUtils.invokeLifeCycleMethod(realServiceObject, postConstructMethod);

                }

            } else {
                realServiceObject = serviceObject;

                if (!cri.contextsAvailable() && !cri.paramsAvailable()) {

                    //if bean customizer is null, means it is a pojo, we need to call postConstruct here
                    Method postConstructMethod = ResourceUtils.findPostConstructMethod(serviceObject.getClass());
                    InjectionUtils.invokeLifeCycleMethod(serviceObject, postConstructMethod);
                }

            }

        } else {
            realServiceObject = serviceObject;
        }

        //
        Message message = JAXRSUtils.getCurrentMessage();

        Object theProvider = null;
        if (isEnableBeanValidation && cxfBeanValidationProviderClass != null) {

            theProvider = getProvider(message);

            try {
                if (isValidateServiceObject()) {
                    //theProvider.validateBean(serviceObject);
                    callValidationMethod("validateBean", new Object[] { realServiceObject }, theProvider);

                }
                //theProvider.validateParameters(serviceObject, m, params.toArray());
                callValidationMethod("validateParameters", new Object[] { realServiceObject, m, params.toArray() }, theProvider);

            } catch (RuntimeException e) {
                // Since BeanValidation is enabled, if this exception is a ConstraintViolationException
                // then we will want to put a FaultListener on the message so that
                // when this exception bubbles up to PhaseInterceptorChain that we do not
                // use default logging which will log this exception.  BeanValidation is
                // supposed to block logging these messages.
                if (beanValidationFaultListener != null && beanValidationFaultListener.cve.isInstance(e)) {
                    Message m2 = exchange.getInMessage();
                    m2.put(FaultListener.class.getName(), beanValidationFaultListener);
                }
                //re-throw exception.  If the FaultListener is set then a ConstraintViolation will not
                //be logged in the messages.log.
                throw e;

            }
        }

        Object response = super.invoke(exchange, realServiceObject, m, params);

        if (isEnableBeanValidation && cxfBeanValidationProviderClass != null && theProvider != null) {

            if (response instanceof MessageContentsList) {
                MessageContentsList list = (MessageContentsList) response;
                if (list.size() == 1) {
                    Object entity = list.get(0);

                    if (entity instanceof Response) {
                        //theProvider.validateReturnValue(serviceObject, m, ((Response) entity).getEntity());
                        callValidationMethod("validateReturnValue", new Object[] { realServiceObject, m, ((Response) entity).getEntity() }, theProvider);

                    } else {
                        //theProvider.validateReturnValue(serviceObject, m, entity);
                        callValidationMethod("validateReturnValue", new Object[] { realServiceObject, m, entity }, theProvider);

                    }
                }
            }
        }

        return response;
    }

    /**
     * CXF wishes to use a singleton Provider, but a little tricky code?
     * seems the beanValidationProvider instance can be changed if message.getContextualProperty,
     * so only during last instance change and next instance change, the instance is in singleton multi-thread mode
     *
     * @param message
     * @return
     */
    protected Object getProvider(Message message) {

        if (this.beanValidationProvider == null) {
            Object prop = message.getContextualProperty(this.cxfBeanValidationProviderClassName);
            if (prop != null) {
                this.beanValidationProvider = prop;
            } else {

                try {
                    /**
                     * get cxfBeanValidationProviderClass from jaxrs-2.0 and beanValidation auto feature project
                     * com.ibm.ws.jaxrs20.beanvalidation.component.BeanValidationProviderLocal
                     */
                    this.beanValidationProvider = cxfBeanValidationProviderClass.newInstance();
                } catch (InstantiationException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "init Bean Validation Provider fails. " + e.getMessage());
                    }
                } catch (IllegalAccessException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Can't access the initialization of Bean Validation Provider. " + e.getMessage());
                    }
                }
            }
        }
        return this.beanValidationProvider;
    }

//    /**
//     * @return
//     * @throws ClassNotFoundException
//     */
//    private Class loadCXFBeanValidationProviderClass() {
//        Class c = null;
//        try {
//            c = this.commonBundleClassLoader.loadClass(this.cxfBeanValidationProviderClassName);
//        } catch (ClassNotFoundException e) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Class \"" + this.cxfBeanValidationProviderClassName + "\" Not Found" + e.getMessage());
//            }
//        }
//        return c;
//    }

    /**
     * call validation method
     * ignore the exception to pass the FAT
     *
     * @param methodName
     * @param paramValues
     * @param theProvider
     */
    @FFDCIgnore(value = { SecurityException.class, IllegalAccessException.class, IllegalArgumentException.class, InvocationTargetException.class })
    private void callValidationMethod(String methodName, Object[] paramValues, Object theProvider) {
        if (theProvider == null) {
            return;
        }

        Method m = cxfBeanValidationProviderMethodsMap.get(methodName);

        if (m == null) {
            return;
        }
        
        ValidateOnExecution validateOnExec = m.getAnnotation(ValidateOnExecution.class);
        if (validateOnExec != null) {
            ExecutableType[] execTypes = validateOnExec.type();
            if (execTypes.length == 1 && execTypes[0] == ExecutableType.NONE) {
                return;
            }
        }

        try {
            m.invoke(theProvider, paramValues);
        } catch (SecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Can't access the method \"" + m.getName() + "\" due to security issue." + e.getMessage());
            }
        } catch (IllegalAccessException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Can't access the method \"" + m.getName() + "\"." + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Illegal argument to the method \"" + m.getName() + "\"." + e.getMessage());
            }
        } catch (InvocationTargetException e) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invocation of the method \"" + m.getName() + "\" fails" + e.getMessage());
            }

            /**
             * throw the javax.validation.ValidationException to keep the same behavior as CXF expected
             */
            Throwable validationException = e.getTargetException();
            if (null != validationException && validationException instanceof RuntimeException) {
                throw (RuntimeException) validationException;
            }

        }
    }

    public void setProvider(Object provider) {
        if (provider == null)
            return;

        if (!provider.getClass().getName().equals(this.cxfBeanValidationProviderClassName))
            return;

        this.beanValidationProvider = provider;
    }

    public boolean isValidateServiceObject() {
        return validateServiceObject;
    }

    public void setValidateServiceObject(boolean validateServiceObject) {
        this.validateServiceObject = validateServiceObject;
    }

    /**
     * override this method for EJB & CDI context&parameter field/setter method injection
     * if the resourceObject is actually a EJB or CDI bean, then store the injection objects
     * when Liberty injection engine triggers the injection, retrieve the injection objects in XXXParamInjecitonBinding or ContextObjectFactory
     */
    @Override
    public Object invoke(Exchange exchange, Object request, Object resourceObject) {

        JaxRsFactoryBeanCustomizer beanCustomizer = libertyJaxRsServerFactoryBean.findBeanCustomizer(resourceObject.getClass());
        //only for EJB or CDI
        if (beanCustomizer != null) {

            final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
            final Message inMessage = exchange.getInMessage();

            /**
             * prepare the CXF objects for param injection
             * real injection should happens in XXXParamInjectionBinding
             */
            InjectionRuntimeContext irc = InjectionRuntimeContextHelper.getRuntimeContext();
            ParamInjectionMetadata pimd = new ParamInjectionMetadata(ori, inMessage);
            irc.setRuntimeCtxObject(ParamInjectionMetadata.class.getName(), pimd);
        }

        //for EJB or CDI or POJO
        return super.invoke(exchange, request, resourceObject);
    }
}
