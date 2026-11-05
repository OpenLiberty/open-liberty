package com.ibm.ws.security.audit.fat.config.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import componenttest.topology.impl.LibertyServer;

/**
 * Provides collection of file based TestableAuditLogs for a given Liberty server.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class TestableAuditLogs<T extends TestableAuditLog> {
	protected Class<T> clazz;
	protected LibertyServer server;
	protected Path dir;

	/**
	 * Given a LibertyServer, a log class and a directory path, create a collection of TestableAuditLogs.
	 * 
	 * @param server - Liberty server 
	 * @param class1 - log class
	 * @param logPath - directory path for log collection
	 */
	public TestableAuditLogs(Class<T> class1, LibertyServer myServer, Path logPath) {
		clazz = class1;
		server = myServer;
		dir = logPath;
	}

	public TestableAuditLogs(Class<T> class1, LibertyServer myServer) {
		this(class1, myServer, Paths.get(myServer.getLogsRoot()));
	}

	/**
	 * Given a log directory path, return a list of TestableAuditLogs from that directory.
	 * 
	 * @return list of TestableAuditLogs
	 */
	public List<T> getLogs() throws Exception {
		List<T> list = new ArrayList<>();

		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir);
			for (Path entry : dirStream) {

				if (entry.getFileName().toString().startsWith("audit")) {
					System.out.println("Log being added: " + dir + "/" + entry.getFileName().toString());
					list.add(clazz.getConstructor(String.class).newInstance(dir + "/" + entry.getFileName().toString()));
				}
			}
			dirStream.close();
		} catch (DirectoryIteratorException ex) {
			// I/O error encountered during the iteration, the cause is an
			// IOException
			throw ex.getCause();
		}
		System.out.println(list.toString());
		return list;

	}

	/**
	 * Assert that TestableAuditLogs contains the expected number of log files.
	 * 
	 * @param expected number of logs
	 */
	public void verifyLogsCount(int expectedLogs) throws Exception {
		assertEquals("Expected " + expectedLogs + " audit logs. Instead found: " + getLogs().size(), expectedLogs,
				getLogs().size());
	}
	
	/**
	 * Assert that TestableAuditLogs contains log files and is not empty.
	 * 
	 */
	public void ensureLogsExist() throws Exception {
		assertTrue("Expected audit logs not found in the log location", !getLogs().isEmpty());
	}

	/**
	 * Assert that TestableAuditLogs collection is empty and does not contain any logs.
	 * 
	 */
	public void ensureLogsNotExist() throws Exception {
		assertTrue("Expected audit logs should not be found, but logs were found.", getLogs().isEmpty());
	}
	

}
