/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.Info;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Mockery for AnnotationService_Service, ClassInfo, MethodInfo, and AnnotationInfo.
 */
public class AnnoMockery {
    private final Mockery mockery;
    private final Map<String, Set<String>> selectableClasses = new HashMap<String, Set<String>>();
    private final Map<String, ClassInfo> classInfos = new HashMap<String, ClassInfo>();
    private final AnnotationTargets_Targets targets;
    private final InfoStore infoStore;
    private int numMethodInfos;
    private int numAnnotationInfos;
    private int numAnnotationValues;

    AnnoMockery(Mockery mockery) {
        this.mockery = mockery;
        targets = mockery.mock(AnnotationTargets_Targets.class);
        infoStore = mockery.mock(InfoStore.class);
    }

    private AnnotationValue mockAnnotationValue() {
        return mockery.mock(AnnotationValue.class, "annotationValue-" + ++numAnnotationValues);
    }

    private AnnotationInfo mockAnnotationInfo(final Annotation ann) throws Exception {
        final Class<?> annType = ann.annotationType();
        final AnnotationInfo annInfo = mockery.mock(AnnotationInfo.class, "annotationInfo-" + annType.getName() + "-" + ++numAnnotationInfos);
        mockery.checking(new Expectations() {
            {
                for (Method m : annType.getMethods()) {
                    Class<?> declaringClass = m.getDeclaringClass();
                    if (declaringClass != Object.class && declaringClass != Annotation.class) {
                        AnnotationValue annValue = mockAnnotationValue();
                        String elementName = m.getName();
                        allowing(annInfo).getValue(elementName);
                        will(returnValue(annValue));

                        Object value = m.invoke(ann);
                        if (value instanceof String) {
                            allowing(annValue).getStringValue();
                            will(returnValue(value));
                        } else if (value instanceof Boolean) {
                            allowing(annInfo).getBoolean(elementName);
                            will(returnValue(value));
                        } else if (value instanceof Class) {
                            allowing(annInfo).getClassNameValue(elementName);
                            will(returnValue(((Class<?>) value).getName()));
                        } else if (value instanceof Enum) {
                            allowing(annInfo).getEnumValue(elementName);
                            will(returnValue(((Enum<?>) value).name()));
                        } else if (value instanceof Object[]) {
                            Object[] valueArray = (Object[]) value;
                            AnnotationValue[] valueAnnValues = new AnnotationValue[valueArray.length];
                            for (int i = 0; i < valueArray.length; i++) {
                                AnnotationValue valueAnnValue = mockAnnotationValue();
                                valueAnnValues[i] = valueAnnValue;

                                Object valueArrayValue = valueArray[i];
                                if (valueArrayValue instanceof Annotation) {
                                    allowing(valueAnnValue).getAnnotationValue();
                                    will(returnValue(mockAnnotationInfo((Annotation) valueArrayValue)));
                                } else if (valueArrayValue instanceof String) {
                                    allowing(valueAnnValue).getStringValue();
                                    will(returnValue(valueArrayValue));
                                } else if (valueArrayValue instanceof Class) {
                                    allowing(valueAnnValue).getClassNameValue();
                                    will(returnValue(((Class<?>) valueArrayValue).getName()));
                                }
                            }

                            allowing(annInfo).getArrayValue(elementName);
                            will(returnValue(Arrays.asList(valueAnnValues)));
                        }
                    }
                }
            }
        });

        return annInfo;
    }

    @SuppressWarnings("unchecked")
    private void mockInfoAnnotations(final Info info, final AnnotatedElement element) throws Exception {
        mockery.checking(new Expectations() {
            {
                for (final Annotation ann : element.getAnnotations()) {
                    final AnnotationInfo annInfo = mockAnnotationInfo(ann);
                    Class<? extends Annotation> annType = ann.annotationType();
                    allowing(info).getAnnotation(annType);
                    will(returnValue(annInfo));
                    allowing(info).isAnnotationPresent(annType.getName());
                    will(returnValue(true));
                }
                allowing(info).getAnnotation(with(any(Class.class)));
                will(returnValue(null));
                allowing(info).isAnnotationPresent(with(any(String.class)));
                will(returnValue(false));
            }
        });
    }

    private MethodInfo mockMethodInfo(final Method method) throws Exception {
        final MethodInfo methodInfo = mockery.mock(MethodInfo.class, "methodInfo-" + method + "-" + ++numMethodInfos);
        mockInfoAnnotations(methodInfo, method);
        return methodInfo;
    }

    private ClassInfo mockClassInfo(final Class<?> klass) throws Exception {
        final String name = klass.getName();

        ClassInfo origClassInfo = classInfos.get(name);
        if (origClassInfo != null) {
            return origClassInfo;
        }

        final ClassInfo classInfo = mockery.mock(ClassInfo.class, "classInfo-" + name);
        classInfos.put(name, classInfo);

        mockery.checking(new Expectations() {
            {
                allowing(classInfo).isArtificial();
                will(returnValue(false));

                allowing(classInfo).getName();
                will(returnValue(name));

                allowing(classInfo).getSuperclass();
                will(returnValue(null));

                List<ClassInfo> interfaces = new ArrayList<ClassInfo>();
                List<String> interfaceNames = new ArrayList<String>();
                for (Class<?> intf : klass.getInterfaces()) {
                    interfaces.add(mockClassInfo(intf));
                    interfaceNames.add(intf.getName());
                }
                allowing(classInfo).getInterfaces();
                will(returnValue(interfaces));
                allowing(classInfo).getInterfaceNames();
                will(returnValue(interfaceNames));

                Set<Class<?>> allInterfaces = new LinkedHashSet<Class<?>>();
                if (klass.isInterface()) {
                    collectInterfaceExtends(klass, allInterfaces);
                } else {
                    collectImplements(klass, allInterfaces);
                }
                for (Class<?> intf : allInterfaces) {
                    allowing(classInfo).isInstanceOf(intf);
                    will(returnValue(true));
                }
                allowing(classInfo).isInstanceOf(with(any(Class.class)));
                will(returnValue(false));

                Collection<MethodInfo> methodInfos = new ArrayList<MethodInfo>();
                for (Method method : klass.getMethods()) {
                    methodInfos.add(mockMethodInfo(method));
                }
                allowing(classInfo).getMethods();
                will(returnValue(methodInfos));

                mockInfoAnnotations(classInfo, klass);
            }
        });

        return classInfo;
    }

    private void collectImplements(Class<?> klass, Set<Class<?>> interfaces) {
        for (Class<?> classIter = klass; classIter != Object.class; classIter = classIter.getSuperclass()) {
            collectInterfaceExtends(classIter, interfaces);
        }
    }

    private void collectInterfaceExtends(Class<?> intf, Set<Class<?>> interfaces) {
        for (Class<?> intfIter : intf.getInterfaces()) {
            if (interfaces.add(intfIter)) {
                collectInterfaceExtends(intfIter, interfaces);
            }
        }
    }

    void addClass(final Class<?> klass) throws Exception {
        final ClassInfo classInfo = mockClassInfo(klass);

        mockery.checking(new Expectations() {
            {
                allowing(infoStore).getDelayableClassInfo(klass.getName());
                will(returnValue(classInfo));

                for (Annotation ann : klass.getAnnotations()) {
                    String annTypeName = ann.annotationType().getName();
                    Set<String> classes = selectableClasses.get(annTypeName);
                    if (classes == null) {
                        classes = new LinkedHashSet<String>();
                        selectableClasses.put(annTypeName, classes);

                        // 95160: Update to the new API, which uses 'get' and has a second scan policies parameter.
                        allowing(targets).getAnnotatedClasses(with(annTypeName), with(any(int.class)));
                        will(returnValue(classes));
                    }

                    classes.add(klass.getName());
                }
            }
        });
    }

    AnnotationService_Service mockAnnotationService() throws AnnotationTargets_Exception, InfoStoreException {
        final AnnotationService_Service annoService = mockery.mock(AnnotationService_Service.class);
        final AnnotationTargets_Factory targetsFactory = mockery.mock(AnnotationTargets_Factory.class);
        final InfoStoreFactory infoStoreFactory = mockery.mock(InfoStoreFactory.class);
        final ClassInfo invalidClass = mockery.mock(ClassInfo.class);

        mockery.checking(new Expectations() {
            {
                allowing(annoService).getAnnotationTargetsFactory();
                will(returnValue(targetsFactory));

                allowing(targetsFactory).createTargets();
                will(returnValue(targets));

                allowing(annoService).getInfoStoreFactory();
                will(returnValue(infoStoreFactory));

                allowing(infoStoreFactory).createInfoStore(with(any(ClassSource_Aggregate.class)));
                will(returnValue(infoStore));

                allowing(infoStore).getDelayableClassInfo(with(any(String.class)));
                will(returnValue(invalidClass));

                allowing(invalidClass).isArtificial();
                will(returnValue(true));

                allowing(invalidClass).isInstanceOf(with(any(Class.class)));
                will(returnValue(false));

                allowing(targets).scan(with(any(ClassSource_Aggregate.class)));

                // 95160: Update to the new API, which uses 'get' and has a second scan policies parameter.                
                allowing(targets).getAnnotatedClasses(with(any(String.class)), with(any(int.class)));
                will(returnValue(Collections.emptySet()));

                allowing(annoService);
            }
        });

        return annoService;
    }

    ModuleAnnotations mockModuleAnnotations() throws AnnotationTargets_Exception, UnableToAdaptException {
        final ModuleAnnotations moduleAnno = mockery.mock(ModuleAnnotations.class);

        mockery.checking(new Expectations() {
            {

                allowing(moduleAnno).getAnnotationTargets();
                will(returnValue(targets));

                allowing(moduleAnno).getInfoStore();
                will(returnValue(infoStore));

            }
        });

        return moduleAnno;
    }
}
