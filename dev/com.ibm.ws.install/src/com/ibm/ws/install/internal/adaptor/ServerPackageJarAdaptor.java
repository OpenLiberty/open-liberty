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
package com.ibm.ws.install.internal.adaptor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.asset.JarAsset;
import com.ibm.ws.install.internal.asset.ServerPackageAsset;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;

import wlp.lib.extract.MapBasedSelfExtractor;

public class ServerPackageJarAdaptor extends ArchiveAdaptor {

    private static Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

    @SuppressWarnings({ "unchecked", "resource" })
    public static void install(Product product, final JarAsset jarAsset, List<File> filesInstalled, boolean downloadDependencies,
                               RestRepositoryConnectionProxy proxy) throws IOException, InstallException {
        // Setup
        Map<String, Object> extractor;
        final URLClassLoader[] loader = new URLClassLoader[] { null };
        try {
            extractor = AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, Object>>() {
                @Override
                public Map<String, Object> run() throws Exception {
                    loader[0] = new URLClassLoader(new URL[] { jarAsset.getAsset().toURI().toURL() }, null);
                    Class<Map<String, Object>> clazz;
                    clazz = (Class<Map<String, Object>>) loader[0].loadClass("wlp.lib.extract.MapBasedSelfExtractor");
                    return clazz.newInstance();
                }
            });
        } catch (PrivilegedActionException e) {
            close(loader[0]);
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_RUN_EXTRACTOR", jarAsset.getJarPath(),
                                                                                      e.getMessage()), e, InstallException.RUNTIME_EXCEPTION);
        }

        extractor.put(MapBasedSelfExtractor.INSTALL_VERSION, Integer.valueOf(1));
        extractor.put(MapBasedSelfExtractor.INSTALL_DIR, product.getInstallDir());
        extractor.put(MapBasedSelfExtractor.TARGET_USER_DIR, product.getUserDir());
        extractor.put(MapBasedSelfExtractor.LICENSE_ACCEPT, Boolean.TRUE);
        extractor.put(MapBasedSelfExtractor.DOWNLOAD_DEPS, Boolean.valueOf(false));
        logger.log(Level.FINEST, "ServerPackageJarAdaptor downloadDependencies: " + downloadDependencies);

        @SuppressWarnings("rawtypes")
        List extDeps = (List) extractor.get(MapBasedSelfExtractor.LIST_EXTERNAL_DEPS);
        List<File> filesToBeInstalled = getFilesToBeInstalled(jarAsset, downloadDependencies ? extDeps : Collections.EMPTY_LIST);
        for (File f : filesToBeInstalled) {
            logger.log(Level.FINEST, "ServerPackageJarAdaptor file to be installed: " + f.getAbsolutePath());
        }
        filesInstalled.addAll(filesToBeInstalled);

        // Execute
        int rc = (Integer) extractor.get(MapBasedSelfExtractor.INSTALL_BUILD_CODE);
        logger.log(Level.FINEST, "ServerPackageJarAdaptor install: " + MapBasedSelfExtractor.INSTALL_BUILD_CODE + "=" + rc);

        PrintStream orgOut = System.out;
        PrintStream orgErr = System.err;
        final StringBuilder sb = new StringBuilder();
        OutputStream outStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                sb.append((char) b);
            }
        };
        PrintStream newOut = new PrintStream(outStream, true);
        System.setOut(newOut);
        System.setErr(newOut);

        rc = (Integer) extractor.get(MapBasedSelfExtractor.INSTALL_CODE);
        logger.log(Level.FINEST, "ServerPackageJarAdaptor install: " + MapBasedSelfExtractor.INSTALL_CODE + "=" + rc);

        System.setOut(orgOut);
        System.setErr(orgErr);

        logger.log(Level.FINEST, "ServerPackageJarAdaptor install stdout/stderr: " + sb.toString());
        String closeErrMsg = (String) extractor.get(MapBasedSelfExtractor.CLOSE);
        if (closeErrMsg != null) {
            logger.log(Level.FINEST, "ServerPackageJarAdaptor failed to close extractor: " + closeErrMsg);
        }
        close(loader[0]);

        if (rc != 0) {
            String errorMsg = (String) extractor.get(MapBasedSelfExtractor.INSTALL_ERROR_MESSAGE);
            InstallUtils.delete(filesInstalled);
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_RUN_EXTRACTOR", jarAsset.getJarPath(),
                                                                                      errorMsg), InstallException.RUNTIME_EXCEPTION);
        }

        if (downloadDependencies) {
            try {
                downloadExtDependencies(extDeps, proxy);
            } catch (InstallException ie) {
                InstallUtils.delete(filesInstalled);
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_RUN_EXTRACTOR", jarAsset.getJarPath(), ie.getMessage()), ie, ie.getRc());
            }
        }
        extDeps = null;
    }

    /**
     * @param urlClassLoader
     */
    private static void close(URLClassLoader loader) {
        if (loader == null)
            return;
        java.lang.reflect.Method m;
        try {
            m = loader.getClass().getDeclaredMethod("close", new Class[0]);
            m.invoke(loader);
        } catch (Exception e) {
            logger.log(Level.WARNING, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_CANNOT_CLOSE_OBJECT"));
        }
    }

    @SuppressWarnings("rawtypes")
    private static List<File> getFilesToBeInstalled(JarAsset jarAsset, List extDeps) {
        List<File> filesToBeInstalled = new ArrayList<File>();
        File userDir = Utils.getUserDir();
        Enumeration<? extends ZipEntry> entries = jarAsset.getJar().entries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = entries.nextElement();
            String zeName = ze.getName().toLowerCase();
            if (zeName.startsWith(WLP_USR)) {
                if (!ze.isDirectory()) {
                    String entryName = ze.getName().substring(WLP_USR.length());
                    File f = new File(userDir, entryName);
                    if (!f.exists()) {
                        filesToBeInstalled.add(f);
                    }
                    if (f.getName().equalsIgnoreCase(ServerPackageAsset.SERVER_FILENAME)) {
                        filesToBeInstalled.add(f.getParentFile());
                    }
                }
            }
        }
        for (Object e : extDeps) {
            Map m = (Map) e;
            File f = new File(userDir, (String) m.get("download.target"));
            if (!f.exists()) {
                filesToBeInstalled.add(f);
            }
        }
        return filesToBeInstalled;
    }

    @SuppressWarnings("rawtypes")
    private static void downloadExtDependencies(List extDeps, RestRepositoryConnectionProxy proxy) throws InstallException {
        if (extDeps == null || extDeps.isEmpty())
            return;
        File userDir = Utils.getUserDir();
        Proxy javaNetProxy = null;
        if (proxy != null)
            javaNetProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getProxyURL().getHost(), proxy.getProxyURL().getPort()));
        for (Object extDep : extDeps) {
            Map m = (Map) extDep;
            URL url = (URL) m.get("download.url");
            if (url == null) {
                continue;
            }
            File targetFile = new File(userDir, (String) m.get("download.target"));
            if (targetFile.exists()) {
                logger.log(Level.FINEST, "External library " + url + " already exists in " + targetFile.getAbsolutePath());
            } else {
                if (!targetFile.getParentFile().exists()) {
                    if (!targetFile.getParentFile().mkdirs()) {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DOWNLOADONLY_UNABLE_TO_CREATE_DIR",
                                                                                                  targetFile.getParentFile().getAbsolutePath()), InstallException.IO_FAILURE);
                    }
                }
                HttpURLConnection connection = null;
                try {
                    logger.log(Level.FINE, Messages.SELF_EXTRACTOR_MESSAGES.getLogMessage("downloadingFileNotice", url, targetFile.getAbsolutePath()));
                    connection = (HttpURLConnection) (javaNetProxy == null ? url.openConnection() : url.openConnection(javaNetProxy));
                    connection.setReadTimeout(30 * 1000);
                    InstallUtils.download(connection.getInputStream(), targetFile);
                    logger.log(Level.FINEST, "External library " + url + " was successfully downloaded to " + targetFile.getAbsolutePath());
                } catch (IOException e) {
                    throw new InstallException(Messages.SELF_EXTRACTOR_MESSAGES.getLogMessage("downloadFileError", url,
                                                                                              targetFile.getAbsolutePath()), e, InstallException.CONNECTION_FAILED);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }
    }
}
