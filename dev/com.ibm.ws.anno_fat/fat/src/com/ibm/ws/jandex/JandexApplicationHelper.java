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
package com.ibm.ws.jandex;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import com.ibm.ws.packaging.Ear;
import com.ibm.ws.packaging.War;
import com.ibm.ws.packaging.Jar;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class JandexApplicationHelper {

    private static final Logger LOG = Logger.getLogger(JandexApplicationHelper.class.getName());

    /*
     * Helper method to create a war and add it to the dropins directory
     */
    public static void addWarToServerDropins(LibertyServer server, String warName, boolean addWarResources,
                                             String... packageNames) throws Exception {
        addEarToServer(server, "dropins", null, false, warName, addWarResources, null, false, packageNames);
    }
   
    
    /*
     * Helper method to create a war and add it to the apps directory
     */
    public static void addWarToServerApps(LibertyServer server, String warName, boolean addWarResources,
                                          String... packageNames) throws Exception {
        addEarToServer(server, "apps", null, false, warName, addWarResources, null, false, packageNames);
    }

    /*
     * Helper method to create an ear and add it to the dropins directory
     */
    public static void addEarToServerDropins(LibertyServer server, String earName, boolean addEarResources,
                                             String warName, boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        addEarToServer(server, "dropins", earName, addEarResources, warName, addWarResources, jarName, addJarResources,
                       packageNames);
    }
    
    
    /*
     * Helper method to create an ear and add it to the apps directory
     */
    public static void addEarToServerDropins(LibertyServer server, 
    		                                 Ear ear,  
                                             String... packageNames) throws Exception {
        addEarToServer(server, 
        		       "dropins", 
        		       ear);
    }     

    /*
     * Helper method to create an ear and add it to the apps directory
     */
    public static void addEarToServerApps(LibertyServer server, 
    		                              Ear ear) throws Exception {
        addEarToServer(server, 
        		       "apps", 
        		       ear);
    }   
    

    /*
     * Method to create jars, wars and ears for testing. Resources are created
     * as needed and added to dropins or apps directories as specified.
     */
    private static void addEarToServer(LibertyServer server, 
                                       String dir, 
                                       Ear ear) throws Exception {

    	if (ear == null) {
    		LOG.info("addEarToServer : no EAR specified."); 
    		return;
    	}

    	if (ear.getWars().isEmpty()) {
    		LOG.info("addEarToServer : no WAR specified.");
    		return;
    	}

    	String earName = ear.getName();

    	// If server is already started and app exists no need to add it.
    	if (server.isStarted()) {
    		boolean allAppsInstalled = true;  // assume all installed
    		for (War war : ear.getWars()) {
    			String warName = war.getName();
    			String appName = warName.substring(0, warName.indexOf(".war"));
    			Set<String> appInstalled = server.getInstalledAppNames(appName);
    			LOG.info("addEarToServer : " + appName + " already installed : " + !appInstalled.isEmpty());
    			if (appInstalled.isEmpty()) {
    				allAppsInstalled = false;
    				break;  // found a WAR that isn't installed
    			}
    		}
    		if (allAppsInstalled) {
    			LOG.info("addEarToServer : RETURN all apps already installed");
    			return;
    		}
    	} else {
    		LOG.info("addEarToServer : server is not already started.");
    	}
    	
    	LOG.info("addEarToServer : create ear " + earName);
    	EnterpriseArchive enterpriseArchive = ShrinkWrap.create(EnterpriseArchive.class, earName);
    	
    	for (War war : ear.getWars()) {
    		String warName = war.getName();
    		
    		LOG.info("addEarToServer : create war " + warName);
    		WebArchive webArchive = ShrinkWrap.create(WebArchive.class, warName);
    		
    		for (String packageName : war.getPackageNames()) {
    			LOG.info("addEarToServer : WAR adding package " + packageName);
    			webArchive.addPackage(packageName);
    		}

    		for (Jar jar : war.getJars()) {
    			String jarName = jar.getName();
    			LOG.info("addEarToServer : jarName [" + jarName + "]");
    			JavaArchive javaArchive = null;

    			if (jarName != null) {

    				LOG.info("addEarToServer : create jar " + jarName);

    				javaArchive = ShrinkWrap.create(JavaArchive.class, jarName);

    				for (String packageName : jar.getPackageNames()) {
    					if (packageName.contains(".jar.")) {
    						LOG.info("addEarToServer : JAR adding package " + packageName);
    						javaArchive.addPackage(packageName);
    					}
    				}

    				// addJarResources
    				String jarResourcesDir = "test-applications/" + jarName + "/resources";
    				LOG.info("addEarToServer : JAR adding resources " + jarName);
    		        ShrinkHelper.addDirectory(javaArchive, jarResourcesDir);

    			} else {
    				LOG.info("addEarToServer : unexpectedly jarName is null");
    			}
    			
        		if (javaArchive != null) {
        			LOG.info("addEarToServer : WAR adding JAR " + jarName);
        			webArchive.addAsLibrary(javaArchive);
        		} else {
        			LOG.info("addEarToServer : Strange - javaArchive is null " + jarName);
        		}
    		}

    		// add War Resources
    		String warResourcesDir = "test-applications/" + warName + "/resources";
			LOG.info("addEarToServer : WAR adding resources " + warName);
    		ShrinkHelper.addDirectory(webArchive, warResourcesDir);
    		
    		LOG.info("addEarToServer : EAR adding WAR " + warName);
    		enterpriseArchive.addAsModule(webArchive);
    	}
	
    	// Add Ear Resources
    	String earResources = "test-applications/" + earName + "/resources/META-INF/application.xml";
		LOG.info("addEarToServer : Adding resource to EAR " + earName);
    	enterpriseArchive.addAsManifestResource(new File(earResources));

    	//Liberty does not use was.policy but permissions.xml
    	File permissionsXML = new File("test-applications/" + earName + "/resources/META-INF/permissions.xml");
    	if (permissionsXML.exists()) {
    		enterpriseArchive.addAsManifestResource(permissionsXML);
    	}
    	
    	 ShrinkHelper.exportToServer(server, dir, enterpriseArchive);
    }

    /*
     * Helper method to create an ear and add it to the apps directory
     */
    public static void addEarToServerApps(LibertyServer server, String earName, boolean addEarResources, String warName,
                                          boolean addWarResources, String jarName, boolean addJarResources, String... packageNames) throws Exception {
        addEarToServer(server, "apps", earName, addEarResources, warName, addWarResources, jarName, addJarResources,
                       packageNames);
    }
    
    /*
     * Method to create jars, wars and ears for testing. Resources are created
     * as needed and added to dropins or apps directories as specified.
     */
    private static void addEarToServer(LibertyServer server, 
    		                           String dir, 
    		                           String earName, 
    		                           boolean addEarResources,
                                       String warName, 
                                       boolean addWarResources, 
                                       String jarName, 
                                       boolean addJarResources, 
                                       String... packageNames) throws Exception {

        if (warName == null)
            return;

        // If server is already started and app exists no need to add it.
        if (server.isStarted()) {
            String appName = warName.substring(0, warName.indexOf(".war"));
            Set<String> appInstalled = server.getInstalledAppNames(appName);
            LOG.info("addEarToServer : " + appName + " already installed : " + !appInstalled.isEmpty());

            if (!appInstalled.isEmpty())
                return;
        }

        JavaArchive jar = null;
        WebArchive war = null;

        if (jarName != null) {

            LOG.info("addEarToServer : create jar " + jarName + ", jar includes resources : " + addJarResources);

            jar = ShrinkWrap.create(JavaArchive.class, jarName);
            if (packageNames != null) {
                for (String packageName : packageNames) {
                    if (packageName.contains(".jar.")) {
                        jar.addPackage(packageName);
                    }
                }
            }
            if (addJarResources)
                ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources");
        }

        war = ShrinkWrap.create(WebArchive.class, warName);
        LOG.info("addEarToServer : create war " + warName + ", war includes resources : " + addWarResources);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                if (packageName.contains(".war.")) {
                    war.addPackage(packageName);
                }
            }
        }
        if (jar != null)
            war.addAsLibrary(jar);
        if (addWarResources)
            ShrinkHelper.addDirectory(war, "test-applications/" + warName + "/resources");

        if (earName != null) {
            LOG.info("addEarToServer : crteate ear " + earName + ", ear include application/.xml : " + addEarResources);
            EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName);
            ear.addAsModule(war);
            if (addEarResources)
                ear.addAsManifestResource(
                                          new File("test-applications/" + earName + "/resources/META-INF/application.xml"));

            //Liberty does not use was.policy but permissions.xml
            File permissionsXML = new File("test-applications/" + earName + "/resources/META-INF/permissions.xml");
            if (permissionsXML.exists()) {
                ear.addAsManifestResource(permissionsXML);
            }

            ShrinkHelper.exportToServer(server, dir, ear);
        } else {
            ShrinkHelper.exportToServer(server, dir, war);
        }

    }
}
