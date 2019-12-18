package componenttest.topology.database.container;

import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * This is a Derby no-op database test container that is returned
 * when attempting to test against derby embedded.
 *
 * This test container overrides the start and stop methods
 * to prevent the creation of a docker container.
 *
 */
class DerbyNoopContainer<SELF extends DerbyNoopContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

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
	public String getJdbcUrl() {
		return "jdbc:derby:memory:test;create=true";
	}

	@Override
	public String getUsername() {
		return "dbuser";
	}

	@Override
	public String getPassword() {
		return "{xor}Oz0vKDtu";
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
