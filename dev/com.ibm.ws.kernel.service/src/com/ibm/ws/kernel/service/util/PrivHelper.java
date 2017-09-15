/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Trivial
public class PrivHelper
{
    private PrivHelper() {}

    public static String getProperty(final String prop) {
        if (System.getSecurityManager() == null)
            return System.getProperty(prop);
        else
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(prop);
                }
            });
    }

    public static String getProperty(final String prop, final String defaultValue) {
        if (System.getSecurityManager() == null)
            return System.getProperty(prop, defaultValue);
        else
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(prop, defaultValue);
                }
            });
    }

    public static ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getClassLoader();
        else
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
    }

    private static PrivilegedAction<ClassLoader> getContextClassLoaderAction = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    };

    public static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null)
            return Thread.currentThread().getContextClassLoader();
        else
            return AccessController.doPrivileged(getContextClassLoaderAction);
    }

    private static PrivilegedAction<ClassLoader> getSystemClassLoaderAction = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return ClassLoader.getSystemClassLoader();
        }
    };

    public static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null)
            return ClassLoader.getSystemClassLoader();
        else
            return AccessController.doPrivileged(getSystemClassLoaderAction);
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static Class<?> loadClass(final ClassLoader cl, final String className) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return cl.loadClass(className);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return cl.loadClass(className);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }

    public static BundleContext getBundleContext(final Bundle bundle) {
        if (System.getSecurityManager() == null)
            return bundle.getBundleContext();
        else
            return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
                @Override
                public BundleContext run() {
                    return bundle.getBundleContext();
                }
            });
    }

    public static Class<?> loadClass(final Bundle b, final String name) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return b.loadClass(name);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return b.loadClass(name);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }

    public static <S> ServiceReference<S> getServiceReference(final Bundle bundle, final Class<S> clazz) {
        if (System.getSecurityManager() == null) {
            BundleContext bCtx = bundle.getBundleContext();
            return bCtx == null ? null : bCtx.getServiceReference(clazz);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<S>>() {
                @Override
                public ServiceReference<S> run() {
                    BundleContext bCtx = bundle.getBundleContext();
                    return bCtx == null ? null : bCtx.getServiceReference(clazz);
                }
            });
        }
    }

    public static BundleContext getBundleContext(final ComponentContext ctx) {
        if (System.getSecurityManager() == null)
            return ctx.getBundleContext();
        else
            return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
                @Override
                public BundleContext run() {
                    return ctx.getBundleContext();
                }
            });
    }

    public static ServiceReference<?> getServiceReference(final ComponentContext ctx) {
        if (System.getSecurityManager() == null)
            return ctx.getServiceReference();
        else
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<?>>() {
                @Override
                public ServiceReference<?> run() {
                    return ctx.getServiceReference();
                }
            });
    }

    public static ServiceReference<?>[] getServiceReferences(final ComponentContext ctx, final String clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            BundleContext bCtx = ctx.getBundleContext();
            return bCtx == null ? null : bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    public ServiceReference<?>[] run() throws InvalidSyntaxException {
                        BundleContext bCtx = ctx.getBundleContext();
                        return bCtx == null ? null : bCtx.getServiceReferences(clazz, filter);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    public static <S> S getService(final ComponentContext ctx, final ServiceReference<S> reference) {
        if (System.getSecurityManager() == null) {
            BundleContext bCtx = ctx.getBundleContext();
            return bCtx == null ? null : bCtx.getService(reference);
        } else
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    BundleContext bCtx = ctx.getBundleContext();
                    return bCtx == null ? null : bCtx.getService(reference);
                }
            });
    }

    public static <S> S getService(final ComponentContext cCtx, final Class<S> clazz) {
        if (System.getSecurityManager() == null) {
            BundleContext bCtx = cCtx.getBundleContext();
            ServiceReference<S> svcRef = bCtx == null ? null : bCtx.getServiceReference(clazz);
            return svcRef == null ? null : bCtx.getService(svcRef);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    BundleContext bCtx = cCtx.getBundleContext();
                    ServiceReference<S> svcRef = bCtx == null ? null : bCtx.getServiceReference(clazz);
                    return svcRef == null ? null : bCtx.getService(svcRef);
                }
            });
        }
    }

    public static Object locateService(final ComponentContext ctx, final String name) {
        if (System.getSecurityManager() == null)
            return ctx.locateService(name);
        else
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return ctx.locateService(name);
                }
            });
    }

    public static <S> S locateService(final ComponentContext ctx, final String name, final ServiceReference<S> reference) {
        if (System.getSecurityManager() == null)
            return ctx.locateService(name, reference);
        else
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    return ctx.locateService(name, reference);
                }
            });
    }

    public static Object[] locateServices(final ComponentContext ctx, final String name) {
        if (System.getSecurityManager() == null)
            return ctx.locateServices(name);
        else
            return AccessController.doPrivileged(new PrivilegedAction<Object[]>() {
                @Override
                public Object[] run() {
                    return ctx.locateServices(name);
                }
            });
    }

    public static <S> S getService(final BundleContext bCtx, final ServiceReference<S> ref) {
        if (System.getSecurityManager() == null)
            return bCtx.getService(ref);
        else
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    return bCtx.getService(ref);
                }
            });
    }

    public static <S> S getService(final BundleContext bCtx, final Class<S> clazz) {
        if (System.getSecurityManager() == null) {
            ServiceReference<S> svcRef = bCtx.getServiceReference(clazz);
            return svcRef == null ? null : bCtx.getService(svcRef);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    ServiceReference<S> svcRef = bCtx.getServiceReference(clazz);
                    return svcRef == null ? null : bCtx.getService(svcRef);
                }
            });
        }
    }

    public static ServiceReference<?> getServiceReference(final BundleContext bCtx, final String clazz) {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReference(clazz);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<?>>() {
                @Override
                public ServiceReference<?> run() {
                    return bCtx.getServiceReference(clazz);
                }
            });
        }
    }

    public static <S> ServiceReference<S> getServiceReference(final BundleContext bCtx, final Class<S> clazz) {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReference(clazz);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<S>>() {
                @Override
                public ServiceReference<S> run() {
                    return bCtx.getServiceReference(clazz);
                }
            });
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static <S> Collection<ServiceReference<S>> getServiceReferences(final BundleContext bCtx, final Class<S> clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Collection<ServiceReference<S>>>() {
                    @Override
                    public Collection<ServiceReference<S>> run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static ServiceReference<?>[] getServiceReferences(final BundleContext bCtx, final String clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    public ServiceReference<?>[] run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    public static <S> ServiceRegistration<S> registerService(final BundleContext bCtx,
                                                             final Class<S> clazz,
                                                             final ServiceFactory<S> factory,
                                                             final Dictionary<String, ?> properties) {
        if (System.getSecurityManager() == null)
            return bCtx.registerService(clazz, factory, properties);
        else
            return AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration<S>>() {
                @Override
                public ServiceRegistration<S> run() {
                    return bCtx.registerService(clazz, factory, properties);
                }
            });
    }

    public static <S> ServiceRegistration<S> registerService(final BundleContext bCtx,
                                                             final Class<S> clazz,
                                                             final S svc,
                                                             final Dictionary<String, ?> properties) {
        if (System.getSecurityManager() == null)
            return bCtx.registerService(clazz, svc, properties);
        else
            return AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration<S>>() {
                @Override
                public ServiceRegistration<S> run() {
                    return bCtx.registerService(clazz, svc, properties);
                }
            });
    }

    public static ServiceRegistration<?> registerService(final BundleContext bCtx,
                                                         final String clazz,
                                                         final Object svc,
                                                         final Dictionary<String, ?> properties) {
        if (System.getSecurityManager() == null)
            return bCtx.registerService(clazz, svc, properties);
        else
            return AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration<?>>() {
                @Override
                public ServiceRegistration<?> run() {
                    return bCtx.registerService(clazz, svc, properties);
                }
            });
    }

    public static ServiceRegistration<?> registerService(final BundleContext bCtx,
                                                         final String[] classes,
                                                         final Object svc,
                                                         final Dictionary<String, ?> properties) {
        if (System.getSecurityManager() == null)
            return bCtx.registerService(classes, svc, properties);
        else
            return AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration<?>>() {
                @Override
                public ServiceRegistration<?> run() {
                    return bCtx.registerService(classes, svc, properties);
                }
            });
    }
}
