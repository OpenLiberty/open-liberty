/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.datastore;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@Deprecated // TODO switch to use the DDLGenScriptHelper from fattest.simplicity
public abstract class DDLGenScriptHelper {

    /**
     * Execute the ddlgen command on the specified server and return the generated ddl file names.
     *
     * @param server The target server to execute the ddlgen command.
     *
     * @return The generated ddl file names.
     * @throws Exception if an error occurs running the ddlgen command or locating the ddl files.
     */
    public static List<String> getGeneratedDDLFiles(LibertyServer server) throws Exception {
        String methodName = "getDDLFiles";
        Properties env = new Properties();
        String scriptName;
        String installRoot = server.getInstallRoot();
        Machine machine = server.getMachine();

        if (machine.getOperatingSystem() == OperatingSystem.WINDOWS) {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen.bat";
        } else {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen";
        }

        ProgramOutput po = server.getMachine()
                        .execute(scriptName,
                                 new String[] {
                                                "generate",
                                                server.getServerName()
                                 },
                                 server.getInstallRoot(),
                                 env);

        int rc = po.getReturnCode();
        String stdout = po.getStdout();

        Log.info(DDLGenScriptHelper.class, methodName, "Executed command:" + po.getCommand());
        Log.info(DDLGenScriptHelper.class, methodName, "stdout:" + stdout);
        Log.info(DDLGenScriptHelper.class, methodName, "stderr:" + po.getStderr());
        Log.info(DDLGenScriptHelper.class, methodName, "rc:" + rc);
        Log.info(DDLGenScriptHelper.class, methodName, "serverRoot:" + server.getServerRoot());

        if (rc != 0)
            throw new Exception("Expected return code 0, actual return code " + rc);

        if (!stdout.contains("CWWKD0107I")) {
            throw new Exception("Output did not contain success message CWWKD0107I");
        }

        List<String> ddlFileNames = server.listDDLFiles(server.getServerName());

        Log.info(DDLGenScriptHelper.class, methodName, "DDL Files Found : ");
        for (String fileName : ddlFileNames) {
            Log.info(DDLGenScriptHelper.class, methodName, "    " + fileName);
        }

        Collections.sort(ddlFileNames);

        return ddlFileNames;
    }

    /**
     * Find the expected DDL file location and return the expected file names
     *
     * @param type - The type of database
     * @return The expected ddl file names.
     */
    public static List<String> getExpectedDDLFiles() {
        File ddlDir = new File("test-resources/ddl/");
        assertTrue(ddlDir.isDirectory());
        return Stream.of(ddlDir.listFiles()).map(file -> file.getName()).sorted().toList();
    }

    /**
     * Read the specified DDL file and return all of the SQL statements found in the file.
     *
     * @param server      The target server that contains the DDL file to be read.
     * @param ddlFileName The name of the generated DDL file.
     *
     * @return The SQL statements read from the specified DDL file.
     * @throws Exception If an error occurs reading the specified DDL file.
     */
    public static List<String> readSQLFromGeneratedDDLFile(LibertyServer server, String ddlFileName) throws Exception {
        String methodName = "readSQLFromGeneratedDDLFile";
        String line;
        ArrayList<String> sqlFromDDL = new ArrayList<String>();

        RemoteFile ddlFile = server.getFileFromLibertyServerRoot("ddl/" + ddlFileName);

        assertTrue("Expected DDL file could not be opened for reading : " + ddlFile.getAbsolutePath(), ddlFile.exists());
        if (!ddlFile.exists()) {
            throw new Exception("Expected DDL file could not be opened for reading : " + ddlFile.getAbsolutePath());
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ddlFile.getAbsolutePath()))) {
            while ((line = reader.readLine()) != null) {
                sqlFromDDL.add(line);
            }
        }

        Log.info(DDLGenScriptHelper.class, methodName, "SQL found in DDL File : " + ddlFileName);
        for (String sql : sqlFromDDL) {
            Log.info(DDLGenScriptHelper.class, methodName, "    " + sql);
        }

        return sqlFromDDL;
    }

    /**
     * Read the specified DDL file and return all of the SQL statements found in the file.
     *
     * @param type        - The type of database
     * @param ddlFileName - The name of the DDL file
     *
     * @return The SQL statements read from the specified DDL file.
     * @throws IOException If an error occurs reading the specified DDL file.
     *
     */
    public static List<String> readSQLFromExpectedDDLFile(String ddlFileName) throws IOException {
        File ddlFile = new File("test-resources/ddl/" + ddlFileName);
        assertTrue(ddlFile.isFile());
        return Files.lines(ddlFile.toPath()).toList();
    }

}
