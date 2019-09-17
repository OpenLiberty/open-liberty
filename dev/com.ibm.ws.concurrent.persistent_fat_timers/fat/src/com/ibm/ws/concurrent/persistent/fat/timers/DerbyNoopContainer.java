package com.ibm.ws.concurrent.persistent.fat.timers;

import java.util.concurrent.Future;

import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * This is a Derby no-op database test container that is returned
 * when attempting to create a database test container for
 * an unsupported database or to test against derby embedded.
 * 
 * This test container overrides the start and stop methods
 * to prevent the creation of a docker container. 
 * 
 * Returns connection information for a derby embedded driver. 
 *  
 * TODO: Since this class is general it should be moved to fattest simplicity in the future
 */
class DerbyNoopContainer<SELF extends DerbyNoopContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
	
	private String databaseName = "memory:test";

	/**
	 * @see DerbyNoopContainer
	 */
	public DerbyNoopContainer(final Future<String> image) {
		super(image);
	}
	
	/**
	 * @see DerbyNoopContainer
	 */
	public DerbyNoopContainer() {
		super("");
	}
	
	@Override
	public String getDockerImageName() {
		return "";
	}
	
	@Override
	public void start() {
		//DO NOTHING
	}
	
	@Override
	protected void doStart() {
		//DO NOTHING
	}
	
	@Override
	public void stop() {
		//DO NOTHING
	}
	
	@Override
	public void close() {
		//DO NOTHING
	}
	
	@Override
	public SELF withDatabaseName(String name) {
		this.databaseName = name;
		return self();
	}
	@Override
	public String getDatabaseName() {
		return databaseName;
	}
	
	@Override
	public String getJdbcUrl() {
		return "jdbc:derby:" + databaseName + ";create=true";
	}
	
	@Override
	public String getUsername() {
		return "userx";
	}
	
	@Override
	public String getPassword() {
		return "passx";
	}
	
	@Override
	public Integer getFirstMappedPort() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String getContainerIpAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDriverClassName() {
		return "org.apache.derby.jdbc.AutoloadedDriver";
	}

	@Override
	protected String getTestQueryString() {
		return "";
	}
}
