
/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**

<p>Set of classes and interfaces to store log records on disk</p>

<p>Both {@link com.ibm.websphere.logging.hpel.writer.HPELRepositoryExporter} and
{@link com.ibm.websphere.logging.hpel.writer.HPELZipRepositoryExporter} implementations
export records in the HPEL format. The former one exports packages into a directory
maintaining file structure required by HPEL where as the latter - into a zip file containing
such file structure.</p>

<p>The {@link com.ibm.websphere.logging.hpel.writer.CompatRepositoryExporter} and
{@link com.ibm.websphere.logging.hpel.writer.CompatZipRepositoryExporter} are the
implementation to write records in the Basic or Advanced text format. The latter one additionally put
it into a zip file.</p>

<p>The following is a sample of code that uses the APIs in this package (and the <code>
com.ibm.websphere.logging.hpel.reader </code> package) to read from an existing repository and
create a new repository with a subSet of the records.  Note the use of the RepositoryExporter for
creating the new repository, and the use of the LogQueryBean to create a subSet repository based on
filter criteria.  This technique is recommended for copying repositories, but not for replacing logging.

<pre>
<code>
	public static void main(String[] args) {
		WriterSample ws = new WriterSample() ;
		ws.createFromExistingQuery(args[0], args[1]) ;			// This shows how to do it by copying an existing directory
	}
	
		// Idea here is that a repository has been created. This extracts from that repository and creates a new one using the write API
	private void createFromExistingQuery(String sourceRepositoryLocation, String targetRepository) {
		File archiveDir = createRootDirectory(targetRepository) ;		// Create a directory to store the repository
		RepositoryExporter archiver = new HPELRepositoryExporter(archiveDir);		// Repository Exporter for writing
				// Note the LogQueryBean has many filtering options.
		LogQueryBean logQueryBean = new LogQueryBean() ;				// Bean with filter criteria
		logQueryBean.setLevels(Level.WARNING, Level.SEVERE) ;			// We will only capture warning and severe msgs
				// Open a reader to read in the repository created in first step (or any HPEL repository)
		RepositoryReader browser = new RepositoryReaderImpl(sourceRepositoryLocation);
		// This will capture all records and copy to new location
		try {
			for(ServerInstanceLogRecordList list: browser.getLogLists(logQueryBean)) {
				archiver.storeHeader(list.getHeader());
				for (RepositoryLogRecord actual: list) {
					archiver.storeRecord(actual);
				}
			}
		} catch (LogRepositoryException lre) {
			System.out.println("Exception reading initial repository to copy it: "+lre);
		}
		archiver.close();
	}
	
	private File createRootDirectory(String targetRepository) {
		File archiveDir = null ;
		archiveDir = new File(targetRepository) ;
		archiveDir.delete();
		archiveDir.mkdir();
		return archiveDir ;
	}
</code>
</pre>


 * @version 2.0.0
 */
@org.osgi.annotation.versioning.Version("2.0.0")
package com.ibm.websphere.logging.hpel.writer;
