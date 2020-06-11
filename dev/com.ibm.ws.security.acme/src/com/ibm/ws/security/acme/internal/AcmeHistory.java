/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.acme.AcmeCertificate;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * This class will create and update the initial acme file in the servers/workarea
 * directory. It holds certificate and directoryURI information, eg.
 * # Version 1.0
 * # Date                Serial                   DirectoryURI                    Account URI
 * # --------------------------------------------------------------------------------------------
 *  20200509231118      6542743894787011570      https://localhost:33827/dir     https://localhost:33827/my-account/1
 *  
 *  Non entry lines begin with # to differentiate them. This will make it easier
 *  to change the format of the file in the future.
 */
public class AcmeHistory {
	private static final TraceComponent tc = Tr.register(AcmeHistory.class);
	private String spaceDelim = "                  ";
	private final String acmeFileName = AcmeConstants.ACME_HISTORY_FILE;
	private final int FILE_EXISTS = 0;
	private final int FILE_CREATED = 1;
	private final int FILE_NOT_CREATED = 2;
	private File acmeFile;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
	private ArrayList<AcmeHistoryEntry> acmeHistoryEntries;
	private ArrayList<String> headers;
	
	/**
	 * This method determines if the directoryURI has changed by checking
	 * the acme file.
	 * @return True if the directoryURI has changed
	 */
	protected boolean directoryURIChanged(String currentDirectoryURI, WsLocationAdmin wslocation, boolean disableRenewOnNewHistory) {
		int acmefilecreation = createAcmeFile(wslocation);
		//For testing purposes, check for disableRenewOnNewHistory. For some tests, we need to make sure
		//the certificate does not refresh when starting a new server and the history file doesn't exist yet.
		if (acmefilecreation == FILE_CREATED && disableRenewOnNewHistory) {
			return false;
		}
		//If the ACME file doesn't exist or it failed to create, return true to force refresh.
		if (acmefilecreation > FILE_EXISTS) {
			return true;
		}
		File file = wslocation.getServerWorkareaResource("acmeca/" + acmeFileName).asFile();
		String fileDirURI = currentDirectoryURI;
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line, lastLine = null; 
			while ((line = br.readLine()) != null && !line.isEmpty()) {
				lastLine=line;
			}
			br.close();
			if (lastLine != null && !lastLine.isEmpty()) {
				StringTokenizer tok = new StringTokenizer(lastLine);
				if (tok.hasMoreTokens()) tok.nextToken();
				if (tok.hasMoreTokens()) tok.nextToken();
				if (tok.hasMoreTokens()) fileDirURI = tok.nextToken();
			}
		} catch (Exception e) {
			Tr.error(tc, "CWPKI2072W", file.getAbsolutePath(), e.getMessage());
			return true;
		}
		if (currentDirectoryURI.equals(fileDirURI)) {
			return false;
		}
		return true;
	}
	
	/**
	 * This method will create the initial acme file in the servers/workarea
	 * directory. It holds certificate and directoryURI information, eg.
	 * # Version 1.0
	 * # Date                Serial                   DirectoryURI                    Account URI
     * # --------------------------------------------------------------------------------------------
     *  20200509231118      6542743894787011570      https://localhost:33827/dir     https://localhost:33827/my-account/1
     *  
     *  @return 0 if the file already exists
     *          1 if the file was successfully created
     *          2 if the file was not created
	 */
	private int createAcmeFile(WsLocationAdmin wslocation) {
		acmeFile = wslocation.getServerWorkareaResource("acmeca/" + acmeFileName).asFile();
		if (acmeFile.exists()) return FILE_EXISTS;
		acmeHistoryEntries = new ArrayList<AcmeHistoryEntry>();
		headers = new ArrayList<String>();
		acmeFile.getParentFile().mkdirs();
		LocalDateTime now = LocalDateTime.now();  
		String date = FORMATTER.format(now);
		
		//Save headers
		headers.add("# WARNING!!! DO NOT MODIFY THIS FILE. IT HAS BEEN AUTO-GENERATED: " + date);
		headers.add("# Version 1.0");
		headers.add("# Date" + spaceDelim + "Serial" + spaceDelim + "DirectoryURI" + spaceDelim + "Account URI" + spaceDelim + "Expiration");
		headers.add("# -------------------------------------------------------------------------------------------------------------------------");
		
		try {
			acmeFile.createNewFile();
			FileWriter fr;
			fr = new FileWriter(acmeFile, false);
			for (String h: headers) {
				fr.write(h + "\n");
			}
	       	fr.close();
	       	return FILE_CREATED;
		} catch (IOException e) {
			Tr.error(tc, "CWPKI2072W", acmeFile.getAbsolutePath(), e.getMessage());
		}
		return FILE_NOT_CREATED;
	}

	/**
	 * Updates the historical ACME file.
	 * @param certificate The current X509Certificate.
	 * @param directoryURI The current directoryURI from config.
	 * @param accountURI The current accountURI from config.
	 */
	protected void updateAcmeFile(X509Certificate certificate, String directoryURI, String accountURI, WsLocationAdmin wslocation) {
		updateAcmeFile(null, certificate, directoryURI, accountURI, wslocation);
	}

	/**
	 * Update the acme file with the original or updated certificate
	 * and directoryURI information. Max file size is 12 lines (2 header lines and 10 entries).
	 * @param acmeCertificate The certificate to use to update the ACME file.
	 * @param certificate The X509Certificate to use if the AcmeCertificate is null.
	 * @param directoryURI The current directoryURI from config.
	 * @param accountURI The current accountURI from config.
	 */
	protected void updateAcmeFile(AcmeCertificate acmeCertificate, X509Certificate certificate, String directoryURI, String accountURI, WsLocationAdmin wslocation) {
		//If the file doesn't exist and failed to create, return.
		if (createAcmeFile(wslocation) == FILE_NOT_CREATED) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();  
		String date = FORMATTER.format(now);
		String serial = null, expirationDate = null;
		X509Certificate cert = certificate;
		if (acmeCertificate != null) {
			cert = acmeCertificate.getCertificate();
		}
		serial = cert.getSerialNumber().toString(16);
		expirationDate = df.format(cert.getNotAfter());
		AcmeHistoryEntry newEntry = new AcmeHistoryEntry(date, serial, directoryURI, accountURI, expirationDate);
		acmeHistoryEntries.add(newEntry);
		boolean rewriteFile = false;
		//Enforce max file size and remove the oldest entry if we are over max
		if (acmeHistoryEntries.size() > AcmeConstants.ACME_HISTORICAL_FILE_MAX_SIZE) {
			rewriteFile = true;
			acmeHistoryEntries.remove(0);
		}
		File file = wslocation.getServerWorkareaResource("acmeca/" + acmeFileName).asFile();
       	FileWriter fr;
		try {

			if (rewriteFile) {
				fr = new FileWriter(file, false);
				for (String h: headers) {
					fr.write(h);
					fr.write("\n");
				}
				for (AcmeHistoryEntry entry: acmeHistoryEntries) {
					fr.write(entry.toString());
					fr.write("\n");
				}
			} else {
				fr = new FileWriter(file, true);
				//write the new entry
			   	fr.write(newEntry.toString() + "\n");
			}
		   	fr.close();
		} catch (IOException e) {
			Tr.error(tc, "CWPKI2072W", file.getAbsolutePath(), e.getMessage());
		}
	}
	
	/**
	 * Convenience method to get a list of directoryURIs
	 * in the ACME historical file. This is used by tests
	 * so we can't update this to use acmeHistoricalEntries
	 * ArrayList.
	 * @param file The ACME file to pull directoryURIs from.
	 * @return A list of directoryURIs from the ACME file.
	 */
	public ArrayList<String> getDirectoryURIHistory(File file) {
		ArrayList<String> entries = new ArrayList<String>();
		if (!file.exists()) {
			return entries;
		}
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    //Read the commented lines
		    String line;
			while ((line = br.readLine()) != null && !line.isEmpty() && line.startsWith("#")) {}
			do {
				StringTokenizer tok = new StringTokenizer(line);
				if (tok.hasMoreTokens()) tok.nextToken();
				if (tok.hasMoreTokens()) tok.nextToken();
				if (tok.hasMoreTokens()) entries.add(tok.nextToken());	
			} while ((line = br.readLine()) != null && !line.isEmpty());
			br.close();

		} catch (Exception e) {
			Tr.error(tc, "CWPKI2072W", file.getAbsolutePath(), e.getMessage());
			return null;
		}
		return entries;
	}
	
	/**
	 * Get the directory URI that was used to obtain
	 * a certificate. This is used for revoking a
	 * certificate after the configured directory URI
	 * has changed.
	 * @param serial The certificate serial number to be revoked.
	 * @return The directory URI corresponding to the certificate serial number.
	 */
	public String getDirectoryURI(String serial) {
		for(AcmeHistoryEntry entry: acmeHistoryEntries) {
			if (serial.equals(entry.getSerial())) {
				return entry.getDirectoryURI();
			}
		}
		return null;
	}
}
