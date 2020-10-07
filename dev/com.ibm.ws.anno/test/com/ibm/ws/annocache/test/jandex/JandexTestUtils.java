package com.ibm.ws.annocache.test.jandex;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;

import com.ibm.ws.annocache.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;

public class JandexTestUtils {

    public static final long NANOS_IN_MICRO = 1000;
    public static final long NANOS_IN_MILLI = 1000 * 1000;

    public static final int BYTES_IN_KILOBYTE = 1024;
    public static final int BYTES_IN_MEGABYTE = 1024 * 1024;

	public static FileInputStream openStream(String indexPath) {
		try {
			return new FileInputStream(indexPath); // throws FileNotFoundException
		} catch ( FileNotFoundException e ) {
			Assert.fail("Failed to open [ " + indexPath + " ]: " + e.getMessage());
			return null;
		}
	}

	public static void closeStream(String indexPath, InputStream inputStream) {
        try {
            inputStream.close(); // throws IOException
        } catch ( IOException e ) {
            Assert.fail("Failed to close [ " + indexPath + " ]: " + e.getMessage());
        }
	}

	public static byte[] asBytes(String indexPath) {
        InputStream inputStream = openStream(indexPath);

        byte[] bytes;
        try {
        	bytes = asBytes(indexPath, inputStream);
        } finally {
        	closeStream(indexPath, inputStream);
        }

        return bytes;
	}

	public static byte[] asBytes(String indexPath, InputStream inputStream) {
		try {
			ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

			byte[] transferBuffer = new byte[ 16 * 1024 ];
			int bytesRead;
			while ( (bytesRead = inputStream.read(transferBuffer)) != -1 ) { // throws IOException
				byteOutput.write(transferBuffer, 0, bytesRead); // throws IOException
			}

			return byteOutput.toByteArray();

		} catch ( IOException e ) {
            Assert.fail("Failed to read [ " + indexPath + " ]: " + e.getMessage());
            return null;
		}
	}

    public static org.jboss.jandex.Index readFullIndex(String indexPath) {
        InputStream inputStream = openStream(indexPath);

        org.jboss.jandex.Index fullIndex;
        try {
        	fullIndex = readFullIndex(indexPath, inputStream);
        } finally {
        	closeStream(indexPath, inputStream);
        }
        return fullIndex;
    }

    public static org.jboss.jandex.Index readFullIndex(String indexPath, InputStream inputStream) {
        try {
        	return Jandex_Utils.basicReadIndex(inputStream); // throws IOException
        } catch ( IOException e ) {
            Assert.fail("Failed to read [ " + indexPath + " ]: " + e.getMessage());
            return null;
        }
    }

    public static SparseIndex readSparseIndex(String indexPath) {
        InputStream inputStream = openStream(indexPath);

        SparseIndex sparseIndex;
        try {
        	sparseIndex = readSparseIndex(indexPath, inputStream);
        } finally {
        	closeStream(indexPath, inputStream);
        }
        return sparseIndex;
    }

    public static SparseIndex readSparseIndex(String indexPath, InputStream inputStream) {
        try {
            return Jandex_Utils.basicReadSparseIndex(inputStream); // throws IOException
        } catch ( IOException e ) {
            Assert.fail("Failed to read [ " + indexPath + " ]: " + e.getMessage());
            return null;
        }
    }
}
