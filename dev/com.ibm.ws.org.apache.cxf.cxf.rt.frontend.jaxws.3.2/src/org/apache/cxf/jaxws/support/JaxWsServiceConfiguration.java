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

package org.apache.cxf.jaxws.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Action;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.Response;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebFault;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.JaxWsConfigurationException;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.wsdl.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

public class JaxWsServiceConfiguration extends AbstractServiceConfiguration {

    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsServiceConfiguration.class);

    private JaxWsImplementorInfo implInfo;
    /**
     * We retrieve the wrapper methods more than once
     * while creating an endpoint. So caching the wrapper
     * classes saves CPU time.
     *
     * It would also be good to cache across creations,
     * but Method.equals isn't good enough.
     */
    private Map<Object, Class<?>> responseMethodClassCache;
    private Map<Object, Class<?>> requestMethodClassCache;
    private List<Method> responseMethodClassNotFoundCache;
    private List<Method> requestMethodClassNotFoundCache;
    private Map<Method, Annotation[][]> methodAnnotationCache;

    public JaxWsServiceConfiguration() {
        responseMethodClassCache = new HashMap<>();
        requestMethodClassCache = new HashMap<>();
        responseMethodClassNotFoundCache = new ArrayList<>();
        requestMethodClassNotFoundCache = new ArrayList<>();
        methodAnnotationCache = new HashMap<>();
    }

    @Override
    public void setServiceFactory(ReflectionServiceFactoryBean serviceFactory) {
        super.setServiceFactory(serviceFactory);
        implInfo = ((JaxWsServiceFactoryBean)serviceFactory).getJaxWsImplementorInfo();
    }

    @Override
    public String getServiceName() {
        QName service = implInfo.getServiceName();
        return service.getLocalPart();
    }

    @Override
    public String getServiceNamespace() {
        String ret = super.getServiceNamespace();
        if (ret != null) {
            return ret;
        }
        QName service = implInfo.getServiceName();
        return service.getNamespaceURI();
    }

    @Override
    public QName getEndpointName() {
        return implInfo.getEndpointName();
    }

    @Override
    public QName getInterfaceName() {
        return implInfo.getInterfaceName();
    }

    @Override
    public String getWsdlURL() {
        String wsdlLocation = implInfo.getWsdlLocation();
        if (wsdlLocation != null && wsdlLocation.length() > 0) {
            return wsdlLocation;
        }
        return null;
    }

    @Override
    public QName getOperationName(InterfaceInfo intf, Method method) {
        method = getDeclaredMethod(method);

        WebMethod wm = method.getAnnotation(WebMethod.class);
        if (wm != null) {
            String name = wm.operationName();
            if (name.length() == 0) {
                name = method.getName();
            }

            return new QName(intf.getName().getNamespaceURI(), name);
        }
        return new QName(intf.getName().getNamespaceURI(), method.getName());
    }

    public Boolean isWebMethod(final Method method) {
        if (method == null
            || method.getReturnType().equals(Future.class)
            || method.getReturnType().equals(Response.class)
            || method.isSynthetic()) {
            return Boolean.FALSE;
        }

        WebMethod wm = method.getAnnotation(WebMethod.class);
        Class<?>  cls = method.getDeclaringClass();
        if ((wm != null) && wm.exclude()) {
            return Boolean.FALSE;
        }
        if ((wm != null && !wm.exclude()) // Liberty change: || (implInfo.getSEIClass() != null && cls.isInterface() is removed
                || (cls.isInterface()     // Liberty change: line added
                && cls.isAssignableFrom(implInfo.getSEIClass()))) {
            return Boolean.TRUE;
        }
        if (method.getDeclaringClass().isInterface()) {
            return hasWebServiceAnnotation(method);
        }
        if (implInfo.getSEIClass() == null) {
            return hasWebServiceAnnotation(method)
                && !Modifier.isFinal(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers());
        }
        return implInfo.getSEIClass().isAssignableFrom(method.getDeclaringClass());
    }

    @Override
    public Boolean isOperation(final Method method) {
        if (Object.class.equals(method.getDeclaringClass())) {
            return false;
        }

        if (method.getDeclaringClass() == implInfo.getSEIClass()) {
            WebMethod wm = method.getAnnotation(WebMethod.class);
            if (wm != null && wm.exclude()) {
                Message message = new Message("WEBMETHOD_EXCLUDE_NOT_ALLOWED", LOG,
                                              method.getName());
                throw new JaxWsConfigurationException(message);
            }
        }


        Class<?> implClz = implInfo.getImplementorClass();
        Method m = getDeclaredMethod(implClz, method);
        if (m != null) {
            WebMethod wm = m.getAnnotation(WebMethod.class);
            if (wm != null && wm.exclude()) {
                return Boolean.FALSE;
            }
        }
        if (isWebMethod(m)) {
            return true;
        }
        return isWebMethod(getDeclaredMethod(method));
    }

    private boolean hasWebServiceAnnotation(Method method) {
        return method.getDeclaringClass().getAnnotation(WebService.class) != null;
    }

    Method getDeclaredMethod(Method method) {
        return getDeclaredMethod(implInfo.getEndpointClass(), method);
    }

    private Method getDeclaredMethod(Class<?> endpointClass, Method method) {
        if (!method.getDeclaringClass().equals(endpointClass)) {
            try {
                method = endpointClass.getMethod(method.getName(), method.getParameterTypes());
            } catch (SecurityException e) {
                throw new ServiceConstructionException(e);
            } catch (NoSuchMethodException e) {
                return isWebMethod(method) ? method : null;
            }
        }
        return method;
    }

    @Override
    public QName getInPartName(OperationInfo op, Method method, int paramNumber) {
        if (paramNumber < 0) {
            return null;
        }

        return getPartName(op, method, paramNumber, op.getInput(), "arg", true);
    }

    @Override
    public QName getInParameterName(OperationInfo op, Method method, int paramNumber) {
        if (paramNumber < 0) {
            return null;
        }

        return getParameterName(op, method, paramNumber, op.getInput().size(), "arg", true);
    }

    private QName getPartName(OperationInfo op, Method method,
                              int paramNumber, MessageInfo mi, String prefix, boolean isIn) {
        int partIndex = getPartIndex(method, paramNumber, isIn);
        method = getDeclaredMethod(method);
        WebParam param = getWebParam(method, paramNumber);
        String tns = mi.getName().getNamespaceURI();
        String local = null;
        if (param != null) {
            if (Boolean.TRUE.equals(isRPC(method))
                || isDocumentBare(method)
                || param.header()) {
                local = param.partName();
            }
            if (local == null || local.length() == 0) {
                local = param.name();
            }
        }

        if (local == null || local.length() == 0) {
            if (Boolean.TRUE.equals(isRPC(method)) || !Boolean.FALSE.equals(isWrapped(method))) {
                local = getDefaultLocalName(op, method, paramNumber, partIndex, prefix);
            } else {
                local = getOperationName(op.getInterface(), method).getLocalPart();
            }
        }

        return new QName(tns, local);
    }

    private int getPartIndex(Method method, int paraNumber, boolean isIn) {
        int ret = 0;
        if (isIn && isInParam(method, paraNumber)) {
            for (int i = 0; i < paraNumber; i++) {
                if (isInParam(method, i)) {
                    ret++;
                }
            }
        }
        if (!isIn && isOutParam(method, paraNumber)) {
            // if (method.getReturnType() != Void.class && method.getReturnType() != Void.TYPE) Liberty change: line removed
            if (!method.getReturnType().equals(Void.class)) { // Liberty change: line added
                ret++;
            }
            for (int i = 0; i < paraNumber; i++) {
                if (isOutParam(method, i)) {
                    ret++;
                }
            }
        }
        return ret;
    }

    private QName getParameterName(OperationInfo op, Method method, int paramNumber,
                                   int curSize, String prefix, boolean input) {
        int partIndex = getPartIndex(method, paramNumber, input);
        method = getDeclaredMethod(method);
        WebParam param = getWebParam(method, paramNumber);
        String tns = null;
        String local = null;
        if (param != null) {
            tns = param.targetNamespace();
            local = param.name();
        }

        if (tns == null || tns.length() == 0) {
            QName wrappername = null;
            if (input) {
                wrappername = getRequestWrapperName(op, method);
            } else {
                wrappername = getResponseWrapperName(op, method);
            }
            if (wrappername != null) {
                tns = wrappername.getNamespaceURI();
            }
        }
        if (tns == null || tns.length() == 0) {
            tns = op.getName().getNamespaceURI();
        }

        if (local == null || local.length() == 0) {
            if (Boolean.TRUE.equals(isRPC(method)) || !Boolean.FALSE.equals(isWrapped(method))) {
                local = getDefaultLocalName(op, method, paramNumber, partIndex, prefix);
            } else {
                local = getOperationName(op.getInterface(), method).getLocalPart();
                if (!input) {
                    local += "Response";
                }
            }
        }

        return new QName(tns, local);
    }

    private String getDefaultLocalName(OperationInfo op, Method method, int paramNumber,
                                       int partIndex, String prefix) {
        String paramName = null;
        if (paramNumber != -1) {
            paramName = prefix + partIndex;
        } else {
            paramName = prefix;
        }
        return paramName;
    }

    private WebParam getWebParam(Method method, int parameter) {
        // we could really use a centralized location for this.
        Annotation[][] annotations = methodAnnotationCache.get(method);
        if (annotations == null) {
            annotations = method.getParameterAnnotations();
            methodAnnotationCache.put(method, annotations);
        }
        if (parameter >= annotations.length) {
            return null;
        }
        for (int i = 0; i < annotations[parameter].length; i++) {
            Annotation annotation = annotations[parameter][i];
            // With the ibm jdk, the condition:
            // if (annotation.annotationType().equals(WebParam.class)) {
            // SOMETIMES returns false even when the annotation type
            // is a WebParam.  Doing an instanceof check or using the
            // == operator seems to give the desired result.
            if (annotation instanceof WebParam) {
                return (WebParam)annotation;
            }
        }
        return null;
    }
    @Override
    public String getRequestWrapperPartName(OperationInfo op, Method method) {
        method = getDeclaredMethod(method);
        RequestWrapper rw = method.getAnnotation(RequestWrapper.class);
        if (rw != null) {
            return getWithReflection(RequestWrapper.class, rw, "partName");
        }
        return null;
    }

    @Override
    public String getResponseWrapperPartName(OperationInfo op, Method method) {
        method = getDeclaredMethod(method);
        WebResult webResult = getWebResult(method);
        ResponseWrapper rw = method.getAnnotation(ResponseWrapper.class);
        if (rw != null) {
            String pn = getWithReflection(ResponseWrapper.class, rw, "partName");
            if (pn != null) {
                return pn;
            }
        }
        int countOut = 0;
        int countHeaders = 0;

        if (webResult != null
            && webResult.header()) {
            countHeaders++;
        } else if (method.getReturnType() != Void.TYPE) {
            countOut++;
        }

        for (int x = 0; x < method.getParameterTypes().length; x++) {
            WebParam parm = getWebParam(method, x);
            if (parm != null) {
                if (parm.header()) {
                    countHeaders++;
                }
                if (parm.mode() != WebParam.Mode.IN) {
                    countOut++;
                }
            }
        }
        if (countHeaders > 0 && countOut == 0) {
            //all outs are headers, thus it's an empty body part
            //thus return the default for an empty part of "result"
            return "result";
        }
        return null;
    }
    public String getFaultMessageName(OperationInfo op, Class<?> exClass, Class<?> beanClass) {
        WebFault f = exClass.getAnnotation(WebFault.class);
        if (f != null) {
            return getWithReflection(WebFault.class, f, "messageName");
        }
        return null;
    }

    private  <T> String getWithReflection(Class<T> cls, T obj, String name) {
        try {
            String s = cls.getMethod(name).invoke(obj).toString();
            if (!StringUtils.isEmpty(s)) {
                return s;
            }
        } catch (Exception e) {
            //ignore = possibly JAX-WS 2.1
        }
        return null;
    }

    @Override
    public QName getOutParameterName(OperationInfo op, Method method, int paramNumber) {
        method = getDeclaredMethod(method);

        if (paramNumber >= 0) {
            return getParameterName(op, method, paramNumber, op.getOutput().size(), "return", false);
        }
        WebResult webResult = getWebResult(method);

        String tns = null;
        String local = null;
        if (webResult != null) {
            tns = webResult.targetNamespace();
            local = webResult.name();
        }
        if (tns == null || tns.length() == 0) {
            QName wrappername = getResponseWrapperName(op, method);
            if (wrappername != null) {
                tns = wrappername.getNamespaceURI();
            }
        }

        if (tns == null || tns.length() == 0) {
            tns = op.getName().getNamespaceURI();
        }

        if (local == null || local.length() == 0) {
            if (Boolean.TRUE.equals(isRPC(method)) || !Boolean.FALSE.equals(isWrapped(method))) {
                local = getDefaultLocalName(op, method, paramNumber, op.getOutput().size(), "return");
            } else {
                local = getOperationName(op.getInterface(),
                                         method).getLocalPart() + "Response";
            }
        }

        return new QName(tns, local);
    }

    @Override
    public QName getOutPartName(OperationInfo op, Method method, int paramNumber) {
        method = getDeclaredMethod(method);

        if (paramNumber >= 0) {
            return getPartName(op, method, paramNumber, op.getOutput(), "return", false);
        }
        WebResult webResult = getWebResult(method);
        String tns = op.getOutput().getName().getNamespaceURI();
        String local = null;
        if (webResult != null) {
            if (Boolean.TRUE.equals(isRPC(method)) || isDocumentBare(method)) {
                local = webResult.partName();
            }
            if (local == null || local.length() == 0) {
                local = webResult.name();
            }
        }

        if (local == null || local.length() == 0) {
            if (Boolean.TRUE.equals(isRPC(method)) || !Boolean.FALSE.equals(isWrapped(method))) {
                local = "return";
            } else {
                local = getOperationName(op.getInterface(), method).getLocalPart() + "Response";
            }
        }

        return new QName(tns, local);
    }

    @Override
    public Boolean isInParam(Method method, int j) {
        if (j < 0) {
            return Boolean.FALSE;
        }

        method = getDeclaredMethod(method);

        WebParam webParam = getWebParam(method, j);

        return webParam == null || (webParam.mode().equals(Mode.IN) || webParam.mode().equals(Mode.INOUT));
    }

    private WebResult getWebResult(Method method) {
        return method.getAnnotation(WebResult.class);
    }

    @Override
    public Boolean isOutParam(Method method, int j) {
        method = getDeclaredMethod(method);
        if (j == -1) {
            return !method.getReturnType().equals(void.class);
        }

        WebParam webParam = getWebParam(method, j);

        if (webParam != null && (webParam.mode().equals(Mode.OUT) || webParam.mode().equals(Mode.INOUT))) {
            return Boolean.TRUE;
        }
        return method.getParameterTypes()[j] == Holder.class;
    }

    @Override
    public Boolean isInOutParam(Method method, int j) {
        method = getDeclaredMethod(method);
        if (j == -1) {
            return !method.getReturnType().equals(void.class);
        }

        WebParam webParam = getWebParam(method, j);

        if (webParam != null && webParam.mode().equals(Mode.INOUT)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    @Override
    public QName getRequestWrapperName(OperationInfo op, Method method) {
        Method m = getDeclaredMethod(method);
        RequestWrapper rw = m.getAnnotation(RequestWrapper.class);
        String nm = null;
        String lp = null;
        if (rw != null) {
            nm = rw.targetNamespace();
            lp = rw.localName();
        }
        WebMethod meth = m.getAnnotation(WebMethod.class);
        if (meth != null && StringUtils.isEmpty(lp)) {
            lp = meth.operationName();
        }
        if (StringUtils.isEmpty(nm)) {
            nm = op.getName().getNamespaceURI();
        }
        if (!StringUtils.isEmpty(nm) && !StringUtils.isEmpty(lp)) {
            return new QName(nm, lp);
        }
        return null;
    }

    @Override
    public QName getResponseWrapperName(OperationInfo op, Method method) {
        Method m = getDeclaredMethod(method);
        ResponseWrapper rw = m.getAnnotation(ResponseWrapper.class);
        String nm = null;
        String lp = null;
        if (rw != null) {
            nm = rw.targetNamespace();
            lp = rw.localName();
        }
        WebMethod meth = m.getAnnotation(WebMethod.class);
        if (meth != null && StringUtils.isEmpty(lp)) {
            lp = meth.operationName();
            if (!StringUtils.isEmpty(lp)) {
                lp += "Response";
            }
        }
        if (StringUtils.isEmpty(nm)) {
            nm = op.getName().getNamespaceURI();
        }
        if (!StringUtils.isEmpty(nm) && !StringUtils.isEmpty(lp)) {
            return new QName(nm, lp);
        }
        return null;
    }


    @Override
    public Class<?> getResponseWrapper(Method selected) {
        if (this.responseMethodClassNotFoundCache.contains(selected)) {
            return null;
        }
        Class<?> cachedClass = responseMethodClassCache.get(selected);
        if (cachedClass != null) {
            return cachedClass;
        }

        Method m = getDeclaredMethod(selected);

        ResponseWrapper rw = m.getAnnotation(ResponseWrapper.class);
        String clsName = "";
        if (rw == null) {
            clsName = getPackageName(selected) + ".jaxws." + StringUtils.capitalize(selected.getName())
                      + "Response";
        } else {
            clsName = rw.className();
        }

        if (clsName.length() > 0) {
            cachedClass = responseMethodClassCache.get(clsName);
            if (cachedClass != null) {
                responseMethodClassCache.put(selected, cachedClass);
                return cachedClass;
            }
            try {
                Class<?> r = ClassLoaderUtils.loadClass(clsName, implInfo.getEndpointClass());
                responseMethodClassCache.put(clsName, r);
                responseMethodClassCache.put(selected, r);

                if (r.equals(m.getReturnType())) {
                    LOG.log(Level.WARNING, "INVALID_RESPONSE_WRAPPER", new Object[] {clsName,
                            m.getReturnType().getName()});
                }

                return r;
            } catch (ClassNotFoundException e) {
                //do nothing, we will mock a schema for wrapper bean later on
            }
        }
        responseMethodClassNotFoundCache.add(selected);
        return null;
    }

    @Override
    public String getResponseWrapperClassName(Method selected) {
        Method m = getDeclaredMethod(selected);

        ResponseWrapper rw = m.getAnnotation(ResponseWrapper.class);
        String clsName = "";
        if (rw != null) {
            clsName = rw.className();
        }
        if (clsName.length() > 0) {
            return clsName;
        }
        return null;
    }
    public String getRequestWrapperClassName(Method selected) {
        Method m = getDeclaredMethod(selected);

        RequestWrapper rw = m.getAnnotation(RequestWrapper.class);
        String clsName = "";
        if (rw != null) {
            clsName = rw.className();
        }
        if (clsName.length() > 0) {
            return clsName;
        }
        return null;
    }

    @Override
    public Class<?> getRequestWrapper(Method selected) {
        if (this.requestMethodClassNotFoundCache.contains(selected)) {
            return null;
        }
        Class<?> cachedClass = requestMethodClassCache.get(selected);
        if (cachedClass != null) {
            return cachedClass;
        }

        Method m = getDeclaredMethod(selected);

        RequestWrapper rw = m.getAnnotation(RequestWrapper.class);
        String clsName = "";
        if (rw == null) {
            clsName = getPackageName(selected) + ".jaxws." + StringUtils.capitalize(selected.getName());
        } else {
            clsName = rw.className();
        }

        if (clsName.length() > 0) {
            cachedClass = requestMethodClassCache.get(clsName);
            if (cachedClass != null) {
                requestMethodClassCache.put(selected, cachedClass);
                return cachedClass;
            }
            try {
                Class<?> r = ClassLoaderUtils.loadClass(clsName, implInfo.getEndpointClass());
                requestMethodClassCache.put(clsName, r);
                requestMethodClassCache.put(selected, r);
                if (m.getParameterTypes().length == 1 && r.equals(m.getParameterTypes()[0])) {
                    LOG.log(Level.WARNING, "INVALID_REQUEST_WRAPPER", new Object[] {clsName,
                            m.getParameterTypes()[0].getName()});
                }
                return r;
            } catch (ClassNotFoundException e) {
                //do nothing, we will mock a schema for wrapper bean later on
            }
        }
        requestMethodClassNotFoundCache.add(selected);
        return null;
    }

    private static String getPackageName(Method method) {
        return PackageUtils.getPackageName(method.getDeclaringClass());
    }

    @Override
    public QName getFaultName(InterfaceInfo service, OperationInfo o, Class<?> exClass, Class<?> beanClass) {
        WebFault fault = exClass.getAnnotation(WebFault.class);
        if (fault != null) {
            String name = fault.name();
            if (name.length() == 0) {
                name = exClass.getSimpleName();
            }
            String ns = fault.targetNamespace();
            if (ns.length() == 0) {
                ns = service.getName().getNamespaceURI();
            }

            return new QName(ns, name);
        }
        return null;
    }

    @Override
    public Boolean isWrapped(Method m) {
        // see if someone overrode the default value
        if (getServiceFactory().getWrapped() != null) {
            return getServiceFactory().getWrapped();
        }
        m = getDeclaredMethod(m);

        SOAPBinding ann = m.getAnnotation(SOAPBinding.class);
        if (ann != null) {
            if (ann.style().equals(Style.RPC)) {
                Message message = new Message("SOAPBinding_MESSAGE_RPC", LOG, m.getName());
                throw new Fault(new JaxWsConfigurationException(message));
            }
            return !(ann.parameterStyle().equals(ParameterStyle.BARE));
        }

        return isWrapped();
    }

    @Override
    public Boolean isWrapped() {
        SOAPBinding ann = implInfo.getEndpointClass().getAnnotation(SOAPBinding.class);
        if (ann != null) {
            return !(ann.parameterStyle().equals(ParameterStyle.BARE) || ann.style().equals(Style.RPC));
        }
        return null;
    }

    @Override
    public Boolean isHeader(Method method, int j) {
        method = getDeclaredMethod(method);
        if (j >= 0) {
            WebParam webParam = getWebParam(method, j);
            return webParam != null && webParam.header();
        }
        WebResult webResult = getWebResult(method);
        return webResult != null && webResult.header();
    }

    @Override
    public String getStyle() {
        SOAPBinding ann = implInfo.getEndpointClass().getAnnotation(SOAPBinding.class);
        if (ann != null) {
            return ann.style().toString().toLowerCase();
        }
        return super.getStyle();
    }

    private boolean isDocumentBare(Method method) {
        SOAPBinding ann = method.getAnnotation(SOAPBinding.class);
        if (ann != null) {
            return ann.style().equals(SOAPBinding.Style.DOCUMENT)
                   && ann.parameterStyle().equals(SOAPBinding.ParameterStyle.BARE);
        }
        ann = implInfo.getEndpointClass().getAnnotation(SOAPBinding.class);
        if (ann != null) {
            return ann.style().equals(SOAPBinding.Style.DOCUMENT)
                   && ann.parameterStyle().equals(SOAPBinding.ParameterStyle.BARE);
        }
        return false;
    }

    @Override
    public Boolean isRPC(Method method) {
        SOAPBinding ann = method.getAnnotation(SOAPBinding.class);
        if (ann != null) {
            return ann.style().equals(SOAPBinding.Style.RPC);
        }
        ann = implInfo.getEndpointClass().getAnnotation(SOAPBinding.class);
        if (ann != null) {
            return ann.style().equals(SOAPBinding.Style.RPC);
        }
        return super.isRPC(method);
    }


    @Override
    public Boolean hasOutMessage(Method method) {
        method = getDeclaredMethod(method);
        return !method.isAnnotationPresent(Oneway.class);
    }

    @Override
    public String getAction(OperationInfo op, Method method) {
        method = getDeclaredMethod(method);
        WebMethod wm = method.getAnnotation(WebMethod.class);
        String action = "";
        if (wm != null) {
            action = wm.action();
        }
        if (StringUtils.isEmpty(action)) {
            Action act = method.getAnnotation(Action.class);
            if (act != null) {
                action = act.input();
            }
        }
        return action;
    }
    public Boolean isHolder(Class<?> cls, Type type) {
        return Holder.class.equals(cls);
    }

    public Type getHolderType(Class<?> cls, Type type) {
        if (cls.equals(Holder.class) && type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType)type;
            return paramType.getActualTypeArguments()[0];
        }

        return cls;
    }

    public Boolean isWrapperPartQualified(MessagePartInfo mpi) {
        Annotation[] annotations = (Annotation[])mpi.getProperty("parameter.annotations");
        if (annotations != null) {
            for (Annotation an : annotations) {
                String tns = null;
                if (an instanceof WebParam) {
                    tns = ((WebParam)an).targetNamespace();
                } else if (an instanceof WebResult) {
                    tns = ((WebResult)an).targetNamespace();
                }
                if (tns != null && !StringUtils.isEmpty(tns)) {
                    return Boolean.TRUE;
                }
            }
        }
        return null;
    }

    public Long getWrapperPartMinOccurs(MessagePartInfo mpi) {
        Annotation[] a = (Annotation[])mpi.getProperty(ReflectionServiceFactoryBean.PARAM_ANNOTATION);
        if (a != null) {
            for (Annotation a2 : a) {
                if (a2 instanceof XmlElement) {
                    XmlElement e = (XmlElement)a2;
                    if (e.required()) {
                        return 1L;
                    }
                }
            }
        }
        return null;
    }


}
