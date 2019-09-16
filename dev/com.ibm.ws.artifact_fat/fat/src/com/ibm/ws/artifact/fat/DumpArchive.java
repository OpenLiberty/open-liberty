/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat;

import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilenameFilter;

import componenttest.topology.impl.LibertyServer;

import com.ibm.websphere.simplicity.log.Log;

public class DumpArchive extends ZipFile{

    public static String ZIP_CACHING_INTROSPECTOR_FILE_NAME = "ZipCachingIntrospector.txt";

    private static class DumpArchiveFilenameFilter implements FilenameFilter{

        private final String serverName;

        public DumpArchiveFilenameFilter(String serverName){
            this.serverName = serverName;
        }

        @Override
        public boolean accept(File dir, String name){
            return name.contains(serverName + ".dump");
        }
    }

    private static void logInfo(String methodName, String outputString){
        FATLogging.info(DumpArchive.class, methodName, outputString);
    }

    private DumpArchive(File dumpArchive) throws IOException{
        super(dumpArchive);
    }

    private ZipEntry getIntrospectorDumpFile(String introspectorFileName){
        Enumeration<? extends ZipEntry> e = entries();
        ZipEntry currentEntry = e.nextElement();
        
        //if there aren't any entries in the dump return null
        if(currentEntry == null){
            return null;
        }
        else{
            //if the first entry is the file we are looking for then return it
            if(currentEntry.getName().contains(introspectorFileName) && !currentEntry.isDirectory()){
                return currentEntry;
            }
            //iterate over all the entires until the file is found
            while(e.hasMoreElements()){
                //get the next zip entry
                currentEntry = e.nextElement();
                
                //if the file is found the return it
                if(currentEntry.getName().contains(introspectorFileName) && !currentEntry.isDirectory()){
                    return currentEntry;
                }
            }

            return null;
        }

    }

    public boolean doesZipCachingIntrospectorDumpExist(){

        return getIntrospectorDumpFile(ZIP_CACHING_INTROSPECTOR_FILE_NAME) != null;
    }

    public InputStream getZipCachingDumpStream() throws IOException{
        ZipEntry introspectorDump;
        if((introspectorDump = getIntrospectorDumpFile(ZIP_CACHING_INTROSPECTOR_FILE_NAME)) != null){
            return getInputStream(introspectorDump);
        }
        else{
            throw new ZipException(String.format("%s missing from Archive [%s]",ZIP_CACHING_INTROSPECTOR_FILE_NAME,this.getName()));
        }
    } 


    public static DumpArchive getMostRecentDumpArchive(LibertyServer server) throws IOException{
        if(server == null)
            return null;

        String serverRoot = server.getServerRoot();
        File serverDirectory = new File(serverRoot);
        File[] dumpArchives;
        int mostRecentIndex;

        if(serverDirectory.isDirectory()){
            dumpArchives = serverDirectory.listFiles(new DumpArchiveFilenameFilter(server.getServerName()));
            if(dumpArchives.length == 1){
                return new DumpArchive(dumpArchives[0]);
            }
            else if(dumpArchives.length == 0){
                return null;
            }
            else{
                mostRecentIndex = 0;
                for(int counter = 1; counter < dumpArchives.length; ++counter){
                    if(dumpArchives[counter].lastModified() > dumpArchives[mostRecentIndex].lastModified()){
                        mostRecentIndex = counter;
                    }
                }

                return new DumpArchive(dumpArchives[mostRecentIndex]);
            }
        }
        else{
            return null;
        }   
    }

}