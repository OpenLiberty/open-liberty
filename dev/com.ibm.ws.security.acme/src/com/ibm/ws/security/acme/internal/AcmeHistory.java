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
	private String smallSpaceDelim = "         ";
	private final String acmeFile = AcmeConstants.ACME_HISTORY_FILE;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	
	/**
	 * This method determines if the directoryURI has changed by checking
	 * the acme file.
	 * @return True if the directoryURI has changed
	 */
	protected boolean directoryURIChanged(String currentDirectoryURI, WsLocationAdmin wslocation, boolean disableRenewOnNewHistory) {
		int acmefilecreation = createAcmeFile(wslocation);
		//For testing purposes, check for disableRenewOnNewHistory. For some tests, we need to make sure
		//the certificate does not refresh when starting a new server and the history file doesn't exist yet.
		if (acmefilecreation == 1 && disableRenewOnNewHistory) {
			return false;
		}
		//If the ACME file doesn't exist or it failed to create, return true to force refresh.
		if (acmefilecreation > 0) {
			return true;
		}
		File file = wslocation.getServerWorkareaResource("acme/" + acmeFile).asFile();
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
		File file = wslocation.getServerWorkareaResource("acme/" + acmeFile).asFile();
		if (file.exists()) return 0;
		file.getParentFile().mkdirs();
		LocalDateTime now = LocalDateTime.now();  
		String date = FORMATTER.format(now);
		try {
			file.createNewFile();
			FileWriter fr;
			fr = new FileWriter(file, false);
			fr.write("# WARNING!!! DO NOT MODIFY THIS FILE. IT HAS BEEN AUTO-GENERATED: " + date + "\n");
			fr.write("# Version 1.0" + "\n");
			fr.write("# Date" + spaceDelim + "Serial" + spaceDelim + "DirectoryURI" + spaceDelim + "Account URI" + "\n");
			fr.write("# -------------------------------------------------------------------------------------------------------------------------");
	       	fr.close();
	       	return 1;
		} catch (IOException e) {
			Tr.error(tc, "CWPKI2072W", file.getAbsolutePath(), e.getMessage());
		}
		return 2;
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
		if (createAcmeFile(wslocation) == 2) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();  
		String date = FORMATTER.format(now);
		String serial = null;
		if (acmeCertificate == null) {
			if (certificate != null) serial = certificate.getSerialNumber().toString(16);
		} else {
			serial = acmeCertificate.getCertificate().getSerialNumber().toString(16);
		}

		File file = wslocation.getServerWorkareaResource("acme/" + acmeFile).asFile();
		writeAcmeFileLine(file, date, serial, directoryURI, accountURI);

	}
	
	/**
	 * Helper method to update the ACME file specified using the parameters specified.
	 * @param file The file to update.
	 * @param date The current date and time.
	 * @param serial The certificate serial number.
	 * @param directoryURI The current directoryURI from config.
	 * @param accountURI The current accountURI from config.
	 */
	public void writeAcmeFileLine(File file, String date, String serial, String directoryURI, String accountURI) {
		//Enforce max file size
		ArrayList<String> header = new ArrayList<String>();
		ArrayList<String> entries = new ArrayList<String>();
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line; 
			while ((line = br.readLine()) != null && !line.isEmpty()) {
				if (line.startsWith("#")) {
					header.add(line);
				} else {
					entries.add(line);
				}
			}
			br.close();
		} catch (Exception e) {
			Tr.error(tc, "CWPKI2072W", file.getAbsolutePath(), e.getMessage());
		}
		//remove the oldest entry if we are at max
		if (entries.size() >= AcmeConstants.ACME_HISTORICAL_FILE_MAX_SIZE) {
			entries.remove(0);
		}
       	FileWriter fr;
		try {
			fr = new FileWriter(file, false);
			for (String h: header) {
				fr.write(h);
				fr.write("\n");
			}
			for (String e: entries) {
				fr.write(e);
				fr.write("\n");
			}
			//write the new entry
		   	fr.write(date + smallSpaceDelim + serial + smallSpaceDelim + directoryURI + smallSpaceDelim + accountURI);
		   	fr.close();
		} catch (IOException e) {
			Tr.error(tc, "CWPKI2072W", file.getAbsolutePath(), e.getMessage());
		}
	}
	
	/**
	 * Convenience method to get a list of directoryURIs
	 * in the ACME historical file.
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
}
