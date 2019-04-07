/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Properties;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Utility class to execute common privileged code.
 *
 * @see {@link SecureAction#get()} for how to obtain an instance.
 */
@Trivial
public class SecureAction {
    // make sure we use the correct controlContext;
    private final AccessControlContext controlContext;

    // This ClassLoader is used in loadSystemClass if System.getClassLoader() returns null
    static final ClassLoader bootClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return new ClassLoader(Object.class.getClassLoader()) { /* boot class loader */};
        }
    });

    private SecureAction() {
        // save the control context to be used.
        this.controlContext = AccessController.getContext();
    }

    /**
     * Creates a privileged action that can be used to construct a SecureAction object.
     * The recommended way to construct and cache SecureAction object is the following: <p>
     *
     * <pre>
     * static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());
     * </pre>
     *
     * NOTE: For optimal performance, the SecureAction should be cached in a package private field.
     *
     * @return a privileged action object that can be used to construct a SecureAction object.
     */
    public static PrivilegedAction<SecureAction> get() {
        return new PrivilegedAction<SecureAction>() {
            @Override
            public SecureAction run() {
                return new SecureAction();
            }
        };
    }

    /**
     * Returns a system property. Same as calling
     * System.getProperty(String).
     *
     * @param property the property key.
     * @return the value of the property or null if it does not exist.
     */
    public String getProperty(final String property) {
        if (System.getSecurityManager() == null)
            return System.getProperty(property);
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(property);
            }
        }, controlContext);
    }

    public String getProperty(final String prop, final String defaultValue) {
        if (System.getSecurityManager() == null)
            return System.getProperty(prop, defaultValue);
        else
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(prop, defaultValue);
                }
            }, controlContext);
    }

    /**
     * Returns a system properties. Same as calling
     * System.getProperties().
     *
     * @return the system properties.
     */
    public Properties getProperties() {
        if (System.getSecurityManager() == null)
            return System.getProperties();
        return AccessController.doPrivileged(new PrivilegedAction<Properties>() {
            @Override
            public Properties run() {
                return System.getProperties();
            }
        }, controlContext);
    }

    /**
     * Creates a FileInputStream from a File. Same as calling
     * new FileInputStream(File).
     *
     * @param file the File to craete a FileInputStream from.
     * @return The FileInputStream.
     * @throws FileNotFoundException if the File does not exist.
     */
    public FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
        if (System.getSecurityManager() == null)
            return new FileInputStream(file);
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                @Override
                public FileInputStream run() throws FileNotFoundException {
                    return new FileInputStream(file);
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof FileNotFoundException)
                throw (FileNotFoundException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Creates a FileInputStream from a File. Same as calling
     * new FileOutputStream(File,boolean).
     *
     * @param file the File to create a FileOutputStream from.
     * @param append indicates if the OutputStream should append content.
     * @return The FileOutputStream.
     * @throws FileNotFoundException if the File does not exist.
     */
    public FileOutputStream getFileOutputStream(final File file, final boolean append) throws FileNotFoundException {
        if (System.getSecurityManager() == null)
            return new FileOutputStream(file.getAbsolutePath(), append);
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
                @Override
                public FileOutputStream run() throws FileNotFoundException {
                    return new FileOutputStream(file.getAbsolutePath(), append);
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof FileNotFoundException)
                throw (FileNotFoundException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Returns the length of a file. Same as calling
     * file.length().
     *
     * @param file a file object
     * @return the length of a file.
     */
    public long length(final File file) {
        if (System.getSecurityManager() == null)
            return file.length();
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return new Long(file.length());
            }
        }, controlContext).longValue();
    }

    /**
     * Returns the canonical path of a file. Same as calling
     * file.getCanonicalPath().
     *
     * @param file a file object
     * @return the canonical path of a file.
     * @throws IOException on error
     */
    public String getCanonicalPath(final File file) throws IOException {
        if (System.getSecurityManager() == null)
            return file.getCanonicalPath();
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws IOException {
                    return file.getCanonicalPath();
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof IOException)
                throw (IOException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Returns the absolute file. Same as calling
     * file.getAbsoluteFile().
     *
     * @param file a file object
     * @return the absolute file.
     */
    public File getAbsoluteFile(final File file) {
        if (System.getSecurityManager() == null)
            return file.getAbsoluteFile();
        return AccessController.doPrivileged(new PrivilegedAction<File>() {
            @Override
            public File run() {
                return file.getAbsoluteFile();
            }
        }, controlContext);
    }

    /**
     * Returns the canonical file. Same as calling
     * file.getCanonicalFile().
     *
     * @param file a file object
     * @return the canonical file.
     */
    public File getCanonicalFile(final File file) throws IOException {
        if (System.getSecurityManager() == null)
            return file.getCanonicalFile();
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {
                @Override
                public File run() throws IOException {
                    return file.getCanonicalFile();
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof IOException)
                throw (IOException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Returns true if a file exists, otherwise false is returned. Same as calling
     * file.exists().
     *
     * @param file a file object
     * @return true if a file exists, otherwise false
     */
    public boolean exists(final File file) {
        if (System.getSecurityManager() == null)
            return file.exists();
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists() ? Boolean.TRUE : Boolean.FALSE;
            }
        }, controlContext).booleanValue();
    }

    public boolean mkdirs(final File file) {
        if (System.getSecurityManager() == null)
            return file.mkdirs();
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.mkdirs() ? Boolean.TRUE : Boolean.FALSE;
            }
        }, controlContext).booleanValue();
    }

    /**
     * Returns true if a file is a directory, otherwise false is returned. Same as calling
     * file.isDirectory().
     *
     * @param file a file object
     * @return true if a file is a directory, otherwise false
     */
    public boolean isDirectory(final File file) {
        if (System.getSecurityManager() == null)
            return file.isDirectory();
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.isDirectory() ? Boolean.TRUE : Boolean.FALSE;
            }
        }, controlContext).booleanValue();
    }

    /**
     * Returns a file's last modified stamp. Same as calling
     * file.lastModified().
     *
     * @param file a file object
     * @return a file's last modified stamp.
     */
    public long lastModified(final File file) {
        if (System.getSecurityManager() == null)
            return file.lastModified();
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return new Long(file.lastModified());
            }
        }, controlContext).longValue();
    }

    /**
     * Returns a file's list. Same as calling
     * file.list().
     *
     * @param file a file object
     * @return a file's list.
     */
    public String[] list(final File file) {
        if (System.getSecurityManager() == null)
            return file.list();
        return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                return file.list();
            }
        }, controlContext);
    }

    /**
     * Returns a ZipFile. Same as calling
     * new ZipFile(file)
     *
     * @param file the file to get a ZipFile for
     * @return a ZipFile
     * @throws IOException if an error occured
     */
    public ZipFile getZipFile(final File file) throws IOException {
        try {
            if (System.getSecurityManager() == null)
                return new ZipFile(file);
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {
                    @Override
                    public ZipFile run() throws IOException {
                        return new ZipFile(file);
                    }
                }, controlContext);
            } catch (PrivilegedActionException e) {
                if (e.getException() instanceof IOException)
                    throw (IOException) e.getException();
                throw (RuntimeException) e.getException();
            }
        } catch (ZipException e) {
            ZipException zipNameException = new ZipException("Exception in opening zip file: " + file.getPath()); //$NON-NLS-1$
            zipNameException.initCause(e);
            throw zipNameException;
        } catch (IOException e) {
            throw new IOException("Exception in opening zip file: " + file.getPath(), e); //$NON-NLS-1$
        }
    }

    /**
     * Gets a URL. Same a calling
     * {@link URL#URL(java.lang.String, java.lang.String, int, java.lang.String, java.net.URLStreamHandler)}
     *
     * @param protocol the protocol
     * @param host the host
     * @param port the port
     * @param file the file
     * @param handler the URLStreamHandler
     * @return a URL
     * @throws MalformedURLException
     */
    public URL getURL(final String protocol, final String host, final int port, final String file, final URLStreamHandler handler) throws MalformedURLException {
        if (System.getSecurityManager() == null)
            return new URL(protocol, host, port, file, handler);
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException {
                    return new URL(protocol, host, port, file, handler);
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof MalformedURLException)
                throw (MalformedURLException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Creates a new Thread from a Runnable. Same as calling
     * new Thread(target,name).setContextClassLoader(contextLoader).
     *
     * @param target the Runnable to create the Thread from.
     * @param name The name of the Thread.
     * @param contextLoader the context class loader for the thread
     * @return The new Thread
     */
    public Thread createThread(final Runnable target, final String name, final ClassLoader contextLoader) {
        if (System.getSecurityManager() == null)
            return createThread0(target, name, contextLoader);
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                return createThread0(target, name, contextLoader);
            }
        }, controlContext);
    }

    Thread createThread0(Runnable target, String name, ClassLoader contextLoader) {
        Thread result = new Thread(target, name);
        if (contextLoader != null)
            result.setContextClassLoader(contextLoader);
        return result;
    }

    /**
     * Gets a service object. Same as calling
     * context.getService(reference)
     *
     * @param reference the ServiceReference
     * @param context the BundleContext
     * @return a service object
     */
    public <S> S getService(final ServiceReference<S> reference, final BundleContext context) {
        if (System.getSecurityManager() == null)
            return context.getService(reference);
        return AccessController.doPrivileged(new PrivilegedAction<S>() {
            @Override
            public S run() {
                return context.getService(reference);
            }
        }, controlContext);
    }

    /**
     * Returns a Class. Same as calling
     * Class.forName(name)
     *
     * @param name the name of the class.
     * @return a Class
     * @throws ClassNotFoundException
     */
    public Class<?> forName(final String name) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return Class.forName(name);
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws Exception {
                    return Class.forName(name);
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof ClassNotFoundException)
                throw (ClassNotFoundException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Returns a Class.
     * Tries to load a class from the System ClassLoader or if that doesn't exist tries the boot ClassLoader
     *
     * @param name the name of the class.
     * @return a Class
     * @throws ClassNotFoundException
     */
    public Class<?> loadSystemClass(final String name) throws ClassNotFoundException {
        if (System.getSecurityManager() == null) {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            return (systemClassLoader != null) ? systemClassLoader.loadClass(name) : bootClassLoader.loadClass(name);
        }
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws Exception {
                    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                    return (systemClassLoader != null) ? systemClassLoader.loadClass(name) : bootClassLoader.loadClass(name);
                }
            }, controlContext);
        } catch (PrivilegedActionException e) {
            if (e.getException() instanceof ClassNotFoundException)
                throw (ClassNotFoundException) e.getException();
            throw (RuntimeException) e.getException();
        }
    }

    /**
     * Opens a ServiceTracker. Same as calling tracker.open()
     *
     * @param tracker the ServiceTracker to open.
     */
    public void open(final ServiceTracker<?, ?> tracker) {
        if (System.getSecurityManager() == null) {
            tracker.open();
            return;
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                tracker.open();
                return null;
            }
        }, controlContext);
    }

    public BundleContext getContext(final Bundle bundle) {
        if (System.getSecurityManager() == null) {
            return bundle.getBundleContext();
        }
        return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
            @Override
            public BundleContext run() {
                return bundle.getBundleContext();
            }
        }, controlContext);
    }

    public String getLocation(final Bundle bundle) {
        if (System.getSecurityManager() == null) {
            return bundle.getLocation();
        }
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return bundle.getLocation();
            }
        }, controlContext);
    }

    public ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getClassLoader();
        else
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            }, controlContext);
    }

    private static PrivilegedAction<ClassLoader> getContextClassLoaderAction = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    };

    public ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null)
            return Thread.currentThread().getContextClassLoader();
        else
            return AccessController.doPrivileged(getContextClassLoaderAction, controlContext);
    }

    private static PrivilegedAction<ClassLoader> getSystemClassLoaderAction = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return ClassLoader.getSystemClassLoader();
        }
    };

    public ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null)
            return ClassLoader.getSystemClassLoader();
        else
            return AccessController.doPrivileged(getSystemClassLoaderAction, controlContext);
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public Class<?> loadClass(final ClassLoader cl, final String className) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return cl.loadClass(className);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return cl.loadClass(className);
                    }
                }, controlContext);
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }

    public BundleContext getBundleContext(final Bundle bundle) {
        if (System.getSecurityManager() == null)
            return bundle.getBundleContext();
        else
            return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
                @Override
                public BundleContext run() {
                    return bundle.getBundleContext();
                }
            }, controlContext);
    }

    public Class<?> loadClass(final Bundle b, final String name) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return b.loadClass(name);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return b.loadClass(name);
                    }
                }, controlContext);
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }

    public <S> ServiceReference<S> getServiceReference(final Bundle bundle, final Class<S> clazz) {
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
            }, controlContext);
        }
    }

    public BundleContext getBundleContext(final ComponentContext ctx) {
        if (System.getSecurityManager() == null)
            return ctx.getBundleContext();
        else
            return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
                @Override
                public BundleContext run() {
                    return ctx.getBundleContext();
                }
            }, controlContext);
    }

    public ServiceReference<?> getServiceReference(final ComponentContext ctx) {
        if (System.getSecurityManager() == null)
            return ctx.getServiceReference();
        else
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<?>>() {
                @Override
                public ServiceReference<?> run() {
                    return ctx.getServiceReference();
                }
            }, controlContext);
    }

    public ServiceReference<?>[] getServiceReferences(final ComponentContext ctx, final String clazz, final String filter) throws InvalidSyntaxException {
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
                }, controlContext);
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    public <S> S getService(final ComponentContext ctx, final ServiceReference<S> reference) {
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
            }, controlContext);
    }

    public <S> S getService(final ComponentContext cCtx, final Class<S> clazz) {
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
            }, controlContext);
        }
    }

    public Object locateService(final ComponentContext ctx, final String name) {
        if (System.getSecurityManager() == null)
            return ctx.locateService(name);
        else
            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return ctx.locateService(name);
                }
            }, controlContext);
    }

    public <S> S locateService(final ComponentContext ctx, final String name, final ServiceReference<S> reference) {
        if (System.getSecurityManager() == null)
            return ctx.locateService(name, reference);
        else
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    return ctx.locateService(name, reference);
                }
            }, controlContext);
    }

    public Object[] locateServices(final ComponentContext ctx, final String name) {
        if (System.getSecurityManager() == null)
            return ctx.locateServices(name);
        else
            return AccessController.doPrivileged(new PrivilegedAction<Object[]>() {
                @Override
                public Object[] run() {
                    return ctx.locateServices(name);
                }
            }, controlContext);
    }

    public <S> S getService(final BundleContext bCtx, final ServiceReference<S> ref) {
        if (System.getSecurityManager() == null)
            return bCtx.getService(ref);
        else
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    return bCtx.getService(ref);
                }
            }, controlContext);
    }

    public <S> S getService(final Bundle bundle, final Class<S> clazz) {
        if (System.getSecurityManager() == null) {
            BundleContext bCtx = bundle.getBundleContext();
            if (bCtx == null)
                return null;
            ServiceReference<S> svcRef = bCtx.getServiceReference(clazz);
            return svcRef == null ? null : bCtx.getService(svcRef);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                public S run() {
                    BundleContext bCtx = bundle.getBundleContext();
                    if (bCtx == null)
                        return null;
                    ServiceReference<S> svcRef = bCtx.getServiceReference(clazz);
                    return svcRef == null ? null : bCtx.getService(svcRef);
                }
            }, controlContext);
        }
    }

    public <S> S getService(final BundleContext bCtx, final Class<S> clazz) {
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
            }, controlContext);
        }
    }

    public ServiceReference<?> getServiceReference(final BundleContext bCtx, final String clazz) {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReference(clazz);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<?>>() {
                @Override
                public ServiceReference<?> run() {
                    return bCtx.getServiceReference(clazz);
                }
            }, controlContext);
        }
    }

    public <S> ServiceReference<S> getServiceReference(final BundleContext bCtx, final Class<S> clazz) {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReference(clazz);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ServiceReference<S>>() {
                @Override
                public ServiceReference<S> run() {
                    return bCtx.getServiceReference(clazz);
                }
            }, controlContext);
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public <S> Collection<ServiceReference<S>> getServiceReferences(final BundleContext bCtx, final Class<S> clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Collection<ServiceReference<S>>>() {
                    @Override
                    public Collection<ServiceReference<S>> run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                }, controlContext);
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public ServiceReference<?>[] getServiceReferences(final BundleContext bCtx, final String clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    public ServiceReference<?>[] run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                }, controlContext);
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    public <S> ServiceRegistration<S> registerService(final BundleContext bCtx,
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
            }, controlContext);
    }

    public <S> ServiceRegistration<S> registerService(final BundleContext bCtx,
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
            }, controlContext);
    }

    public ServiceRegistration<?> registerService(final BundleContext bCtx,
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
            }, controlContext);
    }

    public ServiceRegistration<?> registerService(final BundleContext bCtx,
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
            }, controlContext);
    }

    public void setAccessible(final Method m, final boolean accessible) {
        if (System.getSecurityManager() == null) {
            m.setAccessible(accessible);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    m.setAccessible(accessible);
                    return null;
                }
            }, controlContext);
        }
    }

    public void setAccessible(final Field f, final boolean accessible) {
        if (System.getSecurityManager() == null) {
            f.setAccessible(accessible);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    f.setAccessible(accessible);
                    return null;
                }
            }, controlContext);
        }
    }
}
