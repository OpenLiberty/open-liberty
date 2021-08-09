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
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.cxf.common.classloader.JAXBClassLoaderUtils;
import org.apache.cxf.common.jaxb.JAXBUtils;
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

    private Set<Class<?>> classes;
    private Collection<Object> typeReferences;

    public JAXBContextInitializer(ServiceInfo serviceInfo,
                                  Set<Class<?>> classes,
                                  Collection<Object> typeReferences) {
        super(serviceInfo);
        this.classes = classes;
        this.typeReferences = typeReferences;
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
                && inf.getMessagePart(0).getTypeClass() != null) {
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
                Object ref = JAXBClassLoaderUtils.createTypeReference(part.getName(), clazz);
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
                    inspectTypeAdapter(((XmlJavaTypeAdapter)a).value());
                }
            }
        }
        XmlJavaTypeAdapter xjta = clazz.getAnnotation(XmlJavaTypeAdapter.class);
        if (xjta != null) {
            inspectTypeAdapter(xjta.value());
        }
        
    }

    private void addType(Type cls) {
        addType(cls, false);
    }
    private void addType(Type cls, boolean allowArray) {
        if (cls instanceof Class) {
            if (((Class<?>)cls).isArray() && !allowArray) {
                addClass(((Class<?>)cls).getComponentType());
            } else {
                addClass((Class<?>)cls);
            }
        } else if (cls instanceof ParameterizedType) {
            addType(((ParameterizedType)cls).getRawType());
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                addType(t2);
            }
        } else if (cls instanceof GenericArrayType) {
            Class<?> ct;
            GenericArrayType gt = (GenericArrayType)cls;
            Type componentType = gt.getGenericComponentType();
            if (componentType instanceof Class) {
                ct = (Class<?>)componentType;
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


    private  void addClass(Class<?> cls) {
        if (Throwable.class.isAssignableFrom(cls)) {
            if (!Throwable.class.equals(cls)
                && !Exception.class.equals(cls)) {
                walkReferences(cls);
            }
            addClass(String.class);
        } else {
            cls = JAXBUtils.getValidClass(cls);
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
                    @SuppressWarnings("rawtypes")
                    Class<? extends XmlAdapter> c2 = xjta.value();
                    inspectTypeAdapter(c2);
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

    private void inspectTypeAdapter(@SuppressWarnings("rawtypes") Class<? extends XmlAdapter> aclass) {
        Class<?> c2 = aclass;
        Type sp = c2.getGenericSuperclass();
        while (!XmlAdapter.class.equals(c2) && c2 != null) {
            sp = c2.getGenericSuperclass();
            c2 = c2.getSuperclass();
        }
        if (sp instanceof ParameterizedType) {
            addType(((ParameterizedType)sp).getActualTypeArguments()[0]);
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

        XmlAccessorType accessorType = cls.getAnnotation(XmlAccessorType.class);
        if (accessorType == null && cls.getPackage() != null) {
            accessorType = cls.getPackage().getAnnotation(XmlAccessorType.class);
        }
        XmlAccessType accessType = accessorType != null ? accessorType.value() : XmlAccessType.PUBLIC_MEMBER;

        if (accessType != XmlAccessType.PROPERTY) {   // only look for fields if we are instructed to
            //fields are accessible even if not public, must look at the declared fields
            //then walk to parents declared fields, etc...
            Field fields[] = ReflectionUtil.getDeclaredFields(cls); 
            for (Field f : fields) {
                if (isFieldAccepted(f, accessType)) {
                    addType(f.getGenericType());
                }
            }
            walkReferences(cls.getSuperclass());
        }

        if (accessType != XmlAccessType.FIELD) {   // only look for methods if we are instructed to
            Method methods[] = ReflectionUtil.getDeclaredMethods(cls); 
            for (Method m : methods) {
                if (isMethodAccepted(m, accessType)) {
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
        // We only accept non static fields which are not marked @XmlTransient
        if (Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(XmlTransient.class)
            || Modifier.isTransient(field.getModifiers())) {
            return false;
        }
        if (accessType == XmlAccessType.PUBLIC_MEMBER 
            && !Modifier.isPublic(field.getModifiers())) {
            return false;
        }
        if (field.getAnnotation(XmlJavaTypeAdapter.class) != null) {
            return false;
        }
        if (accessType == XmlAccessType.NONE
            || accessType == XmlAccessType.PROPERTY) {
            return checkJaxbAnnotation(field.getAnnotations());
        } else {
            return true;
        }
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
        if (method.getReturnType() == Void.class 
            || method.getParameterTypes().length != 0
            || (method.getDeclaringClass().equals(Throwable.class)
            && !("getMessage".equals(method.getName())))) {
            return false;
        }

        boolean isPropGetter = method.getName().startsWith("get") || method.getName().startsWith("is");

        if (!isPropGetter 
            || method.getAnnotation(XmlJavaTypeAdapter.class) != null) {
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
        if ((setter != null) 
                && ((setter.isAnnotationPresent(XmlTransient.class)
                || !Modifier.isPublic(setter.getModifiers())))) {
            return false;
             
        }
        if (accessType == XmlAccessType.NONE
            || accessType == XmlAccessType.FIELD) {
            return checkJaxbAnnotation(method.getAnnotations());
        } else {
            return true;
        }
    }

    /**
     * Checks if there are JAXB annotations among the annotations of the class member.
     * 
     * @param annotations the array of annotations from the class member
     * @return true if JAXB annotations are present, false otherwise
     */
    private static boolean checkJaxbAnnotation(Annotation[] annotations) {
        // must check if there are any jaxb annotations
        Package jaxbAnnotationsPackage = XmlElement.class.getPackage();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getPackage() == jaxbAnnotationsPackage) {
                return true;
            }
        }
        return false;
    }
}