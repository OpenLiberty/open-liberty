package com.ibm.ws.install;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.repository.RepositoryException;
import com.ibm.ws.install.repository.RepositoryFactory;

public class DirectoryRepositoryTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testNonExistDirectory() {
        try {
            RepositoryFactory.getInstance(new File("unknown"));
            fail("RepositoryFactory.getInstance() didn't throw exception CWWKF1500E.");
        } catch (RepositoryException e) {
            assertTrue("RepositoryFactory.getInstance() should throw exception CWWKF1500E.", e.getMessage().contains("CWWKF1500E"));
        }
    }

    @Test
    public void testFile() {
        try {
            RepositoryFactory.getInstance(new File("build/unittest/wlpDirs/developers/wlp/lib/versions/WebSphereApplicationServer.properties"));
            fail("RepositoryFactory.getInstance() didn't throw exception CWWKF1501E.");
        } catch (RepositoryException e) {
            assertTrue("RepositoryFactory.getInstance() should throw exception CWWKF1501E.", e.getMessage().contains("CWWKF1501E"));
        }
    }
}
