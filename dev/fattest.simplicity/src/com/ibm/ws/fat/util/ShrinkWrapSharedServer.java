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

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import org.jboss.shrinkwrap.api.Archive;

import org.junit.Test;

public class ShrinkWrapSharedServer extends SharedServer {

    private boolean shutdownAfterTest = true;
    private final Map<Archive,List<String>> archivesAndPaths;
    private static Class<?> c = ShrinkWrapSharedServer.class;

     /**
     * Creates a {@link SharedServer} then runs any methods in testClass annotated with
     * @BuildShrinkWrap and copies the returned Archives to the servers 
     *
     * Methods must be static, have no paramaters, return: Archive, Archive[], List<Archive> Map<Archive,String>. 
     * If Archive, List<Archive> or Archive[] is returned the returned values will be placed to the server's 
     * dropins folders. If a map is returned each archive will be placed whereever the
     * string points too.
     * 
     * If the method returns the wrong type an exception will be logged and the test will proceed
     * without that application. 
     */
    public ShrinkWrapSharedServer(String serverName, Class testClass){
        super(serverName);
        archivesAndPaths = new HashMap<Archive,List<String>>();
        getArchivesViaAnnotation(serverName, testClass);
    }

    private void getArchivesViaAnnotation(String serverName, Class testClass) {
        String dropinsPath = "/dropins";

        for (Method method : testClass.getDeclaredMethods()){
            if (method.isAnnotationPresent(BuildShrinkWrap.class)){
                try { 
                    Object archive = method.invoke(null);
                    if (archive == null) {
                        //do nothing
                    } else if (archive instanceof Archive){
                        archivesAndPaths.put((Archive) archive, Arrays.asList(dropinsPath));
                    } else if (archive instanceof Archive[]){
                        Archive[] archives = (Archive[]) archive;
                        for (Archive a : archives) {
                            if (a != null) { 
                              archivesAndPaths.put(a, Arrays.asList(dropinsPath));
                            }
                        }
                    } else if (archive instanceof List<?>){
                        List<?> archives = (List<?>) archive;
                        for (Object a : archives) {
                            if (! (a instanceof Archive)) {
                                throw new IllegalArgumentException("A method annotated BuildShrinkWrap returned a List, but an entry was not an Archive");
                            }
                            if (a != null) { 
                              archivesAndPaths.put((Archive) a, Arrays.asList(dropinsPath));
                            }
                        }
                    } else if (archive instanceof Map<?,?>) { 
                        Map<?,?> archiveMap = (Map<?,?>) archive;
                        for (Object key : archiveMap.keySet()){
                            Object value = archiveMap.get(key);
                            if (! (key instanceof Archive)){
                                throw new IllegalArgumentException("A method annotated BuildShrinkWrap returned a map, but the key was not an Archive");
                            }
                            if (! (value instanceof String) && ! (value instanceof List) && ! (((List) value).get(0) instanceof String) ){
                                throw new IllegalArgumentException("A method annotated BuildShrinkWrap returned a map, but the key was not a String or a List of Strings with at least one element");
                            }
                            if (value instanceof String) {
                                archivesAndPaths.put((Archive) key, Arrays.asList((String) value));
                            } else {
                                archivesAndPaths.put((Archive) key, (List) value);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("A method annotated BuildShrinkWrap did not return an Archive, an array of Archives, a map of <Archive,String>, or a map of <Archive,List<String>>");
                    }
                } catch (Exception e) {
                    //We log and eat the exceptions here, so the constructor can be invoked statically and exceptions will make the test fail with an easy to debug error message. 
                    Log.error(c, "<init>", e);
                }
            }
        }
    }

    /**
     * A convinence wrapper around 
     * ShrinkWrapSharedServer(String serverName, Class testClass)
     * 
     * This method reflexively gets the first class with method annotated
     * @Test on the stack then calls 
     * ShrinkWrapSharedServer(String serverName, Class testClass) with that
     * class. Inhereted methods are ignored. 
     */
    public ShrinkWrapSharedServer(String serverName){
        super(serverName);
        Log.info(c, "<init>", "buildingServer: " + serverName); 
        archivesAndPaths = new HashMap<Archive,List<String>>();
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            int i = 0;
            Class testClass = null;
            while (testClass == null) {
                StackTraceElement element = stackTrace[i];
                Class clazz = Class.forName(element.getClassName());
                for (Method method : clazz.getDeclaredMethods()){
                    if (method.isAnnotationPresent(Test.class)){
                        testClass = clazz;
                        Log.info(c, "<init>", "found test class: " + testClass.getCanonicalName()); 
                        break;
                    }
                }
                i++;
            }
            getArchivesViaAnnotation(serverName, testClass);
        } catch (Exception e) {
            //We log and eat the exceptions here, so the constructor can be invoked statically and exceptions will make the test fail with an easy to debug error message. 
            Log.error(c, "<init>", e);
        }
    }



     /**
     * Creates a {@link SharedServer} and copies ShrinkWrap archives to the server
     *
     * Where buildApps() is a static method that returns Archive or Archive[]
     *
     * As ShrinkWrap can throw exceptions when creating an archive you will need to 
     * make sure all exceptions are handled if you invoke this constructor from a
     * static context.
     *
     * @param shirnkWrapArchives ShrinkWrap archives with will be installed to the server's
     * dropins folder.
     */
    public ShrinkWrapSharedServer(String serverName, Archive... shirnkWrapArchives){
        super(serverName);

        archivesAndPaths = new HashMap<Archive,List<String>>();
        String dropinsPath = "/dropins";
        
        for (Archive archive : shirnkWrapArchives) {
            archivesAndPaths.put(archive, Arrays.asList(dropinsPath));
        }
    }

     /**
     * Creates a {@link SharedServer} and copies ShrinkWrap archives to the server
     *
     * Where buildApps() is a static method that returns Map<Archive,String>
     *
     * As ShrinkWrap can throw exceptions when creating an archive you will need to 
     * make sure all exceptions are handled if you invoke this constructor from a
     * static context.
     *
     * @param archivesAndPaths a map of ShrinkWrap archives and their install paths.
     */
    public ShrinkWrapSharedServer(String serverName, Map<Archive,List<String>> archivesAndPaths){
        super(serverName);
        this.archivesAndPaths = archivesAndPaths;
    }

    //I'm putting this here rather than as part of the test itself for two reasons: 
    //It keeps all the boilerplate needed for ShrinkWrap in a single class. 
    //Secondly it has to occur before LibertyServerFactory is invoked, and that happens
    //in SharedServer.before(). This way I keep the ordering dependencies isolated to a single 
    //codepath.
    @Override 
    protected void before() {
        for (Archive archive : archivesAndPaths.keySet()){
            //This takes place before the servers are copied, so we do not need to worry about
            //moving archives ourselves beyond this. 
            for (String path : archivesAndPaths.get(archive)) { 
                try {
                    ShrinkHelper.exportToServer(getLibertyServer(), path, archive);
                } catch (Exception e) {
                    throw new RuntimeException(e); //TODO something better here. 
                }
            }
        }
        super.before();
   }

    //I don't think it should be nessacary to shut down a SharedServer after every test but this is
    //Just a guess based on the name "SharedServer" and the fact this addition was nessacary when I
    //Ported from WS-CD open. TODO, investigate this further. 
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
