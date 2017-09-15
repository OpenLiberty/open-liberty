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
package com.ibm.ws.jmx.fat.attach;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class VirtualMachineProxyHelper {

    private static final String IBM_VIRTUAL_MACHINE = "com.ibm.tools.attach.VirtualMachine";
    private static final String SUN_VIRTUAL_MACHINE = "com.sun.tools.attach.VirtualMachine";

    private static final String IBM_VIRTUAL_MACHINE_DESCRIPTOR = "com.ibm.tools.attach.VirtualMachineDescriptor";
    private static final String SUN_VIRTUAL_MACHINE_DESCRIPTOR = "com.sun.tools.attach.VirtualMachineDescriptor";

    private static final Class<?> VIRTUAL_MACHINE_CLASS;
    private static final Class<?> VIRTUAL_MACHINE_DESCRIPTOR_CLASS;
    static {
        Class<?> c1 = null;
        Class<?> c2 = null;
        try {
            // Attempt to load the IBM flavor of the Attach API.
            c1 = Class.forName(IBM_VIRTUAL_MACHINE);
            c2 = Class.forName(IBM_VIRTUAL_MACHINE_DESCRIPTOR);
        } catch (ClassNotFoundException e) {
            try {
                // Not an IBM JDK. Attempt to load the Oracle/Sun flavor of the Attach API.
                // This requires tools.jar. We cannot assume it's on the system classpath
                // so we add it to the search path of the ClassLoader.
                String javaHome = System.getProperty("java.home");
                // Assumes "java.home" points to JDK_BASE_DIR/jre or JDK_BASE_DIR
                File toolsJarLocation1 = new File(javaHome + File.separator + ".." + File.separator + "lib" + File.separator + "tools.jar");
                File toolsJarLocation2 = new File(javaHome + File.separator + "lib" + File.separator + "tools.jar");
                ClassLoader cl = URLClassLoader.newInstance(new URL[] { toolsJarLocation1.toURI().toURL(),
                                                                       toolsJarLocation2.toURI().toURL() });
                c1 = Class.forName(SUN_VIRTUAL_MACHINE, true, cl);
                c2 = Class.forName(SUN_VIRTUAL_MACHINE_DESCRIPTOR, true, cl);
            } catch (MalformedURLException e2) {
                throw new IllegalStateException(e2);
            } catch (ClassNotFoundException e2) {
                throw new IllegalStateException("An implementation of the Attach API was not found.", e2);
            }
        }
        VIRTUAL_MACHINE_CLASS = c1;
        VIRTUAL_MACHINE_DESCRIPTOR_CLASS = c2;
    }

    public static VirtualMachineProxy attach(String id) throws IOException {
        try {
            Method attachMethod = VIRTUAL_MACHINE_CLASS.getMethod("attach", String.class);
            attachMethod.setAccessible(true);
            Object vm = attachMethod.invoke(null, id);
            if (vm != null) {
                return (VirtualMachineProxy) Proxy.newProxyInstance(VirtualMachineProxy.class.getClassLoader(),
                                                                    new Class<?>[] { VirtualMachineProxy.class },
                                                                    new ForwardingInvocationHandler(vm));
            }
            return null;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static VirtualMachineProxy attach(VirtualMachineDescriptorProxy vmd) throws IOException {
        Object _vmd = null;
        if (vmd instanceof ObjectProxy) {
            _vmd = ((ObjectProxy) vmd).getBackingObject();
        } else if (vmd != null) {
            throw new IllegalArgumentException("Unsupported proxy instance.");
        }
        try {
            Method attachMethod = VIRTUAL_MACHINE_CLASS.getMethod("attach", VIRTUAL_MACHINE_DESCRIPTOR_CLASS);
            attachMethod.setAccessible(true);
            Object vm = attachMethod.invoke(null, _vmd);
            if (vm != null) {
                return (VirtualMachineProxy) Proxy.newProxyInstance(VirtualMachineProxy.class.getClassLoader(),
                                                                    new Class<?>[] { VirtualMachineProxy.class },
                                                                    new ForwardingInvocationHandler(vm));
            }
            return null;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<VirtualMachineDescriptorProxy> list() {
        try {
            Method attachMethod = VIRTUAL_MACHINE_CLASS.getMethod("list");
            attachMethod.setAccessible(true);
            @SuppressWarnings("rawtypes")
            List<?> list = (List) attachMethod.invoke(null);
            if (list == null) {
                return null;
            }
            final int size = list.size();
            List<VirtualMachineDescriptorProxy> _list = new ArrayList<VirtualMachineDescriptorProxy>(size);
            for (int i = 0; i < size; ++i) {
                Object vmd = list.get(i);
                if (vmd != null) {
                    _list.add((VirtualMachineDescriptorProxy) Proxy.newProxyInstance(VirtualMachineDescriptorProxy.class.getClassLoader(),
                                                                                     new Class<?>[] { VirtualMachineDescriptorProxy.class, ObjectProxy.class },
                                                                                     new ForwardingInvocationHandler(vmd)));
                } else {
                    _list.add(null);
                }
            }
            return _list;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private VirtualMachineProxyHelper() {}

}
