/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class PackageLooseConfigTest extends AbstractPackageTest {
	private static final String[] CONFIGS = new String[] { 
			"NestedArchive.war.xml", 
			"SingleDir.war.xml",
			"SingleFile.war.xml",
			"SkipInvalidEntries.war.xml",
			"FullArchive.war.xml", 
			};

	@Before
	public void before() throws Exception {

		// Delete previous archive file if it exists
		LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
		server.deleteFileFromLibertyServerRoot(ARCHIVE_PACKAGE);
	}

	/**
	 * This tests that when --server-root is supplied, and you have a loose
	 * application that the loose application is output in the archive as a .war
	 * file as well as in an expanded format in the expanded folder
	 *
	 */
	@Test
	public void testExpandedAndArchiveCreatedFromLooseConfig() throws Exception {
		LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
		try {
			packageAndTestConfigs(new ExpandedAndArchiveCreatedFromLooseConfigTest(), server, CONFIGS);
		} catch (FileNotFoundException ex) {
			assumeTrue(false); // the directory does not exist, so we skip this test.
		}
	}

	private class ExpandedAndArchiveCreatedFromLooseConfigTest implements AbstractPackageTest.LooseConfigTest {
		@Override
		public void testConfig(LibertyServer server, String config) throws IOException {
			// Remove .xml from config file name to get loose config archive name
			String archive = config.substring(0, config.lastIndexOf('.'));

			ZipFile zipFile = new ZipFile(server.getServerRoot() + "/" + ARCHIVE_PACKAGE);
			try {
				boolean foundServerEntry = false;
				boolean foundWarFileEntry = false;
				boolean foundExpandedEntry = false;
				for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements();) {
					ZipEntry entry = en.nextElement();
					// Uses String.matches() to ensure exact path is found
					foundServerEntry |= entry.getName().matches("^" + SERVER_PATH + "/$");
					foundWarFileEntry |= entry.getName().matches("^" + SERVER_PATH + "/apps/" + archive + "$");
					foundExpandedEntry |= entry.getName()
							.matches("^" + SERVER_PATH + "/apps/expanded/" + archive + "/.*$");
				}
				assertTrue("The package did not contain " + SERVER_PATH + "/ as expected.", foundServerEntry);
				assertTrue("The package did not contain " + SERVER_PATH + "/apps/" + archive + " as expected.",
						foundWarFileEntry);
				assertTrue(
						"The package did not contain " + SERVER_PATH + "/apps/expanded/" + archive + "/ as expected.",
						foundExpandedEntry);
			} finally {
				try {
					zipFile.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	/**
	 * Tests the contents of the .war and expanded folder created from packaging a loose application
	 * to ensure they contain the exact same files in the exact same order 
	 * 
	 */
	@Test
	public void testExpandedFolderMatchesArchiveFromLooseConfig() throws Exception {
		LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
		try {
			packageAndTestConfigs(new ExpandedFolderMatchesArchiveFromLooseConfigTest(), server, CONFIGS);
		} catch (FileNotFoundException ex) {
			assumeTrue(false); // the directory does not exist, so we skip this test.
		}
	}

	private class ExpandedFolderMatchesArchiveFromLooseConfigTest implements AbstractPackageTest.LooseConfigTest {

		@Override
		public void testConfig(LibertyServer server, String config) throws Exception {
			// Remove .xml from config file name to get loose config archive name
			String archive = config.substring(0, config.lastIndexOf('.'));
			
			// Map every entry found in both the .war and the expanded folder to
			// the relative order they were found. Add the values from the archive and
			// subtract the values from the expanded folder. 
			// Resulting values should all be 0.
			HashMap<String, Integer> checkMatch = new HashMap<>();
			int expandedOrder = -1;
			ZipFile packageZip = new ZipFile(server.getServerRoot() + "/" + ARCHIVE_PACKAGE);
			try {
				for (Enumeration<? extends ZipEntry> packageEn = packageZip.entries(); packageEn.hasMoreElements();) {
					ZipEntry packageEntry = packageEn.nextElement();

					// Found the entry for the .war in the .zip
					if (packageEntry.getName().matches("^" + SERVER_PATH + "/apps/" + archive + "$")) {
						// Read the contents of the compressed .war into warFile
						BufferedInputStream bis = new BufferedInputStream(packageZip.getInputStream(packageEntry));
						File warFile = new File(server.getServerRoot() + "/packagedWar");
						FileOutputStream warOutput = new FileOutputStream(warFile);
						while (bis.available() > 0) {
							warOutput.write(bis.read());
						}
						warOutput.close();

						// Add the index of each extracted entry from the .war into the hashmap using
						// their name as the key
						ZipFile warZip = new ZipFile(warFile);
						int warOrder = 1;
						for (Enumeration<? extends ZipEntry> warEn = warZip.entries(); warEn.hasMoreElements();) {
							String warEntry = warEn.nextElement().getName();
							putMatch(checkMatch, warEntry, warOrder++);

						}
						warZip.close();
					}

					// Subtract the index of each entry in the expanded folder from the hashmap
					// using their name as the key
					if (packageEntry.getName().matches("^" + SERVER_PATH + "/apps/expanded/" + archive + "/.+$")) {
						String expandedEntry = packageEntry.getName()
								.replaceFirst(SERVER_PATH + "/apps/expanded/" + archive + "/", "");

						putMatch(checkMatch, expandedEntry, expandedOrder--);
					}
				}

				// If the archive and the expanded folder contain the same files in the same
				// order then every entry will have a value of 0
				Iterator<Map.Entry<String, Integer>> matchSet = checkMatch.entrySet().iterator();
				while (matchSet.hasNext()) {
					Map.Entry<String, Integer> match = matchSet.next();
					assertTrue("The archive does not match the expanded folder at file: " + match.getKey(),
							match.getValue() == 0);
				}
			} finally {
				try {
					packageZip.close();
				} catch (IOException ex) {
				}
			}
		}

		/**
		 * Add the given value to the value found in checkMath at the given key if one exists,
		 * otherwise make a new entry
		 * @param checkMatch
		 * @param key
		 * @param value
		 */
		private void putMatch(HashMap<String, Integer> checkMatch, String key, int value) {
			int match = checkMatch.get(key) == null ? 0 : checkMatch.get(key);
			checkMatch.put(key, match + value);
		}

	}
}