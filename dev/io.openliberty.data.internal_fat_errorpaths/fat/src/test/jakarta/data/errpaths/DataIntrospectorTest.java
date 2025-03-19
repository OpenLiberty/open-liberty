/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.errpaths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;

/**
 * Contains tests that make assertions on the Jakarta Data introspector output.
 * The introspector runs at the end of DataErrPathsTest before stopping the server
 * so that it contains the state of a running server, and guaranteeing that it is
 * available to these tests.
 */
@RunWith(FATRunner.class)
public class DataIntrospectorTest extends FATServletClient {
    /**
     * Jakarta Data introspector output is captured during DataErrPathsTest.tearDown
     */
    static final List<String> introspectorOutput = new ArrayList<>();

    /**
     * Asserts that a line containing the expected substring is found
     * within the Jakarta Data introspector output.
     *
     * @param expected the line to search for.
     */
    private static void assertLineContains(String expectedSubstring) {
        if (introspectorOutput.isEmpty())
            fail("JakartaDataIntrospector output not found. Unable to run test. " +
                 "Check server logs for errors.");

        for (String line : introspectorOutput)
            if (line.contains(expectedSubstring))
                return;

        fail("Substring not found within introspector output. " +
             "To view introspector output from the test results page, " +
             "follow the System.out link and then search for " +
             "testIntrospectorOutputObtained. Missing substring is: " +
             expectedSubstring);
    }

    /**
     * Asserts that a line is found within the Jakarta Data introspector output.
     *
     * @param expected the line to search for.
     */
    private static void assertLineFound(String expected) {
        if (introspectorOutput.isEmpty())
            fail("JakartaDataIntrospector output not found. Unable to run test. " +
                 "Check server logs for errors.");

        if (!introspectorOutput.contains(expected))
            fail("Information not found in introspector output. " +
                 "To view introspector output from the test results page, " +
                 "follow the System.out link and then search for " +
                 "testIntrospectorOutputObtained. Missing line is: " +
                 expected);
    }

    /**
     * Verify that introspector output was obtained.
     */
    @Test
    public void testIntrospectorOutputObtained() {
        assertEquals(false, introspectorOutput.isEmpty());

        // To view output, from the test results page, follow the System.out link
        // and search for "testIntrospectorOutputObtained"
        for (String line : introspectorOutput)
            System.out.println(line);
    }

    /**
     * Verify that introspector output contains the entity class that is used
     * by the repository.
     */
    @Test
    public void testOutputContainsCauseOfException() {
        assertLineFound("        Caused by: javax.naming.NameNotFoundException: AbsentFromConfig");
    }

    /**
     * Verify the introspector output contains a count query that is used to
     * find the number of total elements across all pages.
     */
    @Test
    public void testOutputContainsCountQueryForPages() {
        assertLineFound("    JPQL count query: " +
                        "SELECT COUNT(o) FROM Voter o WHERE (o.address=?1)");
    }

    /**
     * Verify that introspector output contains the config display id of
     * databaseStore elements that are used by repositories.
     */
    @Test
    public void testOutputContainsDatabaseStore() {
        assertLineFound("    for databaseStore application[DataErrPathsTestApp]" +
                        "/databaseStore[java:app/jdbc/DerbyDataSource]");

        assertLineFound("    for databaseStore application[DataErrPathsTestApp]" +
                        "/module[DataErrPathsTestApp.war]" +
                        "/databaseStore[java:comp/jdbc/InvalidDatabase]");
    }

    /**
     * Verify that introspector output contains the dataStore value that is
     * configured on the repository.
     */
    @Test
    public void testOutputContainsDataStore() {
        assertLineFound("        dataStore: AbsentFromConfig");
        assertLineFound("        dataStore: java:app/env/WrongPersistenceUnitRef");
        assertLineFound("        dataStore: java:app/jdbc/DerbyDataSource");
        assertLineFound("        dataStore: java:comp/DefaultDataSource");
        assertLineFound("        dataStore: java:comp/jdbc/InvalidDatabase");
        assertLineFound("        dataStore: java:module/env/DoesNotExist");
        assertLineFound("        dataStore: java:module/jdbc/DataSourceForInvalidEntity");
    }

    /**
     * Verify that introspector output contains the name of the module or
     * application that defines the repository.
     */
    @Test
    public void testOutputContainsDefiningArtifact() {
        assertLineFound("        defining artifact: DataErrPathsTestApp#DataErrPathsTestApp.war");
    }

    /**
     * Verify that introspector output contains the entity class that is used
     * by the repository.
     */
    @Test
    public void testOutputContainsEntity() {
        assertLineFound("        entity: test.jakarta.data.errpaths.web.Voter");
    }

    /**
     * Verify that introspector output contains entity information.
     */
    @Test
    public void testOutputContainsEntityInfo() {
        assertLineFound("        name: Voter");
        assertLineFound("          id(this) -> ssn");
        assertLineFound("        name: PollingLocationEntity");
        assertLineFound("          opensAt: java.time.LocalTime");
        assertLineFound("        attributes for entity update:" +
                        " [address, closesAt, opensAt, precinct, ward]");
    }

    /**
     * Verify that introspector output contains method signatures from a
     * generated entity class that is automatically created for an
     * application-supplied record entity.
     */
    @Test
    public void testOutputContainsGeneratedEntityClassSignatures() {
        assertLineContains("public PollingLocationEntity(");
        assertLineContains("public java.time.LocalTime getClosesAt()");
        assertLineContains("public void setClosesAt(java.time.LocalTime)");
    }

    /**
     * Verify that the introspector output contains the names of JPQL named parameters.
     */
    @Test
    public void testOutputContainsJPQLNamedParameterNames() {
        assertLineFound("    JPQL parameter names: [lname]");
    }

    /**
     * Verify that introspector output contains the name of the primary entity class.
     */
    @Test
    public void testOutputContainsPrimaryEntity() {
        assertLineFound("      primary entity: test.jakarta.data.errpaths.web.Invention");
        assertLineFound("      primary entity: test.jakarta.data.errpaths.web.Volunteer");
        assertLineFound("      primary entity: test.jakarta.data.errpaths.web.Voter");
    }

    /**
     * Verify that introspector output contains the a failure that occurred when
     * attempting to initialize query information for a repository method.
     */
    @Test
    public void testOutputContainsQueryInfoInitFailure() {
        assertLineContains("java.lang.UnsupportedOperationException: CWWKD1003E:");
    }

    /**
     * Verify that introspector output contains method signatures from an
     * application-supplied record entity.
     */
    @Test
    public void testOutputContainsRecordClassSignatures() {
        assertLineContains("public PollingLocation(long, java.lang.String," +
                           " java.time.LocalTime, java.time.LocalTime, int, int)");
        assertLineContains("public java.time.LocalTime closesAt()");
    }

    /**
     * Verify that introspector output contains the name of repository interfaces.
     */
    @Test
    public void testOutputContainsRepository() {
        assertLineFound("      repository: test.jakarta.data.errpaths.web.InvalidDatabaseRepo");
        assertLineFound("      repository: test.jakarta.data.errpaths.web.InvalidJNDIRepo");
        assertLineFound("      repository: test.jakarta.data.errpaths.web.InvalidNonJNDIRepo");
        assertLineFound("      repository: test.jakarta.data.errpaths.web.Inventions");
        assertLineFound("      repository: test.jakarta.data.errpaths.web.RepoWithoutDataStore");
        assertLineFound("      repository: test.jakarta.data.errpaths.web.Voters");
        assertLineFound("      repository: test.jakarta.data.errpaths.web.WrongPersistenceUnitRefRepo");
    }

    /**
     * Verify that introspector output contains the Repository annotation.
     */
    @Test
    public void testOutputContainsRepositoryAnnotation() {
        // fields of the annotation could be printed in any order
        assertLineContains("      @Repository(");
        assertLineContains("dataStore=\"java:app/jdbc/DerbyDataSource\"");
    }

    /**
     * Verify that introspector output contains the result type of a repository method.
     */
    @Test
    public void testOutputContainsRepositoryMethodResultType() {
        assertLineFound("    return array type: test.jakarta.data.errpaths.web.Voter");
        assertLineFound("    multiple result type: [Ltest.jakarta.data.errpaths.web.Voter;");
        assertLineFound("    single result type: test.jakarta.data.errpaths.web.Voter");
    }

    /**
     * Verify that introspector output contains the signature of repository methods.
     */
    @Test
    public void testOutputContainsRepositoryMethodSignature() {
        assertLineFound("        @Find");
        assertLineFound("        public abstract java.util.List<Voter> livesAt(" +
                        "@By(\"address\") java.lang.String, jakarta.data.Limit, " +
                        "jakarta.data.Order<Voter>, jakarta.data.Limit) ");
    }
}
