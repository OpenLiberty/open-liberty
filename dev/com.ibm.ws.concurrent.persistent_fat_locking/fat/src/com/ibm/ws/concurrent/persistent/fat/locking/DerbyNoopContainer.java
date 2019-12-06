package com.ibm.ws.concurrent.persistent.fat.locking;

import java.util.concurrent.Future;

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
    public String getDriverClassName() {
        return null;
    }

    @Override
    public String getJdbcUrl() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    protected String getTestQueryString() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }
}
