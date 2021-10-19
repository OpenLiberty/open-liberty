package com.ibm.ws.annocache.test.jandex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.junit.Test;

import com.ibm.ws.annocache.jandex.internal.SparseClassInfo;
import com.ibm.ws.annocache.jandex.internal.SparseIndex;
import com.ibm.ws.annocache.jandex.internal.SparseIndexReader;

import junit.framework.Assert;

public class JandexSparseReadTest {

	public static final String SAMPLE_PACKAGE_NAME =
		"com.ibm.ws.anno.test.data.jandex";
	
	public static final String[] SAMPLE_SIMPLE_CLASS_NAMES = {
		"AnnoChildWithDefault",
		"AnnoChildWithoutDefault",
		"AnnoParentWithDefault",
		"AnnoParentWithoutDefault",
		"AnnoTarget"
	};

	public static final String[] SAMPLE_CLASS_NAMES; 
	public static final String[] SAMPLE_RESOURCE_NAMES;

	static {
		String classPrefix = SAMPLE_PACKAGE_NAME + '.';
		String resourcePrefix = SAMPLE_PACKAGE_NAME.replace('.', '/') + '/';

		String[] classNames = new String[ SAMPLE_SIMPLE_CLASS_NAMES.length ];
		String[] resourceNames = new String[ SAMPLE_SIMPLE_CLASS_NAMES.length ];

		for ( int sampleNo = 0; sampleNo < SAMPLE_SIMPLE_CLASS_NAMES.length; sampleNo++ ) {
			String simpleName = SAMPLE_SIMPLE_CLASS_NAMES[sampleNo];
			classNames[sampleNo] = classPrefix + simpleName;
			resourceNames[sampleNo] = resourcePrefix + simpleName + ".class";
		}

		SAMPLE_CLASS_NAMES = classNames;
		SAMPLE_RESOURCE_NAMES = resourceNames;
	}

	//

	@Test
	public void testReadsVersion6() throws IOException {
		testReads(SAMPLE_CLASS_NAMES, SAMPLE_RESOURCE_NAMES, 6); // throws IOException
	}

	@Test
	public void testReadsVersion7() throws IOException {
		testReads(SAMPLE_CLASS_NAMES, SAMPLE_RESOURCE_NAMES, 7); // throws IOException
	}

	@Test
	public void testReadsVersion8() throws IOException {
		testReads(SAMPLE_CLASS_NAMES, SAMPLE_RESOURCE_NAMES, 8); // throws IOException
	}

	@Test
	public void testReadsVersion9() throws IOException {
		testReads(SAMPLE_CLASS_NAMES, SAMPLE_RESOURCE_NAMES, 9); // throws IOException
	}
	
	@Test
	public void testReadsVersion10() throws IOException {
		testReads(SAMPLE_CLASS_NAMES, SAMPLE_RESOURCE_NAMES, 10); // throws IOException
	}

	//

	public Indexer createIndexer() {
		return new Indexer();
	}

	public IndexWriter createWriter(OutputStream output) {
		return new IndexWriter(output);
	}
	
	public IndexReader createReader(InputStream input) {
		return new IndexReader(input);
	}

	public SparseIndexReader createSparseReader(InputStream input) throws IOException {
		return new SparseIndexReader(input); // throws IOException
	}

	//

	public InputStream openResource(String resource) throws IOException {
		InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resource);
		if ( resourceStream == null ) {
			throw new IOException("Failed to open resource [ " + resource + " ]");
		}
		return resourceStream;
	}

	public void add(String resource, Indexer indexer) throws IOException {
		InputStream resourceStream = openResource(resource); // throws IOException
		try {
			indexer.index(resourceStream); // throws IOException
		} finally {
			resourceStream.close(); // throws IOException
		}
	}
	
	public Index index(String[] resources) throws IOException {
		Indexer indexer = createIndexer();
		for ( String resource : resources ) {
			add(resource, indexer); // throws IOException
		}
		return indexer.complete();
	}

	public byte[] write(Index index, int version) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(8 * 1024);

		IndexWriter writer = createWriter(byteStream);
		@SuppressWarnings("unused")

		int bytesWritten = writer.write(index, version); // throws IOException

		return byteStream.toByteArray();
	}

	public byte[] createRawIndex(String[] resources, int version) throws IOException {
		Indexer indexer = createIndexer();
		for ( String resource : resources ) {
			add(resource, indexer); // throws IOException
		}
		Index index = indexer.complete();

		byte[] rawIndex = write(index, version); // throws IOException
		return rawIndex;
	}

	public Index readIndex(byte[] rawIndex) throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(rawIndex);
		IndexReader reader = createReader(input);
		Index index = reader.read(); // throws IOException
		return index;
	}

	public SparseIndex readSparseIndex(byte[] rawIndex) throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(rawIndex);
		SparseIndexReader reader = createSparseReader(input);
		return reader.getIndex();
	}

	public void testReads(String[] classNames, String[] resources, int version) throws IOException {
		System.out.println("Testing reads; version [ " + version + " ]");
		for ( String className : classNames ) {
			System.out.println("  [ " + className + " ]");
		}

		System.out.println("Generating, writing, and reading indexes");

		byte[] rawIndex = createRawIndex(resources, version); // throws IOException
		Index index = readIndex(rawIndex); // throws IOException
		SparseIndex sparseIndex = readSparseIndex(rawIndex); // throws IOException

		System.out.println("Validating reads");

		boolean valid = validate(classNames, index, sparseIndex);
		System.out.println( "Testing reads; version [ " + version + " ]: " + (valid ? "PASS" : "FAIL") );
		Assert.assertTrue("Incorrect read", valid); 
	}

	public boolean validate(String[] classNames, Index fullFndex, SparseIndex sparseIndex) {
		Set<String> expectedNames = new HashSet<String>(classNames.length);
		for ( String className : classNames ) {
			expectedNames.add(className);
		}

		Set<String> actualFullNames = new HashSet<String>(classNames.length);
		Collection<ClassInfo> indexClasses = fullFndex.getKnownClasses();
		for ( ClassInfo classInfo : indexClasses ) {
			actualFullNames.add( classInfo.name().toString() );
		}

		Set<String> actualSparseNames = new HashSet<String>(classNames.length);
		Collection<? extends SparseClassInfo> sparseClasses = sparseIndex.getKnownClasses();
		for ( SparseClassInfo classInfo : sparseClasses ) {
			actualSparseNames.add( classInfo.name().toString() );
		}

		int missingFull = 0;
		int extraFull = 0;

		int missingSparse = 0;
		int extraSparse = 0;

		for ( String expectedName : expectedNames ) {
			if ( !actualFullNames.contains(expectedName) ) {
				System.out.println("Full read missing [ " + expectedName + " ]");
				missingFull++;
			}
			if ( !actualSparseNames.contains(expectedName) ) {
				System.out.println("Sparse read missing [ " + expectedName + " ]");
				missingSparse++;
			}
		}

		for ( String fullName : actualFullNames ) {
			if ( !expectedNames.contains(fullName) ) {
				System.out.println("Full read added [ " + fullName + " ]");
				extraFull++;
			}
		}

		for ( String sparseName : actualSparseNames ) {
			if ( !expectedNames.contains(sparseName) ) {
				System.out.println("Sparse read added [ " + sparseName + " ]");
				extraSparse++;
			}
		}

		return ( (missingFull == 0) && (extraFull == 0) && (missingSparse == 0) && (extraSparse == 0) );
	}
}
