/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

/**
 * <p>Extends a {@link SharedServer} with methods useful for testing applications.</p>
 * <p>created by shrinkwrap.</p>
 *
 * @author Benjamin Confino
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.EnterpriseContainer;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

public class ImplicitShrinkWrapSharedServer extends SharedServer {

    private final boolean shutdownAfterTest = true;
    private static final Map<String, Archive> archivesAndPaths = new HashMap<String, Archive>();
    private static final Map<String, Archive> archivesByName = new HashMap<String, Archive>();
    private static final Map<String, List<Archive>> serversAndApps = new HashMap<String, List<Archive>>();
    private static Class<?> c = ImplicitShrinkWrapSharedServer.class;

    //static for performance reasons.
    private static Map<String, File> serverApps = null;

    public ImplicitShrinkWrapSharedServer(String serverName) {
        super(serverName);

        buildAllArchives();
        wireArchives();
    }

    @SuppressWarnings("rawtypes")
    private void wireArchives() {
        for (String archiveDirPath : archivesAndPaths.keySet()) {
            File packageProperties = new File(archiveDirPath + "/package.properties");
            try {
                FileReader fileReader = new FileReader(packageProperties);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                do {
                    line = bufferedReader.readLine();
                    if (line.startsWith("module.pattern")) {
                        String modulesLine = line.split("=")[1];
                        String[] modules = modulesLine.split(",");
                        for (String archiveName : modules) {
                            EnterpriseContainer eModule = (EnterpriseContainer) archivesAndPaths.get(archiveDirPath);
                            eModule.addAsModule(archivesByName.get(archiveName));
                        }
                    } else if (line.startsWith("lib.pattern")) {
                        String modulesLine = line.split("=")[1];
                        String[] modules = modulesLine.split(",");
                        for (String archiveName : modules) {
                            LibraryContainer lModule = (LibraryContainer) archivesAndPaths.get(archiveDirPath);
                            lModule.addAsLibrary(archivesByName.get(archiveName));
                        }
                    } else if (line.startsWith("server.pattern")) {
                        String serversLine = line.split("=")[1];
                        String[] servers = serversLine.split(",");
                        for (String server : servers) {
                            List<Archive> archives = serversAndApps.get(server);
                            if (archives == null) {
                                archives = new LinkedList<Archive>();
                                serversAndApps.put(server, archives);
                            }
                            archives.add(archivesAndPaths.get(archiveDirPath));
                        }
                    }

                    //TODO serverLib

                } while (line != null);
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void wireModule(Archive parent, String childName) {

    }

    @SuppressWarnings("rawtypes")
    private void buildAllArchives() {
        File testAppsDir = new File("test-applications");
        for (File testAppDir : testAppsDir.listFiles()) {
            try {
                Archive app = null;
                if (testAppDir.getCanonicalPath().endsWith("jar")) {
                    app = ShrinkWrap.create(JavaArchive.class, testAppDir.getName());
                } else if (testAppDir.getCanonicalPath().endsWith("war")) {
                    app = ShrinkWrap.create(WebArchive.class, testAppDir.getName());
                } else if (testAppDir.getCanonicalPath().endsWith("war")) {
                    app = ShrinkWrap.create(EnterpriseArchive.class, testAppDir.getName());
                }

                //Convert File back to string so we don't hold refernces forever
                archivesAndPaths.put(testAppDir.getCanonicalPath(), app);
                archivesByName.put(testAppDir.getName(), app);

                File srcDirectory = new File(testAppDir.getPath() + "/src");
                List<File> allJavaFiles = allFilesUnderDirectory(srcDirectory);

                for (File appFile : allJavaFiles) {
                    if (appFile.getName().endsWith(".java")) {
                        String javaName = getJavaName(appFile, testAppsDir);
                        ClassContainer cApp = (ClassContainer) app;
                        cApp.addClass(javaName);
                    }
                }
                srcDirectory = null;
                allJavaFiles.clear();
                allJavaFiles = null;

                File resourcesDirectory = new File(testAppDir.getPath() + "/resources");
                List<File> allResourcesFiles = allFilesUnderDirectory(resourcesDirectory);

                for (File appFile : allResourcesFiles) {
                    String destPath = appFile.getPath().split(resourcesDirectory.getPath())[1];
                    app.add(new FileAsset(appFile), destPath);
                }
            } catch (IOException e) {
                Log.error(c, "Failed to create app for" + testAppDir.getName(), e);
            }

        }
    }

    private String getJavaName(File javaFile, File workingDirectory) {
        String path = javaFile.getPath();
        String pathPart = path.split(workingDirectory.getPath(), 2)[1];
        String packagePath = pathPart.split("src", 2)[1];
        String javaPath = packagePath.replaceAll("\\", ".").replaceAll("/", ".");
        return javaPath;
    }

    private List<File> allFilesUnderDirectory(File testAppDir) {
        List<File> allFiles = new LinkedList<File>();
        for (File child : testAppDir.listFiles()) {
            if (child.isDirectory()) {
                //This should be safe from stack overflows. We don't have that many nested directories.
                allFiles.addAll(allFilesUnderDirectory(child));
            } else {
                allFiles.add(child);
            }
        }
        return allFiles;
    }

    //I'm putting this here rather than as part of the test itself for two reasons:
    //It keeps all the boilerplate needed for ShrinkWrap in a single class.
    //Secondly it has to occur before LibertyServerFactory is invoked, and that happens
    //in SharedServer.before(). This way I keep the ordering dependencies isolated to a single
    //codepath.
    @Override
    protected void before() {
        String dropinsPath = "publish/servers/" + getServerName() + "/dropins";
        for (Archive archive : serversAndApps.get(getServerName())) {
            //This takes place before the servers are copied, so we do not need to worry about
            //moving archives ourselves beyond this.
            ShrinkHelper.exportArtifact(archive, dropinsPath);
        }
        super.before();
    }

    @Override
    protected void after() {
        if (shutdownAfterTest && getLibertyServer().isStarted()) {
            try {
                getLibertyServer().stopServer();
            } catch (Exception e) {
                throw new RuntimeException(e); //TODO something better here.
            }
        }
    }

}
