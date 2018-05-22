/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.embeddable;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Properties;

import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent;

/**
 * Builder for constructing an embedded <code>Server</code>.
 * <p>
 * Use this Builder to set immutable server attributes, then call
 * the {@link #build()} method to build and return a <code>Server</code> instance.
 * <p>
 * As an example, to build a new <code>Server</code> instance, this snippet would suffice:
 *
 * <pre>
 * Server defaultServer = new ServerBuilder().setName("embeddedServer").build();
 * </pre>
 */
public class ServerBuilder {

    private String serverName = null;
    private File userDir = null;
    private File outputDir = null;
    private File logDir = null;
    private ServerEventListener listener = null;
    private File installDir;
    private HashMap<String, Properties> productExtensions = null;

    private static class InvalidInstallException extends ServerException {

        /**
         * @param message
         * @param translatedMsg
         * @param cause
         */
        public InvalidInstallException(Throwable cause) {
            super(cause.getMessage(), cause.getMessage(), cause);
        }

        /** The serial version uid */
        private static final long serialVersionUID = -1897435497398897268L;

    }

    /**
     * Set the locally unique name for the server; the name can be constructed
     * using Unicode alphanumeric characters (for example, A-Za-z0-9), the underscore (_),
     * dash (-), plus (+) and period (.). A server name cannot begin with a dash (-) or
     * period (.).
     *
     * @param serverName The name for the server instance.
     * @return a reference to this object.
     */
    public ServerBuilder setName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    /**
     * Set the path of the user directory. The server must have read access to this directory.
     * This is the equivalent of setting the <code>WLP_USER_DIR</code> environment
     * variable when launching the server from the command line.
     * <p>
     * This is optional. By default, the user directory will be <code>wlp/usr</code>, where
     * <code>wlp</code> is the root of the Liberty installation as determined by the
     * location of the jar providing the server implementation.
     * <p>
     * This path is referred to by the <code>${wlp.user.dir}</code> variable.
     * <p>
     * The server configuration directory, <code>${server.config.dir}</code>,
     * is equivalent to <code>${wlp.user.dir}/servers/serverName</code>.
     *
     * @param userDir The path to the user directory.
     * @return a reference to this object.
     */
    public ServerBuilder setUserDir(File userDir) {
        this.userDir = userDir;
        return this;
    }

    /**
     * Set the path of the user output directory. The server must have read/write access to
     * this directory. This is the equivalent of setting the <code>WLP_OUTPUT_DIR</code> environment
     * variable when launching the server from the command line.
     * <p>
     * This is optional. By default, the user output directory will be <code>${wlp.user.dir}/servers</code>.
     * <p>
     * If this is specified, <code>${server.output.dir}</code> will be set to <code>outputDir/serverName</code>.
     * Otherwise, <code>${server.output.dir}</code> will be the same as <code>${server.config.dir}</code>.
     *
     * @param outputDir The path to the user output directory.
     * @return a reference to this object.
     * @see #setUserDir(File)
     */
    public ServerBuilder setOutputDir(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /**
     * Set the path of the log directory. The server must have read/write access to
     * this directory. This is the equivalent of setting the <code>LOG_DIR</code> environment
     * variable when launching the server from the command line.
     *
     * @param logDir The path to the log directory.
     * @return a reference to this object.
     */
    public ServerBuilder setLogDir(File logDir) {
        this.logDir = logDir;
        return this;
    }

    /**
     * Sets the path of the Liberty install.
     *
     * <p>This is optional. If the Liberty ws-server.jar is on the classpath of
     * the caller this method has no effect. If it is not this allows the Liberty
     * install to be chosen.</p>
     *
     * @param install The path to the Liberty install.
     * @return a reference to this object.
     */
    public ServerBuilder setInstallDir(File install) {
        installDir = install;
        return this;
    }

    /**
     * Set the listener that will receive {@link ServerEvent} notifications.
     * <p>
     * Using a {@link ServerEventListener} is optional.
     *
     * @param listener The <code>ServerEventListener</code> to notify.
     */
    public ServerBuilder setServerEventListener(ServerEventListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Get a new {@link Server} instance. This will fail if the server does not
     * already exist (<code>${server.config.dir}</code> must exist, and must contain
     * a <code>server.xml</code> file).
     *
     * @return a Server instance using any attributes set on the builder.
     *
     * @throws ServerException if the named server does not exist, or if the attributes
     *             set using the set methods fail validation, e.g. the server name
     *             contains invalid characters, or the provided Files point to existing
     *             files in the file system instead of directories.
     */
    public Server build() throws ServerException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Server>() {

                @Override
                public Server run() {
                    return buildWithPriv();
                }
            });
        } catch (PrivilegedActionException e) {
            Exception target = e.getException();
            if (target instanceof ServerException) {
                throw (ServerException) target;
            } else if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            } else {
                throw new InvalidInstallException(target);
            }
        }
    }

    /**
     * Add a product extension.
     *
     * <p>
     * The addProductExtension method can be called
     * multiple times to add multiple extensions.
     * <p>
     * When the server is started, any product extension files added by this method
     * will be combined with the product extension files found in other source locations.
     * Any duplicate product names will invoke the following override logic: product extensions
     * defined through this SPI will override product extensions defined by the Environment
     * variable <code>WLP_PRODUCT_EXT_DIR</code> which in turn would override product extensions
     * found in
     * <code>$WLP_INSTALL_DIR/etc/extensions</code> to constitute the full set of product
     * extensions for this instance of the running server.
     *
     * @param name The name of the product extension.
     * @param props A properties file containing com.ibm.websphere.productId and com.ibm.websphere.productInstall.
     * @return a reference to this object.
     */
    public ServerBuilder addProductExtension(String name, Properties props) {
        if ((name != null) && (props != null)) {
            if (productExtensions == null) {
                productExtensions = new HashMap<String, Properties>();
            }
            this.productExtensions.put(name, props);
        }
        return this;
    }

    private Server buildWithPriv() {
        ClassLoader cl = ServerBuilder.class.getClassLoader();
        if (installDir != null) {
            File wsServerJar = new File(installDir, "bin/tools/ws-server.jar");
            if (wsServerJar.exists()) {
                try {
                    cl = new URLClassLoader(new URL[] { wsServerJar.toURI().toURL() }, cl);
                } catch (MalformedURLException e) {
                    // Ignore since this shouldn't happen
                }
            } else {
                FileNotFoundException e = new FileNotFoundException(installDir.getAbsolutePath());
                throw new InvalidInstallException(e);
            }

        }

        try {
            @SuppressWarnings("unchecked")
            Server server;
            Class<? extends Server> clazz = (Class<? extends Server>) cl.loadClass("com.ibm.ws.kernel.boot.EmbeddedServerImpl");
            Constructor<? extends Server> con = clazz.getConstructor(String.class, File.class, File.class, File.class, ServerEventListener.class, HashMap.class);
            server = con.newInstance(serverName, userDir, outputDir, logDir, listener, productExtensions);

            return server;
        } catch (ClassNotFoundException e) {
            throw new InvalidInstallException(e);
        } catch (NoSuchMethodException e) {
            throw new InvalidInstallException(e);
        } catch (InstantiationException e) {
            throw new InvalidInstallException(e);
        } catch (IllegalAccessException e) {
            throw new InvalidInstallException(e);
        } catch (IllegalArgumentException e) {
            throw new InvalidInstallException(e);
        } catch (InvocationTargetException e) {
            throw new InvalidInstallException(e);
        }
    }
}
