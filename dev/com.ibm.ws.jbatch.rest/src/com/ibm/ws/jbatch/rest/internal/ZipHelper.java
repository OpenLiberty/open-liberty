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
package com.ibm.ws.jbatch.rest.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.ws.jbatch.rest.utils.StringUtils;

public class ZipHelper {
    

    public static void zipFileToStream(File file, List<File> zipRootDirs, OutputStream outputStream) throws IOException {
        zipFilesToStream(Arrays.asList(file), zipRootDirs, outputStream);
    }
    
    public static void zipFileToStream(File file, File zipRootDir, OutputStream outputStream) throws IOException {
        zipFilesToStream(Arrays.asList(file), Arrays.asList(zipRootDir), outputStream);
    }
    
    public static void zipFilesToStream(List<File> files, File zipRootDir, OutputStream outputStream) throws IOException {
        zipFilesToStream(files, Arrays.asList(zipRootDir), outputStream);
    }

    /**
     * Zip up the files and write the zipped data to the given output stream.
     * 
     * @param files The files to zip up
     * @param remoteZipFiles 
     * @param zipRootDirs The root dirs for the zip file.  The root dir's parent dir  
     *                    is stripped away from canonical file name when making the zip entries.
     * @param outputStream the zipped data is written here
     * 
     */
    public static void zipFilesToStream(List<File> files, 
                                        List<File> zipRootDirs,
                                        OutputStream outputStream) throws IOException {
    	  	        
    	// We may have already wrapped this as a zip stream
    	ZipOutputStream zipStream = (outputStream instanceof ZipOutputStream) ? (ZipOutputStream) outputStream
    			                                                              : new ZipOutputStream(outputStream);

 
        for (File file : files) {
            zipStream.putNextEntry(new ZipEntry( getNormalizedRelativePath(file, zipRootDirs) ) );
            copyStream(new FileInputStream(file), zipStream);
            zipStream.closeEntry();
        }
                
        zipStream.close();
    }
    
    /**
     * 
     * @return the path-normalized result of getRelativePath
     */
    protected static String getNormalizedRelativePath(File file, List<File> rootDirs) throws IOException {
    	for (File rootDir : rootDirs) {   		
    		if (file.getCanonicalPath().contains(rootDir.getCanonicalPath())) {
    			return StringUtils.normalizePath( getRelativePath(file, rootDir) );
    		}
    	}
    	// Should we throw something here?
    	return "";
    }
    
    /**
     * 
     * Note: the method assumes rootDir has a non-null parent dir.
     * 
     * @return the relative path of the given file, relative to the rootDir.
     *         E.g: if file="/some/path/to/this/file.txt",
     *              and rootDir="/some/path",
     *              then this method returns "path/to/this/file.txt"
     *         
     */
    protected static String getRelativePath(File file, File rootDir) throws IOException {
        return StringUtils.trimPrefix( file.getCanonicalPath(), rootDir.getParentFile().getCanonicalPath() + File.separator) ;
    }

    /**
     * Copy the given InputStream to the given OutputStream.
     * 
     * Note: the InputStream is closed when the copy is complete.  The OutputStream 
     *       is left open.
     */
    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte buffer[] = new byte[2048];
        int bytesRead;
        while ((bytesRead = from.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead);
        }
        from.close();
    }

    /**
     * Open each file and write it to the given output stream.
     * 
     * Between each file, write header/footer records that indicates the file name.
     * The file name is given as a relative path between the canonical name of the file
     * and the canonical name of the given root dir.  Headers/footers are written
     * with UTF-8 encoding.
     * 
     */
    public static void aggregateFilesToStream(List<File> files,
                                              List<File> rootDirs, 
                                              OutputStream outputStream) throws IOException {
        
        for (File file : files) {
            outputStream.write( buildAggregateHeader(file, rootDirs).getBytes(StandardCharsets.UTF_8) );
            copyStream( new FileInputStream(file), outputStream );
            outputStream.write( buildAggregateFooter(file, rootDirs).getBytes(StandardCharsets.UTF_8) );
        }

    }
    
    public static void aggregateFilesToStream(List<File> files,
    										  File rootDir,
    										  OutputStream outputStream) throws IOException {
    	aggregateFilesToStream(files, Arrays.asList(rootDir), outputStream);
    }
    
    protected static String buildAggregateHeader(File file, List<File> rootDirs) throws IOException {
        return "xxxxx Begin file: " + getNormalizedRelativePath(file, rootDirs)  + " xxxxxxxxxxxxxxxxxx\n";
    }
    
    protected static String buildAggregateFooter(File file, List<File> rootDirs) throws IOException {
        return "\nxxxxx End file: " + getNormalizedRelativePath(file, rootDirs)  + " xxxxxxxxxxxxxxxxxx\n";
    }
    
    // Copy the ZipEntries of the input stream to entries in the output stream.
    public static void copyZipEntries(ZipInputStream zipInput, ZipOutputStream zipOutput) throws IOException {
    	byte[] buf = new byte[2048];
    	ZipEntry entry;
    	while ((entry = zipInput.getNextEntry()) != null) {
    		zipOutput.putNextEntry(entry);
    		int len = 0;
    		while ((len = zipInput.read(buf)) > 0) {
    			zipOutput.write(buf, 0, len);
    		}
    		zipOutput.closeEntry();
    	}
    }
    
}
