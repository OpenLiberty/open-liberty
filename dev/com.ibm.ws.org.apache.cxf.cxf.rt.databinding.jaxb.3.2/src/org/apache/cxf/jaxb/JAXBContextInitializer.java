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

package org.apache.cxf.jaxb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.UnwrappedOperationInfo;

/**
 * Walks the service model and sets up the classes for the context.
 */
class JAXBContextInitializer extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBContextInitializer.class);
    private Set<Class<?>> classes;
    private Collection<Object> typeReferences;
    private Set<Class<?>> globalAdapters = new HashSet<>();
    private Map<String, Object> unmarshallerProperties;
    private Bus bus;

    JAXBContextInitializer(Bus bus, ServiceInfo serviceInfo,
                           Set<Class<?>> classes,
                           Collection<Object> typeReferences,
                           Map<String, Object> unmarshallerProperties) {
        super(serviceInfo);
        this.classes = classes;
        this.typeReferences = typeReferences;
        this.unmarshallerProperties = unmarshallerProperties;
        this.bus = bus;
    }

    @Override
    public void begin(MessagePartInfo part) {
        Class<?> clazz = part.getTypeClass();
        if (clazz == null) {
            return;
        }

        if (Exception.class.isAssignableFrom(clazz)) {
            //exceptions are handled special, make sure we mark it
            part.setProperty(JAXBDataBinding.class.getName() + ".CUSTOM_EXCEPTION",
                             Boolean.TRUE);
        }
        boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
        if (isFromWrapper
            && !Boolean.TRUE.equals(part.getProperty("messagepart.isheader"))) {
            UnwrappedOperationInfo uop = (UnwrappedOperationInfo)part.getMessageInfo().getOperation();
            OperationInfo op = uop.getWrappedOperation();
            MessageInfo inf = null;
            if (uop.getInput() == part.getMessageInfo()) {
                inf = op.getInput();
            } else if (uop.getOutput() == part.getMessageInfo()) {
                inf = op.getOutput();
            }
            if (inf != null
                && inf.getFirstMessagePart().getTypeClass() != null) {
                //if the wrapper has a type class, we don't need to do anything
                //as everything would have been discovered when walking the
                //wrapper type (unless it's a header which wouldn't be in the wrapper)
                return;
            }
        }
        if (isFromWrapper
            && clazz.isArray()
            && !Byte.TYPE.equals(clazz.getComponentType())) {
            clazz = clazz.getComponentType();
        }

        Annotation[] a = (Annotation[])part.getProperty("parameter.annotations");
        checkForAdapter(clazz, a);

        Type genericType = (Type) part.getProperty("generic.type");
        if (genericType != null) {
            boolean isList = Collection.class.isAssignableFrom(clazz);
            if (isFromWrapper) {
                if (genericType instanceof Class
                    && ((Class<?>)genericType).isArray()) {
                    Class<?> cl2 = (Class<?>)genericType;
                    if (cl2.isArray()
                        && !Byte.TYPE.equals(cl2.getComponentType())) {
                        genericType = cl2.getComponentType();
                    }
                    addType(genericType);
                } else if (!isList) {
                    addType(genericType);
                }
            } else {
                addType(genericType, true);
            }

            if (isList
                && genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                if (pt.getActualTypeArguments().length > 0
                    && pt.getActualTypeArguments()[0] instanceof Class) {

                    Class<? extends Object> arrayCls =
                        Array.newInstance((Class<?>) pt.getActualTypeArguments()[0], 0).getClass();
                    clazz = arrayCls;
                    part.setTypeClass(clazz);
                    if (isFromWrapper) {
                        addType(clazz.getComponentType(), true);
                    }
                } else if (pt.getActualTypeArguments().length > 0
                    && pt.getActualTypeArguments()[0] instanceof GenericArrayType) {
                    GenericArrayType gat = (GenericArrayType)pt.getActualTypeArguments()[0];
                    gat.getGenericComponentType();
                    Class<? extends Object> arrayCls =
                        Array.newInstance((Class<?>) gat.getGenericComponentType(), 0).getClass();
                    clazz = Array.newInstance(arrayCls, 0).getClass();
                    part.setTypeClass(clazz);
                    if (isFromWrapper) {
                        addType(clazz.getComponentType(), true);
                    }
                }
            }
            if (isFromWrapper && isList) {
                clazz = null;
            }
        }
        if (clazz != null) {
            if (!isFromWrapper
                && clazz.getAnnotation(XmlRootElement.class) == null
                && clazz.getAnnotation(XmlType.class) != null
                && StringUtils.isEmpty(clazz.getAnnotation(XmlType.class).name())) {
                Object ref = createTypeReference(part.getName(), clazz);
                if (ref != null) {
                    typeReferences.add(ref);
                }
            }

            addClass(clazz);
        }
    }

    private void checkForAdapter(Class<?> clazz, Annotation[] anns) {
        if (anns != null) {
            for (Annotation a : anns) {
                if (XmlJavaTypeAdapter.class.isAssignableFrom(a.annotationType())) {
                    Type t = Utils.getTypeFromXmlAdapter((XmlJavaTypeAdapter)a);
                    if (t != null) {
                        addType(t);
                    }
                }
            }
        }
        XmlJavaTypeAdapter xjta = clazz.getAnnotation(XmlJavaTypeAdapter.class);
        if (xjta != null) {
            Type t = Utils.getTypeFromXmlAdapter(xjta);
            if (t != null) {
                addType(t);
            }
        }
        if (clazz.getPackage() != null) {
            XmlJavaTypeAdapters adapt = clazz.getPackage().getAnnotation(XmlJavaTypeAdapters.class);
            if (adapt != null) {
                for (XmlJavaTypeAdapter a : adapt.value()) {
                    globalAdapters.add(a.type());
                }
                for (XmlJavaTypeAdapter a : adapt.value()) {
                    Type t = Utils.getTypeFromXmlAdapter(a);
                    if (t != null) {
                        addType(t);
                    }
                }
            }
        }
    }

    private void addType(Type cls) {
        addType(cls, false);
    }
    private void addType(Type cls, boolean allowArray) {
        if (cls instanceof Class) {
            if (globalAdapters.contains(cls)) {
                return;
            }
            if (((Class<?>)cls).isArray() && !allowArray) {
                addClass(((Class<?>)cls).getComponentType());
            } else {
                addClass((Class<?>)cls);
            }
        } else if (cls instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)cls;
            addType(parameterizedType.getRawType());
            if (!parameterizedType.getRawType().equals(Enum.class)) {
                for (Type t2 : parameterizedType.getActualTypeArguments()) {
                    if (shouldTypeBeAdded(t2, parameterizedType)) {
                        addType(t2);
                    }
                }
            }
        } else if (cls instanceof GenericArrayType) {
            Class<?> ct;
            GenericArrayType gt = (GenericArrayType)cls;
            Type componentType = gt.getGenericComponentType();
            if (componentType instanceof Class) {
                ct = (Class<?>)componentType;
            } else if (componentType instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType)componentType;
                final Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class) {
                    ct = (Class<?>)rawType;
                } else {
                    throw new IllegalArgumentException("Unable to determine type for " + rawType);
                }
                if (!parameterizedType.getRawType().equals(Enum.class)) {
                    for (Type t2 : parameterizedType.getActualTypeArguments()) {
                        if (shouldTypeBeAdded(t2, parameterizedType)) {
                            addType(t2);
                        }
                    }
                }
            } else {
                TypeVariable<?> tv = (TypeVariable<?>)componentType;
                Type[] bounds = tv.getBounds();
                if (bounds != null && bounds.length == 1) {
                    if (bounds[0] instanceof Class) {
                        ct = (Class<?>)bounds[0];
                    } else {
                        throw new IllegalArgumentException("Unable to determine type for: " + tv);
                    }
                } else {
                    throw new IllegalArgumentException("Unable to determine type for: " + tv);
                }
            }
            ct = Array.newInstance(ct, 0).getClass();

            addClass(ct);
        } else if (cls instanceof WildcardType) {
            for (Type t : ((WildcardType)cls).getUpperBounds()) {
                addType(t);
            }
            for (Type t : ((WildcardType)cls).getLowerBounds()) {
                addType(t);
            }
        } else if (cls instanceof TypeVariable) {
            for (Type t : ((TypeVariable<?>)cls).getBounds()) {
                addType(t);
            }
        }
    }

    private boolean shouldTypeBeAdded(final Type t2, final ParameterizedType parameterizedType) {
        if (!(t2 instanceof TypeVariable)) {
            return true;
        }
        TypeVariable<?> typeVariable = (TypeVariable<?>) t2;
        final Type[] bounds = typeVariable.getBounds();
        for (Type bound : bounds) {
            if (bound instanceof ParameterizedType && bound.equals(parameterizedType)) {
                return false;
            }
        }
        return true;
    }

    void addClass(Class<?> claz) {
        if (Throwable.class.isAssignableFrom(claz)) {
            if (!Throwable.class.equals(claz)
                && !Exception.class.equals(claz)) {
                walkReferences(claz);
            }
            addClass(String.class);
        } else if (claz.getName().startsWith("java.")
            || claz.getName().startsWith("javax.") || claz.getName().startsWith("jakarta.")) { // Liberty Change: Check for jakarta
            return;
        } else {
            Class<?> cls = JAXBUtils.getValidClass(claz);
            if (cls == null
                && ReflectionUtil.getDeclaredConstructors(claz).length > 0
                && !Modifier.isAbstract(claz.getModifiers())) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Class " + claz.getName() + " does not have a default constructor which JAXB requires."); // Liberty Change: Finest Log Level
                }
                Object factory = createFactory(claz);
                unmarshallerProperties.put("com.sun.xml.bind.ObjectFactory", factory);
                cls = claz;
            }
            if (null != cls) {
                if (classes.contains(cls)) {
                    return;
                }

                if (!cls.isInterface()) {
                    classes.add(cls);
                }

                XmlSeeAlso xsa = cls.getAnnotation(XmlSeeAlso.class);
                if (xsa != null) {
                    for (Class<?> c : xsa.value()) {
                        addClass(c);
                    }
                }
                XmlJavaTypeAdapter xjta = cls.getAnnotation(XmlJavaTypeAdapter.class);
                if (xjta != null) {
                    //has an adapter.   We need to inspect the adapter and then
                    //return as the adapter will handle the superclass
                    //and interfaces and such
                    Type t = Utils.getTypeFromXmlAdapter(xjta);
                    if (t != null) {
                        addType(t);
                    }
                    return;
                }

                if (cls.getSuperclass() != null) {
                    //JAXB should do this, but it doesn't always.
                    //in particular, older versions of jaxb don't
                    addClass(cls.getSuperclass());
                }

                if (!cls.isInterface()) {
                    walkReferences(cls);
                }
            }
        }
    }

    private void walkReferences(Class<?> cls) {
        if (cls == null) {
            return;
        }
        if (cls.getName().startsWith("java.")
            || cls.getName().startsWith("javax.")) {
            return;
        }
        //walk the public fields/methods to try and find all the classes. JAXB will only load the
        //EXACT classes in the fields/methods if they are in a different package. Thus,
        //subclasses won't be found and the xsi:type stuff won't work at all.
        //We'll grab the public field/method types and then add the ObjectFactory stuff
        //as well as look for jaxb.index files in those packages.

        XmlAccessType accessType = Utils.getXmlAccessType(cls);

        if (accessType != XmlAccessType.PROPERTY) {   // only look for fields if we are instructed to
            //fields are accessible even if not public, must look at the declared fields
            //then walk to parents declared fields, etc...
            Field[] fields = ReflectionUtil.getDeclaredFields(cls);
            for (Field f : fields) {
                if (isFieldAccepted(f, accessType)) {
                    XmlJavaTypeAdapter xjta = Utils.getFieldXJTA(f);
                    if (xjta != null) {
                        Type t = Utils.getTypeFromXmlAdapter(xjta);
                        if (t != null) {
                            addType(t);
                            continue;
                        }
                    }
                    addType(f.getGenericType());
                }
            }
            walkReferences(cls.getSuperclass());
        }

        if (accessType != XmlAccessType.FIELD) {   // only look for methods if we are instructed to
            Method[] methods = ReflectionUtil.getDeclaredMethods(cls);
            for (Method m : methods) {
                if (isMethodAccepted(m, accessType)) {
                    XmlJavaTypeAdapter xjta = Utils.getMethodXJTA(m);
                    if (xjta != null) {
                        Type t = Utils.getTypeFromXmlAdapter(xjta);
                        if (t != null) {
                            addType(t);
                            continue;
                        }
                    }
                    addType(m.getGenericReturnType());
                    for (Type t : m.getGenericParameterTypes()) {
                        addType(t);
                    }
                }
            }
        }
    }

    /**
     * Checks if the field is accepted as a JAXB property.
     */
    static boolean isFieldAccepted(Field field, XmlAccessType accessType) {
        // We only accept non static fields which are not marked @XmlTransient or has transient modifier
        if (field.isAnnotationPresent(XmlTransient.class)
            || Modifier.isTransient(field.getModifiers())) {
            return false;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            return field.isAnnotationPresent(XmlAttribute.class);
        }
        if (accessType == XmlAccessType.PUBLIC_MEMBER
            && !Modifier.isPublic(field.getModifiers())) {
            return false;
        }
        if (accessType == XmlAccessType.NONE
            || accessType == XmlAccessType.PROPERTY) {
            return checkJaxbAnnotation(field.getAnnotations());
        }
        return true;
    }

    /**
     * Checks if the method is accepted as a JAXB property getter.
     */
    static boolean isMethodAccepted(Method method, XmlAccessType accessType) {
        // We only accept non static property getters which are not marked @XmlTransient
        if (Modifier.isStatic(method.getModifiers())
                || method.isAnnotationPresent(XmlTransient.class)
                || !Modifier.isPublic(method.getModifiers())
                || "getClass".equals(method.getName())) {
            return false;
        }

        // must not have parameters and return type must not be void
        if (method.getReturnType() == Void.class || method.getReturnType() == Void.TYPE
            || method.getParameterTypes().length != 0
            || (method.getDeclaringClass().equals(Throwable.class)
            && !("getMessage".equals(method.getName())))
            || !(method.getName().startsWith("get")
                    || method.getName().startsWith("is"))) {
            return false;
        }
        int beginIndex = 3;
        if (method.getName().startsWith("is")) {
            beginIndex = 2;
        }

        Method setter = null;
        try {
            setter = method.getDeclaringClass()
                .getMethod("set" + method.getName().substring(beginIndex),
                           new Class[] {method.getReturnType()});
        } catch (Exception e) {
            //getter, but no setter
        }
        if (setter != null) {
            if (setter.isAnnotationPresent(XmlTransient.class)
                || !Modifier.isPublic(setter.getModifiers())) {
                return false;
            }
        } else if (!Collection.class.isAssignableFrom(method.getReturnType())
            && !Throwable.class.isAssignableFrom(method.getDeclaringClass())) {
            //no setter, it's not a collection (thus getter().add(...)), and
            //not an Exception,
            return false;
        }

        if (accessType == XmlAccessType.NONE
            || accessType == XmlAccessType.FIELD) {
            return checkJaxbAnnotation(method.getAnnotations());
        }
        return true;
    }

    /**
     * Checks if there are JAXB annotations among the annotations of the class member.
     * @param annotations the array of annotations from the class member
     * @return true if JAXB annotations are present, false otherwise
     */
    static boolean checkJaxbAnnotation(Annotation[] annotations) {
        // must check if there are any jaxb annotations
        Package jaxbAnnotationsPackage = XmlElement.class.getPackage();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getPackage() == jaxbAnnotationsPackage) {
                return true;
            }
        }
        return false;
    }

    /**
     * The TypeReference class is a sun specific class that is found in two different
     * locations depending on environment. In IBM JDK the class is not available at all.
     * So we have to load it at runtime.
     *
     * @param n
     * @param cls
     * @return initiated TypeReference
     */
    private static Object createTypeReference(QName n, Class<?> cls) {
        Class<?> refClass = null;
        try {
            refClass = ClassLoaderUtils.loadClass("com.sun.xml.bind.api.TypeReference",
                                                  JAXBContextInitializer.class);
        } catch (Throwable ex) {
            try {
                refClass = ClassLoaderUtils.loadClass("com.sun.xml.internal.bind.api.TypeReference",
                                                      JAXBContextInitializer.class);
            } catch (Throwable ex2) {
                //ignore
            }
        }
        if (refClass != null) {
            try {
                return refClass.getConstructor(QName.class, Type.class, new Annotation[0].getClass()) //NOPMD
                    .newInstance(n, cls, new Annotation[0]);
            } catch (Throwable e) {
                //ignore
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private Object createFactory(Class<?> cls) {
        FactoryClassCreator creator = bus.getExtension(FactoryClassCreator.class);

        Class<?> factoryClass = creator.createFactory(cls);
        try {
            return factoryClass.newInstance();
        } catch (Exception e) {
           //ignore
        }
        return null;
    }


}
