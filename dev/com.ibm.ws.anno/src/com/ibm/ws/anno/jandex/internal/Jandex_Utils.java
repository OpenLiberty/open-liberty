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
package com.ibm.ws.anno.jandex.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

/**
 * Jandex utility function.
 * 
 * This class provides a gateway to jandex public APIs.
 */
public class Jandex_Utils {

    public static Indexer createIndexer() {
        return new Indexer();
    }

    public static void updateIndexer(
        Indexer outputIndexer,
        String classPath,
        InputStream classInputStream) throws IOException {

        outputIndexer.index(classInputStream); // throws IOException 
    }

    public static Index completeIndexer(Indexer indexer) {
        return indexer.complete();
    }

    //

    public static Index readIndex(String indexPath) {
        // TODO:  should this be a warning/error message?
        try {
            return basicReadIndex(indexPath); // throws Exception
        } catch ( Exception e ) {
            throw new RuntimeException("Failed to read index [ " + indexPath + " ]", e);
        }
    }

    public static Index basicReadIndex(String indexPath) throws Exception {
        FileInputStream indexInputStream =
            new FileInputStream(indexPath); // throws FileNotFoundException
        try {
            return Jandex_Utils.basicReadIndex(indexInputStream); // throws IOException
        } finally {
            indexInputStream.close(); // throws IOException
        }
    }

    public static Index basicReadIndex(InputStream indexInputStream) throws IOException {
        IndexReader indexReader = new IndexReader(indexInputStream);
        return indexReader.read(); // throws IOException
    }

    public static LimitedIndex basicReadLimitedIndex(InputStream input) throws IOException{
        LimitedIndexReader index = new LimitedIndexReader(input);
        return index.read();
    }

    public static void writeIndex(String indexOutputPath, Index index) {
        // TODO:  should this be a warning/error message?
        try {
            basicWriteIndex(indexOutputPath, index); // throws Exception
        } catch ( Exception e ) {
            throw new RuntimeException("Failed to write index [ " + indexOutputPath + " ]");
        }
    }

    public static void basicWriteIndex(String indexOutputPath, Index index) throws Exception {
        FileOutputStream indexOutputStream =
            new FileOutputStream(indexOutputPath); // throws FileNotFoundException
        try {
            Jandex_Utils.basicWriteIndex(indexOutputStream, index, "Write [ " + indexOutputPath+ " ]");
            // throws IOException
        } finally {
            indexOutputStream.close(); // throws IOException
        }
    }

    public static void basicWriteIndex(OutputStream indexOutputStream, Index index, String writeCase)
        throws IOException {

        IndexWriter indexWriter = new IndexWriter(indexOutputStream);
        indexWriter.write(index);
    }

    //

    public static Index createIndex(String archiveSourcePath) {
       // TODO:  should these warning/error messages?
        
        File pathFile = new File(archiveSourcePath);
        if ( !pathFile.exists() ) {
            throw new IllegalArgumentException("Target [ " + archiveSourcePath + " ] does not exist");
        } else if ( pathFile.isDirectory() ) {
            throw new IllegalArgumentException("Target [ " + archiveSourcePath + " ] is a directory");
        }

        Indexer indexer = createIndexer();

        FileInputStream pathInputStream;
        try {
            pathInputStream = new FileInputStream(archiveSourcePath);
        } catch ( IOException e ) {
            throw new RuntimeException("Failed to open [ " + archiveSourcePath + " ]", e);
        }

        try {
            ZipInputStream zipInputStream = new ZipInputStream(pathInputStream);

            ZipEntry zipEntry;
            while ( (zipEntry = zipInputStream.getNextEntry()) != null ) {
                if ( zipEntry.isDirectory() ) {
                    continue;
                }

                String zipEntryName = zipEntry.getName();
                if ( !zipEntryName.endsWith(".class") ) {
                    continue;
                }

                updateIndexer(indexer, zipEntry.getName(), zipInputStream);
            }

        } catch ( Exception e ) {
            throw new RuntimeException("Failed to process [ " + archiveSourcePath + " ]", e);

        } finally {
            try {
                pathInputStream.close();
            } catch ( IOException e ) {
                throw new RuntimeException("Failed to open [ " + archiveSourcePath + " ]", e);
            }
        }

        return completeIndexer(indexer);
    }
}