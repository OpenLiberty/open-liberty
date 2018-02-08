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

public class Jandex_Utils {

    /**
     * Create and return a new Jandex indexer.
     *
     * @return A new Jandex indexer.
     */
    public static Indexer createIndexer() {
        return new Indexer();
    }

    /**
     * Update an indexer with data for a class.
     *
     * @param indexer The indexer to update with class data.
     * @param classPath The path to the class resource which provides the class data.
     * @param classStream The input stream of the class resource.
     *
     * @throws IOException Thrown if class data cannot be read from the stream.
     */
    public static void updateIndexer(Indexer indexer, String classPath, InputStream classStream)
        throws IOException {

        indexer.index(classStream); // throws IOException 
    }

    /**
     * Complete a Jandex indexer.
     *
     * @param indexer The indexer which is to be completed.
     */
    public static Index completeIndexer(Indexer indexer) {
        return indexer.complete();
    }

    //

    /**
     * Write a Jandex index to an output stream.
     *
     * @param outputStream The stream to which to write the index.
     * @param index The index which is to be written to the stream.
     *
     * @throws IOException Thrown if the write failed.
     */
    public static void basicWriteIndex(OutputStream outputStream, Index index)
        throws IOException {

        IndexWriter indexWriter = new IndexWriter(outputStream);
        indexWriter.write(index); // throws IOException
    }

    /**
     * Read a Jandex index from an input stream.
     *
     * @param inputStream The stream from which to read the index.
     *
     * *return The index which was read from the stream.
     *
     * @throws IOException Thrown if the write fails.
     */
    public static Index basicReadIndex(InputStream inputStream)
        throws IOException {

        IndexReader indexReader = new IndexReader(inputStream);
        return indexReader.read(); // throws IOException
    }

    //

    /**
     * Write an index to a specified file.  Truncate the file if it
     * already exists.
     *
     * @param indexFilePath The path to which to write the index.
     * @param index The index which is to be written.
     *
     * @throws IOException Thrown if the write fails.
     */
    public static void writeIndex(String indexFilePath, Index index) throws IOException {
        FileOutputStream outputStream =
            new FileOutputStream(indexFilePath); // throws FileNotFoundException
        try {
            basicWriteIndex(outputStream, index); // throws IOException
        } finally {
            outputStream.close(); // throws IOException
        }
    }

    /**
     * Read an index from a specified file.
     *
     * @param indexFilePath The path from which to read the index.
     *
     * @return The index which was read.
     *
     * @throws IOException Thrown if the write fails.
     */
    public static Index readIndex(String indexFilePath) throws IOException {
        FileInputStream inputStream =
            new FileInputStream(indexFilePath); // throws FileNotFoundException
        try {
            return basicReadIndex(inputStream); // throws IOException
        } finally {
            inputStream.close(); // throws IOException
        }
    }

    //

    /**
     * Create a Jandex index from a specified JAR file.
     *
     * The specified file must exist and must be a JAR file.
     *
     * A new Jandex index is created.  All classes of the specified JAR are
     * added to the index.  The index is completed and returned.
     *
     * @param path The path to the JAR file which is to be read.
     *
     * @return The index which was created from the JAR file.
     *
     * @throws IOException Thrown if any step of creating the index fails.
     */
    public static Index createIndex(String path) throws IOException {
        // TODO: NLS Enable

        File pathFile = new File(path);
        if ( !pathFile.exists() ) {
            throw new IOException("Target [ " + path + " ] does not exist");
        } else if ( pathFile.isDirectory() ) {
            throw new IOException("Target [ " + path + " ] is a directory");
        }

        Indexer indexer = createIndexer();

        FileInputStream pathInputStream = new FileInputStream(path); // throws IOExcepion

        try {
            ZipInputStream zipInputStream = new ZipInputStream(pathInputStream); // throws IOException

            ZipEntry zipEntry;
            while ( (zipEntry = zipInputStream.getNextEntry()) != null ) { // throws IOException
                if ( zipEntry.isDirectory() ) {
                    continue;
                }

                String zipEntryName = zipEntry.getName();
                if ( !zipEntryName.endsWith(".class") ) {
                    continue;
                }

                updateIndexer(indexer, zipEntry.getName(), zipInputStream); // throws IOException
            }

        } finally {
            pathInputStream.close(); // throws IOException
        }

        return completeIndexer(indexer);
    }
}
