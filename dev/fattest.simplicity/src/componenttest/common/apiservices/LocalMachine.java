/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.common.apiservices;

import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

import com.ibm.websphere.simplicity.AsyncProgramOutput;
import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.cmdline.LocalProvider;

public class LocalMachine extends Machine {

    private static Class<Machine> c = Machine.class;
    private static LocalMachine instance = null;

    public static LocalMachine getInstance() throws Exception {
        if (instance == null)
            instance = new LocalMachine();
        return instance;
    }

    public LocalMachine() throws Exception {
        super(Bootstrap.getInstance().getMachineConnectionData("localhost"));
        isLocal = true;
    }

    public LocalMachine(String user, String password) throws Exception {
        super(new ConnectionInfo("localhost", user, password));
        isLocal = true;
    }

    @Override
    public void connect() throws Exception {}

    @Override
    public void disconnect() throws Exception {}

    @Override
    public ProgramOutput execute(String cmd, String[] parameters,
                                 String workDir, Properties envVars) throws Exception {
        return LocalProvider.executeCommand(this, cmd, parameters, workDir,
                                            envVars);
    }

    public void executeAsync(String cmd, String[] parameters, String workDir, Properties envVars, OutputStream redirect) throws Exception {
        LocalProvider.executeCommandAsync(this, cmd, parameters, workDir, envVars, redirect);
    }

    @Override
    public AsyncProgramOutput executeAsync(String cmd, String[] parameters) throws Exception {
        return LocalProvider.executeCommandAsync(this, cmd, parameters, workDir, null);
    }

    @Override
    public String getBootstrapFileKey() {
        return null;
    }

    @Override
    public String getHostname() {
        return "localhost";
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public RemoteFile getTempDir() throws Exception {
        return new RemoteFile(this, System.getProperty("java.io.tmpdir"));
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public boolean isConnected() throws Exception {
        return true;
    }

    @Override
    public OperatingSystem getOperatingSystem() throws Exception {
        if (this.os == null) {
            this.os = OperatingSystem.getOperatingSystem(getRawOSName());
        }
        return this.os;
    }

    /*
     * (non-Javadoc)
     * 
     * @see componenttest.common.apiservices.Machine#killProcess(int)
     */
    @Override
    public void killProcess(int processId) throws Exception {
        killProcessLocal(processId, 0);
    }

    private void killProcessLocal(int processID, int attemptNumber) throws Exception {
        ProgramOutput po = LocalProvider.killProcess(this, processID);
        Thread.sleep(1000);
        if (processStillRunning(processID)) {
            if (attemptNumber >= 3) {
                String ErrorMessage = "The processID " + processID + " could not be killed. The command ran was " + po.getCommand() + " The Std Output is: \'"
                                      + po.getStdout() + "\' and the Std Error was \'" + po.getStderr() + "\'";
                throw new Exception(ErrorMessage);
            } else {
                Log.warning(c, "Unable to kill process " + processID + " - Retrying, this is attempt number " + attemptNumber);
                killProcessLocal(processID, ++attemptNumber);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see componenttest.common.apiservices.Machine#getDate()
     */
    @Override
    public Date getDate() throws Exception {
        return LocalProvider.getDate(this);
    }

    @Override
    public String getRawOSName() throws Exception {
        return System.getProperty("os.name");
    }
}
