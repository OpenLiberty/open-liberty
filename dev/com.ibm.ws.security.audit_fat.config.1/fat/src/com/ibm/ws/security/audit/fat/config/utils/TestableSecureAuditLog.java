package com.ibm.ws.security.audit.fat.config.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides file based secure (signed and/or encrypted) audit log for determining characteristics of the log.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class TestableSecureAuditLog extends TestableAuditLog {

	protected static String ENCRYPTION_SECTION ="EncryptionInformation";
	protected static String SIGNING_SECTION ="SigningInformation";

	protected static String HEADER_ENCRYPTED_SHARED_KEY = "encryptedSharedKey";
	protected static String HEADER_ENCRYPTION_CERT_ALIAS = "encryptionCertAlias";
	protected static String HEADER_ENCRYPTION_KEYSTORE = "encryptionKeyStore";
	protected static String HEADER_ENCRYPTION_CERTIFICATE = "encryptionCertificate";

	protected static String HEADER_SIGNING_SHARED_KEY = "signingSharedKey";
	protected static String HEADER_SIGNING_CERT_ALIAS = "signingCertAlias";
	protected static String HEADER_SIGNING_KEYSTORE = "signingKeyStore";
	protected static String HEADER_SIGNING_CERTIFICATE = "signingCertificate";

	protected static String AUDIT_RECORD = "auditRecord";
	
	public TestableSecureAuditLog(String logName) {
		super(logName);
	}
	
	/**
	 * Given the name of the header section, return a list of the strings from that header.
	 * 
	 * @param header - name of section header
	 *
	 * @return - list of strings within the header or null if the header section is not found.
	 */
	public List<String> getHeader(String header) throws Exception {
		return readSecureLogHeader(logFile.toString(), header);
	}

	/**
	 * Assert that the specified header is present in the TestableAuditLog file.
	 * 
	 * @param header - name of section header
	 */
	public void ensureHeaderFound(String header) throws Exception {
		assertNotNull("Header section " + header + " not found in log as expected.", getHeader(header));
	}

	/**
	 * Assert that the specified header is NOT present in the TestableAuditLog file.
	 * 
	 * @param header - name of section header
	 */
	public void ensureHeaderNotFound(String header) throws Exception {
		assertNull("Header section " + header + " found in log when not expected.", getHeader(header));
	}

	/**
	 * Assert the following values are contained within the encryption header in the TestableAuditLog:
	 *  certificateAlias
	 *  encryptionKeyStore
	 *  encryptionCertificate
	 * 
	 * @param header - name of section header - <EncryptionInformation>
	 * @param certAlias - expected name of the <encryptionCertAlias>
	 * @param keyStore- name of the <encryptionKeyStore>
	 * @param algorithm- name of the algorithm in <encryptionCertificate>
	 */
	public void verifyEncryptionHeaderContents(String header, String certAlias, String keyStore, String algorithm)
			throws Exception {
		Map<String, String> found = parseValues(getHeader(header), HEADER_ENCRYPTED_SHARED_KEY,
				HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
		assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
		assertTrue(
				"Expected encryption cert alias of " + certAlias + " not found. Instead found: "
						+ found.get(HEADER_ENCRYPTION_CERT_ALIAS),
				found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(certAlias));
		assertTrue(
				"Expected encryption keystore of " + keyStore + " not found.  Instead found: "
						+ found.get(HEADER_ENCRYPTION_KEYSTORE),
				found.get(HEADER_ENCRYPTION_KEYSTORE).contains(keyStore));
		assertTrue(
				"Expected encryption certificate algorithm " + algorithm + " not found.  Instead found: "
						+ found.get(HEADER_ENCRYPTION_CERTIFICATE),
				found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(algorithm));
	}

	/**
	 * Assert the following values are contained within the encryption header in the TestableAuditLog:
	 *  certificateAlias
	 *  signingnKeyStore
	 *  signingCertificate
	 * 
	 * @param header - name of section header - <SigningInformation>
	 * @param certAlias - expected name of the <signingCertAlias>
	 * @param keyStore- name of the <signingKeyStore>
	 * @param algorithm- name of the algorithm in <signingCertificate>
	 */
	public void verifySigningHeaderContents(String header, String certAlias, String keyStore, String algorithm)
			throws Exception {
		Map<String, String> found = parseValues(getHeader(header), HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS,
				HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
		assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
		assertTrue(
				"Expected signing cert alias of " + certAlias + " not found. Instead found: "
						+ found.get(HEADER_SIGNING_CERT_ALIAS),
				found.get(HEADER_SIGNING_CERT_ALIAS).contains(certAlias));
		assertTrue("Expected signing keystore of " + keyStore + " not found.  Instead found: "
				+ found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(keyStore));
		assertTrue(
				"Expected signing certificate algorithm " + algorithm + " not found.  Instead found: "
						+ found.get(HEADER_SIGNING_CERTIFICATE),
				found.get(HEADER_SIGNING_CERTIFICATE).contains(algorithm));
	}

	/**
	 * Given the a section name, count the number of sections in the TestableAuditLog
	 * 
	 * @param header - name of section 
	 *
	 * @return - map with section name and number of times it occurred in the log
	 */
	public Map<String, Integer> getSectionCount(String section) throws Exception {
		Map<String, Integer> counts = countSections(logFile.toString(), section);
		return counts;
	}


	/**
	 * Assert that auditRecord entries are present in the TestableAuditLog
	 * Note that an exception will be thrown on getSectionCount if audit records are mismatched without beginning or end.
	 */
	public void ensureLogContainsCompleteAuditRecords() throws Exception {
		assertTrue("Expected multiple auditRecords in audit log but did not find complete set when counting them.",
				!getSectionCount(AUDIT_RECORD).isEmpty());
	}

	/**
	 * Assert that the expected number of audit records are found in the TestableAuditLog
	 * 
	 * @param expectedCount - expected number of auditRecords 
	 *
	 */
	public void verifyAuditRecordCount(int expectedCount) throws Exception {
		 Map <String, Integer> counts = getSectionCount(AUDIT_RECORD);
		assertEquals("Expected " + expectedCount + " auditRecords in the audit log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), expectedCount, counts.get(AUDIT_RECORD).intValue()); 
	}	
	
	/**
	 * Reads lines from the header section of the secure (encrypted and/or
	 * signed) audit log until the section end for the header.
	 * 
	 * @return the list of strings from the header if section is found; return
	 *         null if not found
	 * @throws IOException
	 */
	protected static List<String> readSecureLogHeader(String logName, String section) throws IOException {

		List<String> results = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(logName));
		String line;
		StringBuilder buildLine = new StringBuilder();
		String sectionStart = "<" + section;
		String sectionEnd = "</" + section;
		boolean hasXMLStart = false;
		System.out.println("Searching for section " + section + " in audit log header.");
		try {
			while ((line = br.readLine()) != null) {

				if (line.contains(sectionStart))
					hasXMLStart = true;

				if (hasXMLStart)
					results.add(line);

				if (line.contains(sectionEnd)) {
					System.out.println("Found section " + section + " contents: " + results.toString());
					return results;
				}

			}
			System.out.println("Section " + section + " not found.");
			return null;
		} finally {
			try {
				br.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

	/**
	 * Reads lines from the secure audit log and counts the number of
	 * sections which have a specific section name.
	 * 
	 * @return a Map with the count for each section name
	 * @throws IOException
	 */
	protected Map<String, Integer> countSections(String logName, String... keys) throws IOException {

		Map<String, Integer> results = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(logName));
		String line;
		Stack<String> s = new Stack<String>();

		for (String key : keys)
			results.put(key, 0);

		Pattern startRx = Pattern.compile("^\\s*<([a-zA-Z]+?)>.*$");
		Pattern endRx = Pattern.compile("^.*</([a-zA-Z]+?)>.*$");

		try {
			while ((line = br.readLine()) != null) {

				Matcher startM = startRx.matcher(line);
				Matcher endM = endRx.matcher(line);

				boolean startMatches = startM.matches();
				boolean endMatches = endM.matches();

				String startKey = startMatches ? startM.group(1) : null;
				String endKey = endMatches ? endM.group(1) : null;

				if (startMatches && endMatches) {

					if (!startKey.equals(endKey))
						throw new RuntimeException("Mismatched section elememts: " + startKey + ", " + endKey);
					if (results.containsKey(startKey)) {
						int current = results.get(startKey);
						current++;
						results.put(startKey, current);
					}
				} else if (startMatches && !endMatches) {

					s.push(startKey);
				} else if (!startMatches && endMatches) {
					if (!s.isEmpty()) {
						String saved = s.pop();
						if (saved != null && saved.equals(endKey)) {
							if (results.containsKey(endKey)) {
								int current = results.get(endKey);
								current++;
								results.put(endKey, current);
							}
						}
					} else {
						throw new RuntimeException("Mismatched section elements for: " + endKey);
					}
				}
			}
		} finally {
			try {
				br.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		if (results != null)
			System.out.println("Complete records found : " + results.toString());
		return results;
	}

	/**
	 * Reads lines from the header section of the secure audit log and capture the
	 * values for each of the keys specified.
	 * 
	 * @return Map <key, value>
	 */
	protected Map<String, String> parseValues(List<String> header, String... keys) {
		Map<String, String> results = new HashMap<String, String>();
		for (String key : keys)
			results.put(key, null);
		for (String line : header) {
			if (line != null && !line.trim().isEmpty()) {
				for (String key : results.keySet()) {
					if (results.get(key) == null) {
						if (line.contains(key)) {
							String result = line.replace("<" + key + ">", "").replace("</" + key + ">", "");
							results.put(key, result);
							break;
						}
					}
				}
			}
		}
		return results;
	}
}
