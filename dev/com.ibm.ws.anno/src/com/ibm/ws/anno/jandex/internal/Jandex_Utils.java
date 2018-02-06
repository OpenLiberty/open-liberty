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

    public static Indexer createIndexer() {
        return new Indexer();
    }

    public static void updateIndexer(Indexer indexer, String classPath, InputStream classStream) throws IOException {
        indexer.index(classStream); // throws IOException 
    }

    public static Index completeIndexer(Indexer indexer) {
        return indexer.complete();
    }

    //

    public static Index readIndex(String jarPath) {
        // TODO:  should this be a warning/error message?
        try {
            return basicReadIndex(jarPath); // throws Exception
        } catch ( Exception e ) {
            throw new RuntimeException("Failed to read index [ " + jarPath + " ]");
        }
    }

    public static Index basicReadIndex(String jarPath) throws Exception {
        FileInputStream inputStream =
            new FileInputStream(jarPath); // throws FileNotFoundException
        try {
            return Jandex_Utils.basicReadIndex(inputStream); // throws IOException
        } finally {
            inputStream.close(); // throws IOException
        }
    }

    public static Index basicReadIndex(InputStream inputStream) throws IOException {
        IndexReader indexReader = new IndexReader(inputStream);
        return indexReader.read(); // throws IOException
    }

    public static void writeIndex(String jarPath, Index index) {
        // TODO:  should this be a warning/error message?
        try {
            basicWriteIndex(jarPath, index); // throws Exception
        } catch ( Exception e ) {
            throw new RuntimeException("Failed to write index [ " + jarPath + " ]");
        }
    }

    public static void basicWriteIndex(String jarPath, Index index) throws Exception {
        FileOutputStream outputStream =
            new FileOutputStream(jarPath); // throws FileNotFoundException
        try {
            Jandex_Utils.basicWriteIndex(outputStream, index, "Write [ " + jarPath+ " ]");
            // throws IOException
        } finally {
            outputStream.close(); // throws IOException
        }
    }

    public static void basicWriteIndex(OutputStream outputStream, Index index, String writeCase)
        throws IOException {

        IndexWriter indexWriter = new IndexWriter(outputStream);
        indexWriter.write(index);
    }

    //

    public static Index createIndex(String path) {
     // TODO:  should these warning/error messages?
        
        File pathFile = new File(path);
        if ( !pathFile.exists() ) {
            throw new IllegalArgumentException("Target [ " + path + " ] does not exist");
        } else if ( pathFile.isDirectory() ) {
            throw new IllegalArgumentException("Target [ " + path + " ] is a directory");
        }

        Indexer indexer = createIndexer();

        FileInputStream pathInputStream;
        try {
            pathInputStream = new FileInputStream(path);
        } catch ( IOException e ) {
            throw new RuntimeException("Failed to open [ " + path + " ]", e);
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
            throw new RuntimeException("Failed to process [ " + path + " ]", e);

        } finally {
            try {
                pathInputStream.close();
            } catch ( IOException e ) {
                throw new RuntimeException("Failed to open [ " + path + " ]", e);
            }
        }

        return completeIndexer(indexer);
    }
}
