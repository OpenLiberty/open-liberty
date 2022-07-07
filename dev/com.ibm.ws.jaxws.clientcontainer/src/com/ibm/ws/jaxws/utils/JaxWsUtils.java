/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.utils;

import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.common.util.PackageUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.PackageInfo;

public class JaxWsUtils {

    private static final TraceComponent tc = Tr.register(JaxWsUtils.class);

    public static boolean isWebService(ClassInfo classInfo) {
        return (classInfo.isAnnotationPresent(JaxWsConstants.WEB_SERVICE_ANNOTATION_NAME) || classInfo.isAnnotationPresent(JaxWsConstants.WEB_SERVICE_PROVIDER_ANNOTATION_NAME))
               && isProperWebService(classInfo);
    }

    private static boolean isProperWebService(ClassInfo classInfo) {
        int modifiers = classInfo.getModifiers();
        return (Modifier.isPublic(modifiers) &&
                !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers));
    }

    @SuppressWarnings("deprecation")
    @FFDCIgnore({ Exception.class })
    public static URL resolve(String location, Container container) {
        if (StringUtils.isEmpty(location)) {
            return null;
        }
        URL ret = createAbsoluteURL(location);
        try {
            if (ret == null) {
                Entry entry = container.getEntry(location);
                if (entry != null) {
                    ret = entry.getResource();
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return ret;
    }

    @FFDCIgnore({ MalformedURLException.class })
    private static URL createAbsoluteURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Get serviceName's QName of Web Service
     *
     * @param classInfo
     * @return
     */
    public static QName getServiceQName(ClassInfo classInfo, String seiClassName, String targetNamespace) {
        AnnotationInfo annotationInfo = getAnnotationInfoFromClass(classInfo, "Service QName");
        if (annotationInfo == null) {
            return null;
        }

        //serviceName can only be defined in implementation bean, targetNamespace should be the implemented one.
        return getQName(classInfo, targetNamespace, annotationInfo.getValue(JaxWsConstants.SERVICENAME_ATTRIBUTE).getStringValue(),
                        JaxWsConstants.SERVICENAME_ATTRIBUTE_SUFFIX);

    }

    /**
     * Get the annotation info from current class info,
     * First get @webservice, second get from @webserviceprovider
     *
     * @param classInfo
     * @param attribute
     * @return
     */
    private static AnnotationInfo getAnnotationInfoFromClass(ClassInfo classInfo, String attribute) {
        AnnotationInfo annotationInfo = classInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_ANNOTATION_NAME);
        if (annotationInfo == null) {
            annotationInfo = classInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_PROVIDER_ANNOTATION_NAME);
        }
        if (annotationInfo == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No @WebService or @WebServiceProvider annotation is found on the class " + classInfo + ", will return null or \"\" for " + attribute);
            }
        }
        return annotationInfo;
    }

    /**
     * Get serviceName's localPart of Web Service
     *
     * @param classInfo
     * @return
     */
    public static String getServiceName(ClassInfo classInfo) {
        return getServiceQName(classInfo, null, null).getLocalPart();
    }

    /**
     * Get portName' QName of Web Service
     *
     * @param classInfo
     * @return
     */
    public static QName getPortQName(ClassInfo classInfo, String seiClassName, String targetNamespace) {
        AnnotationInfo annotationInfo = getAnnotationInfoFromClass(classInfo, "Port QName");
        if (annotationInfo == null) {
            return null;
        }

        boolean webServiceProviderAnnotation = isProvider(classInfo);

        String wsName = webServiceProviderAnnotation ? null : annotationInfo.getValue(JaxWsConstants.NAME_ATTRIBUTE).getStringValue();
        return getPortQName(classInfo, targetNamespace, wsName, annotationInfo.getValue(JaxWsConstants.PORTNAME_ATTRIBUTE).getStringValue(),
                            JaxWsConstants.PORTNAME_ATTRIBUTE_SUFFIX);
    }

    /**
     * Only get the Service Endpoint Interface class name from @WebService annotation
     *
     * @param classInfo
     * @return
     */
    public static String getSEIClassNameFromAnnotation(ClassInfo classInfo) {
        AnnotationInfo annotationInfo = getAnnotationInfoFromClass(classInfo, "SEI Name");
        if (annotationInfo == null) {
            return null;
        }

        boolean webServiceProviderAnnotation = isProvider(classInfo);

        return webServiceProviderAnnotation ? null : annotationInfo.getValue(JaxWsConstants.ENDPOINTINTERFACE_ATTRIBUTE).getStringValue();
    }

    /**
     * Judge if is a Web Service Provider.
     *
     * @param classInfo
     * @return
     */
    public static boolean isProvider(ClassInfo classInfo) {
        AnnotationInfo annotationInfo = classInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_ANNOTATION_NAME);
        if (annotationInfo == null) {
            annotationInfo = classInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_PROVIDER_ANNOTATION_NAME);
            if (annotationInfo != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * get the targetNamespace from implementation bean.
     * if can get the targetNamespace attribute from annotation then return it,
     * otherwise return the package name as default value.
     * Both webService and webServiceprovider has the same logic.
     *
     * @param classInfo
     * @return
     */
    public static String getImplementedTargetNamespace(ClassInfo classInfo) {
        String defaultValue = getNamespace(classInfo, null);
        if (StringUtils.isEmpty(defaultValue)) {
            defaultValue = JaxWsConstants.UNKNOWN_NAMESPACE;
        }

        AnnotationInfo annotationInfo = getAnnotationInfoFromClass(classInfo, JaxWsConstants.TARGETNAMESPACE_ATTRIBUTE);
        if (annotationInfo == null) {
            return "";
        }

        AnnotationValue attrValue = annotationInfo.getValue(JaxWsConstants.TARGETNAMESPACE_ATTRIBUTE);
        String attrFromAnnotation = attrValue == null ? null : attrValue.getStringValue().trim();
        return StringUtils.isEmpty(attrFromAnnotation) ? defaultValue : attrFromAnnotation;
    }

    /**
     * get the targetNamespace from SEI.
     * If it is webServiceProvider just return the targetNamespace attribute from annotation.
     * If it is webService and no SEI specified, return the implementedTargetNamespace;
     * If it is webService and SEI specified with no targetNamespace attribute, should report error?
     * If it is webService and SEI specified with targetNamespace attribute, just return the targetNamespace attribute value.
     *
     * @param classInfo
     * @param String
     * @param InfoStore
     * @return
     */
    public static String getInterfaceTargetNamespace(ClassInfo classInfo, String seiClassName, String implementedTargetNamespace, InfoStore infoStore) {
        AnnotationInfo annotationInfo = getAnnotationInfoFromClass(classInfo, JaxWsConstants.TARGETNAMESPACE_ATTRIBUTE);
        if (annotationInfo == null) {
            return "";
        }
        boolean isProvider = isProvider(classInfo);

        // if the serviceImplBean is a WebServiceProvider, return the attribute value or the defaultValue
        if (isProvider) {
            AnnotationValue attrValue = annotationInfo.getValue(JaxWsConstants.TARGETNAMESPACE_ATTRIBUTE);
            String attrFromAnnotation = attrValue == null ? null : attrValue.getStringValue().trim();
            return StringUtils.isEmpty(attrFromAnnotation) ? implementedTargetNamespace : attrFromAnnotation;
        }

        if (null == infoStore || StringUtils.isEmpty(seiClassName)) {
            return implementedTargetNamespace;
        }

        // if can get the SEI className, go here.
        // Here, the SEI package name instead of implementation class package name should be used as the default value for the targetNameSpace
        ClassInfo seiClassInfo = infoStore.getDelayableClassInfo(seiClassName);
        String defaultValue = getNamespace(seiClassInfo, null);
        if (StringUtils.isEmpty(defaultValue)) {
            defaultValue = JaxWsConstants.UNKNOWN_NAMESPACE;
        }

        annotationInfo = seiClassInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_ANNOTATION_NAME);
        if (null == annotationInfo) {// if the SEI does not have the @WebService annotation, we should report it as error? (RI 2.2 will do)
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No @WebService or @WebServiceProvider annotation is found on the class " + seiClassInfo
                             + " will return " + defaultValue);
            }
            return defaultValue;
        }

        // if the attribute is presented in SEI's @WebService, just return it. or, return the default value for Service
        String attrFromSEI = annotationInfo.getValue(JaxWsConstants.TARGETNAMESPACE_ATTRIBUTE).getStringValue().trim();
        return StringUtils.isEmpty(attrFromSEI) ? defaultValue : attrFromSEI;
    }

    /**
     * First, get the WSDL Location.
     *
     * @param classInfo
     * @param seiClassName
     * @param infoStore
     * @return
     */
    public static String getWSDLLocation(ClassInfo classInfo, String seiClassName, InfoStore infoStore) {
        return getStringAttributeFromAnnotation(classInfo, seiClassName, infoStore, JaxWsConstants.WSDLLOCATION_ATTRIBUTE, "", "");
    }

    /**
     * Return the attribute value of WebServiceProvider annotation
     *
     * @param annotationInfo
     * @param attribute
     * @param defaultForServiceProvider
     * @return
     */
    private static String getStringAttributeFromWebServiceProviderAnnotation(AnnotationInfo annotationInfo, String attribute,
                                                                             String defaultForServiceProvider) {
        //the two values can not be found in webserviceProvider annotation so just return the default value to save time
        if (attribute.equals(JaxWsConstants.ENDPOINTINTERFACE_ATTRIBUTE) || attribute.equals(JaxWsConstants.NAME_ATTRIBUTE)) {
            return defaultForServiceProvider;
        }
        AnnotationValue attrValue = annotationInfo.getValue(attribute);
        String attrFromSP = attrValue == null ? null : attrValue.getStringValue().trim();
        return StringUtils.isEmpty(attrFromSP) ? defaultForServiceProvider : attrFromSP;
    }

    /**
     * Get the string value of attribute in WebService or WebserviceProvider annotation.
     *
     * If it is provider, get the attribute from its annotation.
     * If it is not provider,
     * First, try to get the attribute from serviceImplBean,
     * then, try to get the attribute from the SEI either from seiClassName or "endpointInterface" attribute,
     * finally, return the defaultForService if is the WebService annotation, or defaultForServiceProvider for WebServiceProvider annotation.
     *
     * If could not find neither the WebService or WebServiceProvider annotation, just return an empty string.
     *
     * @param classInfo
     * @param seiClassName
     * @param infoStore
     * @param attribute
     * @param defaultForService
     * @param defaultForServiceProvider
     * @return
     */
    private static String getStringAttributeFromAnnotation(ClassInfo classInfo, String seiClassName, InfoStore infoStore, String attribute,
                                                           String defaultForService, String defaultForServiceProvider) {
        AnnotationInfo annotationInfo = getAnnotationInfoFromClass(classInfo, attribute);
        if (annotationInfo == null) {
            return "";
        }
        boolean isProvider = isProvider(classInfo);

        // if the serviceImplBean is a WebServiceProvider, return the attribute value or the defaultValue for ServiceProvider
        if (isProvider) {
            return getStringAttributeFromWebServiceProviderAnnotation(annotationInfo, attribute, defaultForServiceProvider);
        }

        // if is as WebService,  need to get the attribute from itself, the SEI or the interfaces, then the default value for Service
        String attrFromImplBean = annotationInfo.getValue(attribute).getStringValue().trim();
        if (attrFromImplBean.isEmpty()) {
            // can not get the SEI class name just return the default value
            if (seiClassName.isEmpty()) {
                return defaultForService;
            } else {
                // if can get the SEI className, go here.
                ClassInfo seiClassInfo = infoStore.getDelayableClassInfo(seiClassName);

                annotationInfo = seiClassInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_ANNOTATION_NAME);
                if (null == annotationInfo) {// if the SEI does not have the @WebService annotation, we should report it as error? (RI 2.2 will do)
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "No @WebService or @WebServiceProvider annotation is found on the class " + seiClassInfo
                                     + " will return " + defaultForService);
                    }
                    return defaultForService;
                }

                // if the attribute is presented in SEI's @WebService, just return it. or, return the default value for Service
                String attrFromSEI = annotationInfo.getValue(attribute).getStringValue().trim();
                return StringUtils.isEmpty(attrFromSEI) ? defaultForService : attrFromSEI;
            }

        }
        return attrFromImplBean;
    }

    /**
     * Get portName's local part of Web Service
     *
     * @param classInfo
     * @return
     */
    public static String getPortName(ClassInfo classInfo) {
        return getPortQName(classInfo, null, null).getLocalPart();
    }

    /**
     * Get the portComponentName from ClassInfo
     *
     * @param classInfo
     * @return
     */
    public static String getPortComponentName(ClassInfo classInfo, String seiClassName, InfoStore infoStore) {
        String defaultForServiceProvider = classInfo.getName();
        String defaultForService = getClassName(classInfo.getName());
        return getStringAttributeFromAnnotation(classInfo, seiClassName, infoStore, JaxWsConstants.NAME_ATTRIBUTE, defaultForService, defaultForServiceProvider);
    }

    private static String getClassName(String name) {
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return name;
        }
        return name.substring(index + 1);
    }

    private static QName getQName(ClassInfo classInfo, String namespace, String name, String suffix) {
        return new QName(namespace, constructName(classInfo, name, suffix));
    }

    /**
     * Get portName.
     * 1.declared portName in web service annotation
     * 2.name in web service annotation + Port
     * 3.service class name + Port.
     *
     * From specification:
     * The portName element of the WebService annotation, if present, MUST be used to derive the port name to use in WSDL.
     * In the absence of a portName element, an implementation MUST use the value of the name element of the WebService annotation, if present, suffixed with “Port”.
     * Otherwise, an implementation MUST use the simple name of the class annotated with WebService suffixed with “Port”.
     *
     * @param classInfo
     * @param namespace
     * @param wsPortName
     * @param suffix
     * @return
     */
    private static QName getPortQName(ClassInfo classInfo, String namespace, String wsName, String wsPortName, String suffix) {
        String portName;
        if (wsPortName != null && !wsPortName.isEmpty()) {
            portName = wsPortName.trim();
        } else {
            if (wsName != null && !wsName.isEmpty()) {
                portName = wsName.trim();
            } else {
                String qualifiedName = classInfo.getQualifiedName();
                int lastDotIndex = qualifiedName.lastIndexOf(".");
                portName = (lastDotIndex == -1 ? qualifiedName : qualifiedName.substring(lastDotIndex + 1));
            }
            portName = portName + suffix;
        }
        return new QName(namespace, portName);
    }

    public static String getNamespace(ClassInfo classInfo, String namespace) {
        if (namespace == null || namespace.trim().length() == 0) {
            PackageInfo packageInfo = classInfo.getPackage();
            if (packageInfo == null) {
                return null;
            } else {
                return getNamespace(packageInfo.getName());
            }
        } else {
            return namespace.trim();
        }
    }

    private static String getNamespace(String packageName) {
        return PackageUtils.getNamespace(packageName);
    }

    private static String constructName(ClassInfo classInfo, String name, String suffix) {
        if (name == null || name.trim().length() == 0) {
            String qualifiedName = classInfo.getQualifiedName();
            int lastDotIndex = qualifiedName.lastIndexOf(".");
            return (lastDotIndex == -1 ? qualifiedName : qualifiedName.substring(lastDotIndex + 1)) + suffix;
        } else {
            return name.trim();
        }
    }

    @SuppressWarnings("deprecation")
    public static URL getEntryURL(Container container, String entryName) {
        if (entryName == null) {
            return null;
        }

        URL entryURL = null;
        Entry containerEntry = container.getEntry(entryName);
        if (containerEntry != null) {
            entryURL = containerEntry.getResource();
        }

        return entryURL;
    }

    /**
     * Check whether the regQName matches the targetQName
     *
     * @param regQName
     * @param targetQName
     * @return
     */
    public static boolean matchesQName(QName regQName, QName targetQName) {
        return matchesQName(regQName, targetQName, false);
    }

    /**
     * Check whether the regQName matches the targetQName
     *
     * Only the localPart of the regQName supports the * match, it means only the name space and prefix is all matched, then
     * the localPart will be compared considering the *
     *
     * When the ignorePrefix is true, the prefix will be ignored.
     *
     * @param regQName
     * @param targetQName
     * @param ignorePrefix
     * @return
     */
    public static boolean matchesQName(QName regQName, QName targetQName, boolean ignorePrefix) {
        if (regQName == null || targetQName == null) {
            return false;
        }
        if ("*".equals(getQNameString(regQName))) {
            return true;
        }
        // if the name space or the prefix is not equal, just return false;
        if (!(regQName.getNamespaceURI().equals(targetQName.getNamespaceURI())) ||
            !(ignorePrefix || regQName.getPrefix().equals(targetQName.getPrefix()))) {
            return false;
        }

        if (regQName.getLocalPart().contains("*")) {
            return Pattern.matches(mapPattern(regQName.getLocalPart()), targetQName.getLocalPart());
        } else if (regQName.getLocalPart().equals(targetQName.getLocalPart())) {
            return true;
        }
        return false;
    }

    public static ModuleInfo getModuleInfo(Container container) {
        ModuleInfo moduleInfo = null;
        NonPersistentCache overlayCache;
        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to get ModuleInfo due to no NonPersistentCache found");
            }
            return null;
        }
        if (overlayCache != null) {
            moduleInfo = (ModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
            if (moduleInfo == null) {
                moduleInfo = (ModuleInfo) overlayCache.getFromCache(EJBModuleInfo.class);
            }
        }
        return moduleInfo;
    }

    public static WebModuleInfo getWebModuleInfo(Container container) {
        WebModuleInfo moduleInfo = null;
        NonPersistentCache overlayCache;
        try {
            overlayCache = container.adapt(NonPersistentCache.class);
        } catch (UnableToAdaptException e) {
            return null;
        }
        if (overlayCache != null) {
            moduleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
        }
        return moduleInfo;
    }

    private static String mapPattern(String s) {
        StringBuilder buf = new StringBuilder(s);
        for (int x = 0; x < buf.length(); x++) {
            switch (buf.charAt(x)) {
                case '*':
                    buf.insert(x, '.');
                    x++;
                    break;
                case '.':
                case '\\':
                case '^':
                case '$':
                case '{':
                case '}':
                case '(':
                case ')':
                    buf.insert(x, '\\');
                    x++;
                    break;
                default:
                    //nothing to do
                    break;
            }
        }
        return buf.toString();
    }

    private static String getQNameString(QName qName) {
        if ("".equals(qName.getPrefix())) {
            return qName.toString();
        } else {
            int i = qName.getNamespaceURI().length();
            if (i == 0) {
                return qName.getPrefix() + ':' + qName.getLocalPart();
            } else {
                StringBuilder sBuffer = new StringBuilder(i + qName.getLocalPart().length() + qName.getPrefix() + 3);
                sBuffer.append('{');
                sBuffer.append(qName.getNamespaceURI());
                sBuffer.append('}');
                sBuffer.append(qName.getPrefix());
                sBuffer.append(':');
                sBuffer.append(qName.getLocalPart());
                return sBuffer.toString();
            }
        }
    }

    /**
     *
     * @param name
     * @param protocol
     * @return
     */
    public static boolean singleProtocolMatches(String name, String protocol) {
        if (JaxWsConstants.SOAP11_HTTP_TOKEN.equals(name)) {
            return "http://schemas.xmlsoap.org/wsdl/soap/http".contains(protocol)
                   || "http://schemas.xmlsoap.org/soap/".contains(protocol);
        } else if (JaxWsConstants.SOAP11_HTTP_MTOM_TOKEN.equals(name)) {
            return "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true".contains(protocol)
                   || "http://schemas.xmlsoap.org/soap/?mtom=true".contains(protocol);
        } else if (JaxWsConstants.SOAP12_HTTP_TOKEN.equals(name)) {
            return "http://www.w3.org/2003/05/soap/bindings/HTTP/".contains(protocol)
                   || "http://schemas.xmlsoap.org/wsdl/soap12/".contains(protocol);
        } else if (JaxWsConstants.SOAP12_HTTP_MTOM_TOKEN.equals(name)) {
            return "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true".contains(protocol)
                   || "http://schemas.xmlsoap.org/wsdl/soap12/?mtom=true".contains(protocol);
        } else if (JaxWsConstants.XML_HTTP_TOKEN.equals(name)) {
            name = "http://www.w3.org/2004/08/wsdl/http";
        } else if ("http://schemas.xmlsoap.org/wsdl/soap/http".equals(name) ||
                   "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true".equals(name) ||
                   "http://www.w3.org/2003/05/soap/bindings/HTTP/".equals(name) ||
                   "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true".equals(name) ||
                   "http://www.w3.org/2004/08/wsdl/http".equals(name)) {
            return true;
        }
        return name.contains(protocol);
    }

    public static String getProtocolByToken(String token) {
        return getProtocolByToken(token, false);
    }

    /**
     * Get the protocol by token
     *
     * @param token
     * @param returnDefault if true and the token is an empty String or null, return SOAPBinding.SOAP11HTTP_BINDING.
     * @return
     */
    public static String getProtocolByToken(String token, boolean returnDefault) {
        if (StringUtils.isEmpty(token) && returnDefault) {
            return JaxWsConstants.SOAP11HTTP_BINDING;
        }

        if (JaxWsConstants.SOAP11_HTTP_TOKEN.equals(token)) {
            return JaxWsConstants.SOAP11HTTP_BINDING;
        } else if (JaxWsConstants.SOAP11_HTTP_MTOM_TOKEN.equals(token)) {
            return JaxWsConstants.SOAP11HTTP_MTOM_BINDING;
        } else if (JaxWsConstants.SOAP12_HTTP_TOKEN.equals(token)) {
            return JaxWsConstants.SOAP12HTTP_BINDING;
        } else if (JaxWsConstants.SOAP12_HTTP_MTOM_TOKEN.equals(token)) {
            return JaxWsConstants.SOAP12HTTP_MTOM_BINDING;
        } else if (JaxWsConstants.XML_HTTP_TOKEN.equals(token)) {
            return JaxWsConstants.HTTP_BINDING;
        } else {
            return token;
        }
    }

    public static boolean isWSDLGenerationSupported(String bindingProtocol) {
        return SOAPBinding.SOAP11HTTP_BINDING.equals(bindingProtocol) || SOAPBinding.SOAP11HTTP_MTOM_BINDING.equals(bindingProtocol);
    }

    public static boolean isWebModule(Container container) throws UnableToAdaptException {
        NonPersistentCache overlayCache = container.adapt(NonPersistentCache.class);
        if (overlayCache != null) {
            return overlayCache.getFromCache(WebModuleInfo.class) != null;
        }
        return false;
    }

    public static boolean isEJBModule(Container container) throws UnableToAdaptException {
        NonPersistentCache overlayCache = container.adapt(NonPersistentCache.class);
        if (overlayCache != null) {
            return overlayCache.getFromCache(EJBModuleInfo.class) != null;
        }
        return false;
    }

    public static InfoStore getInfoStore(Container container) throws UnableToAdaptException {
        if ( JaxWsUtils.isEJBModule(container) ) {
            return AnnotationsBetaHelper.getModuleAnnotations(container).getInfoStore();
        } else if ( JaxWsUtils.isWebModule(container) ) {
            return AnnotationsBetaHelper.getWebAnnotations(container).getInfoStore();
        }
        return null;
    }
}
