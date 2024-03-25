/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.resources;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.metadata.TypeStore;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * {@link ReflectionCache} implementation that works around possible deadlocks in HotSpot:
 *
 * @see https://issues.jboss.org/browse/WELD-1169
 * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7122142
 * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6588239
 *
 * @author Jozef Hartinger
 *
 */
public class HotspotReflectionCache extends DefaultReflectionCache {

//This class was extracted from weld source code.
//IBM modifications: Adding internalGetAnnotationsLogged, and modifying internalGetAnnotations to call it when a system property is set. internalGetAnnotationsUnlogged is the original weld varient of internalGetAnnotations
//original file: https://github.com/weld/core/blob/4.0/impl/src/main/java/org/jboss/weld/resources/HotspotReflectionCache.java
//Hopefully we can undo this once we have fixed the bug this tracks

    private final Class<?> annotationTypeLock;

    private static final String JBOSS_HOTSPOT_TRACE_ENABLED_VALUE = getSystemProperty("jboss.hotspot.trace.enabled");

    private static final boolean JBOSS_HOTSPOT_TRACE_ENABLED = ((JBOSS_HOTSPOT_TRACE_ENABLED_VALUE != null) &&
                                                                JBOSS_HOTSPOT_TRACE_ENABLED_VALUE.equals("true"));

    private static volatile boolean sentWarning;

    private static void sendWarning() {
        if (!sentWarning) {
            sentWarning = true;
            System.out.println("Warning you have enabled logging that should only be seen in an IBM test environment");
        }
    }

    public HotspotReflectionCache(TypeStore store) {
        super(store);
        try {
            this.annotationTypeLock = Class.forName("sun.reflect.annotation.AnnotationType");
        } catch (ClassNotFoundException e) {
            throw new WeldException(e);
        }
    }

    protected static final void dump(Class<?> targetClass) {
        String className = "HotspotReflectionCache";
        String methodName = "dump";

        String resourceName = FileLogger.getClassResourceName(targetClass);

        String text = "Class [ " + targetClass.getName() + " ] as [ " + resourceName + " ]";
        FileLogger.fileLog(className, methodName, text);

        byte[] classBytes;
        try {
            classBytes = FileLogger.read(targetClass.getClassLoader(), resourceName);
        } catch (IOException e) {
            FileLogger.fileStack(className, methodName, "Failed to read [ " + resourceName + " ]", e);
            return;
        }

        FileLogger.fileDump(className, methodName, text, classBytes);
    }

    @Override
    @FFDCIgnore(Exception.class)
    protected Annotation[] internalGetAnnotations(AnnotatedElement element) {
        if (JBOSS_HOTSPOT_TRACE_ENABLED) {
            sendWarning();
            return internalGetAnnotationsLogged(element);
        }
        return internalGetAnnotationsUnlogged(element);
    }

    protected Annotation[] internalGetAnnotationsUnlogged(AnnotatedElement element) {
        if (element instanceof Class<?>) {
            Class<?> clazz = (Class<?>) element;
            if (clazz.isAnnotation()) {
                synchronized (annotationTypeLock) {
                    return element.getAnnotations();
                }
            }
        }
        return element.getAnnotations();
    }

    protected Annotation[] internalGetAnnotationsLogged(AnnotatedElement element) {
        String className = "HotspotReflectionCache";
        String methodName = "internalGetAnnotations";

        try {
            if (element instanceof Class<?>) {
                Class<?> clazz = (Class<?>) element;
                if (clazz.isAnnotation()) {
                    Annotation[] classAnno;
                    synchronized (annotationTypeLock) {
                        classAnno = element.getAnnotations();
                    }
                    Integer numClassAnno = ((classAnno == null) ? null : Integer.valueOf(classAnno.length));
                    FileLogger.fileLog(className, methodName, "Class [ " + clazz.getName() + " ] [ " + numClassAnno + " ]");
                    return classAnno;
                }
            }

            Annotation[] anno = element.getAnnotations();
            Integer numAnno = ((anno == null) ? null : Integer.valueOf(anno.length));
            FileLogger.fileLog(className, methodName, "Element [ " + element + " ] [ " + numAnno + " ]");
            return anno;

        } catch (Throwable e) {
            String text = "Constant Pool GREP:";
                        // AnnotatedElement
            //   AccessibleObject
            //     Executable
            //       Constructor
            //       Method
            //     Field
            //   Package
            //   Parameter

            Class<?> clazz = null;
            if (element instanceof Parameter) {
                Executable executable = ((Parameter) element).getDeclaringExecutable();
                if ( executable != null ) {
                    clazz = executable.getDeclaringClass();
                }
            } else if (element instanceof Field) {
                clazz = ((Field) element).getDeclaringClass();
            } else if (element instanceof Method) {
                clazz = ((Method) element).getDeclaringClass();
            } else if (element instanceof Constructor) {
                clazz = ((Constructor<?>) element).getDeclaringClass();
            } else if (element instanceof Class<?>) {
                clazz = (Class<?>) element;
            }
            if (clazz != null) {
                text += " Class [ " + clazz.getName() + " ]:";
            }

            FileLogger.fileLog(className, methodName, text, element);
            if (clazz != null) {
                dump(clazz);
            }
            FileLogger.fileStack(className, methodName, text, e);

            throw e;
        }
    }

    static private String getSystemProperty(final String name) {
        try {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(name);
                }
            });
        } catch (Exception e) {
            return null;
        }
    }
}
