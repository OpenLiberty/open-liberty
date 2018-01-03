/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.common.zos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Utility class for interacting with the underlying z/OS SAF product, for
 * creating users, groups, or changing configuration options.
 */
public class SAFConfigHelper {
    /**
     * This class' reference used for logging.
     */
    private static final Class<?> c = SAFConfigHelper.class;

    /**
     * The helper shell script for running tsocmd.
     * This file is created and written by this class.
     */
    private static File _tsoCmdHelperScript = null;

    /**
     * Default label to use when using RACMAP.
     */
    public static final String DEFAULT_MAP_LABEL = "DEFAULT";

    /**
     * Create a group in SAF.
     */
    public static void addGroup(String group, String gid, Cleanup c, boolean ignoreCleanupError) throws Exception {
        // Attempt to delete the group first.
        new CleanupGroup(group, true).call();

        runTsoCmd("ADDGROUP " + group);
        c.addCleanup(new CleanupGroup(group, ignoreCleanupError));

        if (gid != null) {
            runTsoCmd("ALTGROUP " + group + " OMVS(GID(" + gid + "))");
        }
    }

    /**
     * Create a user in SAF.
     */
    public static void addUser(String userName, String[] groups, String uid, String password, Cleanup c, boolean ignoreCleanupError) throws Exception {
        // Attempt to delete the user first.
        new CleanupUser(userName, true).call();

        runTsoCmd("ADDUSER " + userName + " DFLTGRP(" + groups[0] + ")");
        c.addCleanup(new CleanupUser(userName, ignoreCleanupError));

        runTsoCmd("ALTUSER " + userName + " PASSWORD(" + password + ") NOEXPIRED");
        runTsoCmd("PASSWORD USER(" + userName + ") NOINTERVAL");

        // Add OMVS segment, if uid specified.
        if (uid != null) {
            runTsoCmd("ALTUSER " + userName + " OMVS(UID(" + uid + ") HOME(/tmp) PROGRAM(/bin/sh))");
        }

        // Connect user to all specified groups.
        for (int i = 1; i < groups.length; ++i) {
            connectUserToGroup(userName, groups[i]);
        }
    }

    /**
     * Connect a user to a group.
     *
     * Note that doing this can have implications on cleanup logic. A group cannot be
     * deleted until it is empty. It is recommended that you create all groups first, then
     * users, then connect users to groups.
     */
    public static void connectUserToGroup(String userName, String group) throws Exception {
        runTsoCmd("CONNECT " + userName + " GROUP(" + group + ")");
    }

    /**
     * Remove a user from a group.
     *
     * Note that RACF will not let you remove a user from its default group. You will need
     * to change the default group if you want to do this.
     */
    public static void removeUserFromGroup(String userName, String group) throws Exception {
        runTsoCmd("REMOVE " + userName + " GROUP(" + group + ")");
    }

    /**
     * Change the default group for a user.
     *
     * Note that the user must already be connected to the group that you want to make the
     * default.
     */
    public static void changeDefaultGroup(String userName, String newDefaultGroup) throws Exception {
        runTsoCmd("ALTUSER " + userName + " DFLTGRP(" + newDefaultGroup + ")");
    }

    /**
     * Change a user's password.
     */
    public static void changePassword(String userName, String password) throws Exception {
        runTsoCmd("ALTUSER " + userName + " PASSWORD(" + password + ") NOEXPIRED");
    }

    /**
     * Change a user's passphrase.
     *
     * NOTE: The RACF PHRASE() command may not work on some EZWAS-on-VICOM systems.
     * It should work for zOS 1.9, 1.10, and 1.11 systems. zOS 1.12 systems
     * will NOT work, unless they are IPLed with SYSP=(LC,KB). If it doesn't
     * work, this method will throw an IOException with a message stating that
     * the passphrase exit (ICHPWX11) rejected the passphrase.
     */
    public static void changePassphrase(String userName, String passphrase) throws Exception {
        runTsoCmd("ALTUSER " + userName + " PHRASE('" + passphrase + "') NOEXPIRED");
    }

    /**
     * Set a user RESTRICTED.
     */
    public static void setRestricted(String userName) throws Exception {
        runTsoCmd("ALTUSER " + userName + " RESTRICTED");
    }

    /**
     * Set mixed-case passwords enabled.
     */
    public static void setMixedCasePWEnabled(boolean mixedEnabled) throws Exception {
        if (mixedEnabled) {
            runTsoCmd("SETROPTS PASSWORD(MIXEDCASE)");
        } else {
            runTsoCmd("SETROPTS PASSWORD(NOMIXEDCASE)");
        }
    }

    /**
     * Run a TSO command on the system.
     *
     * This method is a total hack at this time. It uses a shell script
     * to execute the TSO commands (because for the life of me I can't figure
     * out how to execute the TSO commands directly via Runtime.exec(). All
     * sorts of problems with the embedded ' and "). The script invokes the
     * TSO command as IBMUSER via su. IBMUSER has special authority, granted
     * to it when zLiberty is installed on the EZWAS machine. This authority
     * allows IBMUSER to invoke certain TSO commands (like PASSWORD).
     */
    public static BufferedReader runTsoCmd(String tsoCmd) throws Exception {
        if (_tsoCmdHelperScript == null) {
            _tsoCmdHelperScript = createTsoCmdHelperScript("/tmp/mytsocmd.sh");
        }

        String[] cmdArray = new String[] { _tsoCmdHelperScript.getCanonicalPath(), tsoCmd };
        Process p = Runtime.getRuntime().exec(cmdArray);
        p.waitFor();

        int rc = p.exitValue();
        if (rc != 0) {
            // rc != 0 means something went wrong.  Retrieve the output and wrap it all
            // up into a nice exception.
            String exMsg = "";
            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                exMsg += (line + ". ");
            }
            buf.close();
            BufferedReader buferr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = buferr.readLine()) != null) {
                exMsg += (line + ". ");
            }
            buferr.close();
            throw new IOException("TSO command '" + tsoCmd + "' failed with rc " + rc + ". " + exMsg);
        }

        return new BufferedReader(new InputStreamReader(p.getInputStream()));
    }

    /**
     * Creates the helper script that invokes the tsocmd under IBMUSER
     * authority via su.
     *
     * This script assumes that:
     * 1) IBMUSER exists and has SPECIAL USER authority to RACF.
     * 2) tsocmd exists on this system (and is in the PATH).
     */
    private static File createTsoCmdHelperScript(String fileName) throws Exception {
        File f = new File(fileName);
        FileOutputStream fos = new FileOutputStream(fileName);
        Writer fw = new BufferedWriter(new OutputStreamWriter(fos, "Cp1047"));

        String[] fileLines = new String[] { "#!/bin/sh\n",
                                            "SPECIAL_USER=IBMUSER\n",
                                            "su -s $SPECIAL_USER -c \"tsocmd \\\"$*\\\"\"\n" };
        for (int i = 0; i < fileLines.length; ++i) {
            fw.write(fileLines[i]);
        }
        fw.close();
        f.setExecutable(true);
        return f;
    }

    /**
     * Populate the SAF registry with users/groups.
     */
    public static void populateSAF(Cleanup c, boolean ignoreCleanupErrors) throws Exception {

        // deleting the users ahead, incase the previous run didnot cleanup users properly.
        // this makes sure that the users and the groups are deleted before creating them again.
        String defaultMapLabel = DEFAULT_MAP_LABEL;
        depopulateSAF(defaultMapLabel, true); // ignore if the users or groups are not already existing

        int uid = 51110; // Need a unique ID for each OMVS group-id and user-id.

        // Create some groups.
        SAFConfigHelper.addGroup("RSTGRP1", Integer.toString(++uid), c, ignoreCleanupErrors);

        SAFConfigHelper.addGroup("RSTGRP2", Integer.toString(++uid), c, ignoreCleanupErrors);

        SAFConfigHelper.addGroup("RSTGRP3", Integer.toString(++uid), c, ignoreCleanupErrors);

        SAFConfigHelper.addGroup("MRSTGRP1", Integer.toString(++uid), c, ignoreCleanupErrors);

        SAFConfigHelper.addGroup("MRSTGRP2", Integer.toString(++uid), c, ignoreCleanupErrors);

        // Disable mixed-case passwords.
        SAFConfigHelper.setMixedCasePWEnabled(false);

        // Regular user.
        SAFConfigHelper.addUser("RSTUSR1", new String[] { "RSTGRP1", "RSTGRP2" }, Integer.toString(++uid), "PRSTUSR1", c, ignoreCleanupErrors);

        // User without OMVS segment.
        SAFConfigHelper.addUser("RSTUSR2", new String[] { "RSTGRP2" }, null, "PRSTUSR2", c, ignoreCleanupErrors);

        // Create user with mixed-case password while mixedCasePWEnabled = false.
        SAFConfigHelper.addUser("RSTUSR5", new String[] { "RSTGRP1", "RSTGRP2" }, Integer.toString(++uid), "PrStUsR5", c, ignoreCleanupErrors);

        // Create RESTRICTED user.
        SAFConfigHelper.addUser("RSTUSR6", new String[] { "RSTGRP1" }, Integer.toString(++uid), "PRSTUSR6", c, ignoreCleanupErrors);
        SAFConfigHelper.setRestricted("RSTUSR6");

        // Create RESTRICTED user without OMVS segment.
        SAFConfigHelper.addUser("RSTUSR7", new String[] { "RSTGRP1" }, null, "PRSTUSR7", c, ignoreCleanupErrors);
        SAFConfigHelper.setRestricted("RSTUSR7");

        // Enable mixed-case passwords for the Following users.
        SAFConfigHelper.setMixedCasePWEnabled(true);

        // User with mixed-case password.
        SAFConfigHelper.addUser("RSTUSR3", new String[] { "RSTGRP3" }, Integer.toString(++uid), "PrStUsR3", c, ignoreCleanupErrors);

        // User with all-upper-case mixed-case password.
        SAFConfigHelper.addUser("RSTUSR4", new String[] { "RSTGRP3" }, Integer.toString(++uid), "PRSTUSR4", c, ignoreCleanupErrors);

        SAFConfigHelper.addUser("MRSTUSR1", new String[] { "RSTGRP1", "RSTGRP2", "RSTGRP3" }, Integer.toString(++uid), "PMRSTUSR", c, ignoreCleanupErrors);

        SAFConfigHelper.addUser("MRSTUSR2", new String[] { "RSTGRP1", "RSTGRP2", "RSTGRP3" }, Integer.toString(++uid), "PMRSTUSR", c, ignoreCleanupErrors);

        SAFConfigHelper.addUser("MRSTUSR3", new String[] { "RSTGRP1", "RSTGRP2", "RSTGRP3" }, Integer.toString(++uid), "PMRSTUSR", c, ignoreCleanupErrors);

        // Add some additional cleanup here... I'm not sure why I am just copying it forward.
        c.addCleanup(new CleanupMap("RSTUSR1", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("RSTUSR2", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("RSTUSR3", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("RSTUSR4", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("RSTUSR5", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("RSTUSR6", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("RSTUSR7", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("MRSTUSR1", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("MRSTUSR2", defaultMapLabel, true));
        c.addCleanup(new CleanupMap("MRSTUSR3", defaultMapLabel, true));
    }

    /**
     * Remove the pre-populated SAF users/groups.
     */
    private static void depopulateSAF(String mapLabel, boolean ignoreErrors) throws Exception {

        // delete any user maps
        new CleanupMap("RSTUSR1", mapLabel, true).call();
        new CleanupMap("RSTUSR2", mapLabel, true).call();
        new CleanupMap("RSTUSR3", mapLabel, true).call();
        new CleanupMap("RSTUSR4", mapLabel, true).call();
        new CleanupMap("RSTUSR5", mapLabel, true).call();
        new CleanupMap("RSTUSR6", mapLabel, true).call();
        new CleanupMap("RSTUSR7", mapLabel, true).call();
        new CleanupMap("MRSTUSR1", mapLabel, true).call();
        new CleanupMap("MRSTUSR2", mapLabel, true).call();
        new CleanupMap("MRSTUSR3", mapLabel, true).call();

        // deleting all the users
        new CleanupUser("RSTUSR1", ignoreErrors).call();
        new CleanupUser("RSTUSR2", ignoreErrors).call();
        new CleanupUser("RSTUSR3", ignoreErrors).call();
        new CleanupUser("RSTUSR4", ignoreErrors).call();
        new CleanupUser("RSTUSR5", ignoreErrors).call();
        new CleanupUser("RSTUSR6", ignoreErrors).call();
        new CleanupUser("RSTUSR7", ignoreErrors).call();
        new CleanupUser("MRSTUSR1", ignoreErrors).call();
        new CleanupUser("MRSTUSR2", ignoreErrors).call();
        new CleanupUser("MRSTUSR3", ignoreErrors).call();

        // In the following calls, the 2nd parm is set to false, meaning we don't
        // want to ignore the INVALID GROUP exception.  This is to make sure the
        // test cleans up properly.  DELGROUP will fail with INVALID GROUP if the
        // group is not empty of users, which probably means somebody added a new
        // user to this unit test, but forget to delete them in the list above.
        new CleanupGroup("RSTGRP1", ignoreErrors).call();
        new CleanupGroup("RSTGRP2", ignoreErrors).call();
        new CleanupGroup("RSTGRP3", ignoreErrors).call();
        new CleanupGroup("MRSTGRP1", ignoreErrors).call();
        new CleanupGroup("MRSTGRP2", ignoreErrors).call();
    }

    /**
     * Define a resource and refresh its class. This attempts to delete the resource first.
     *
     * @param resourceProfile the resource to define
     * @param safClass the class with which to associate the resource
     * @param uacc the access level
     * @throws Exception
     */
    public static void defineResource(String resourceProfile, String safClass, String uacc, Cleanup c, boolean ignoreCleanupError) throws Exception {
        defineResourceWithOwner(resourceProfile, safClass, uacc, null, c, ignoreCleanupError);
    }

    public static void defineResourceWithOwner(String resourceProfile, String safClass, String uacc, String owner, Cleanup c, boolean ignoreCleanupError) throws Exception {
        // Attempt to delete the resource first
        new CleanupResource(safClass, resourceProfile, true).call();

        // Now define the new resource
        String command = "RDEFINE " + safClass + " " + resourceProfile + " UACC(" + uacc + ")";
        if (owner != null) {
            command = command + " OWNER(" + owner + ")";
        }
        runTsoCmd(command);
        c.addCleanup(new CleanupResource(safClass, resourceProfile, ignoreCleanupError));

        refreshClass(safClass);
    }

    public static void defineStartedProfile(String procName, String userId, Cleanup c, boolean ignoreCleanupError) throws Exception {
        // Attempt to delete the resource first.
        String profileName = procName + ".*";
        new CleanupResource("STARTED", profileName, true).call();

        // Now define the new resource.
        SAFConfigHelper.runTsoCmd("RDEFINE STARTED " + profileName + " STDATA(USER(" + userId + ") GROUP(SYS1) TRACE(YES))");
        c.addCleanup(new CleanupResource("STARTED", profileName, ignoreCleanupError));

        refreshClass("STARTED");
    }

    /**
     * Issue the PERMIT command to allow a user the specified level of access
     * to the given resource associated with the given class
     *
     * @param resourceProfile the resource for which the user will be permitted the given level of access
     * @param safClass the class with which the given resource is associated
     * @param user the user
     * @param access the access level
     * @throws Exception
     */
    public static void permitAccess(String resourceProfile, String safClass, String user, String access) throws Exception {
        runTsoCmd("PERMIT " + resourceProfile + " ID(" + user + ") ACCESS(" + access + ") CLASS(" + safClass + ")");
        refreshClass(safClass);
    }

    /**
     * Defines RACMAP data for a user.
     */
    public static class RACMAPData {
        private final String userdidfilter;
        private final String registry;

        private RACMAPData(String userdidfilter, String registry) {
            this.userdidfilter = userdidfilter;
            this.registry = registry;
        }

        public String getUserdidfilter() {
            return userdidfilter;
        }

        public String getRegistry() {
            return registry;
        }
    }

    /**
     * Gets the list of RACMAP entries for a user. Note that this method does not handle long
     * input very well (if the user's mapping is a long string).
     */
    public static Map<String, RACMAPData> getRacmapData(String user) throws Exception {
        BufferedReader output = runTsoCmd("RACMAP ID(" + user + ") LISTMAP");
        Utils.println("Called RACMAP ID(" + user + ") LISTMAP");
        // Sample output:
        // ----------------
        // Mapping information for user RSTUSR1:
        //
        //    Label: LABEL00000001
        //    Distributed Identity User Name Filter:
        //      >This is the name of a really long userdidfilter that i am typing into TS<
        //      >O and I hope that the string makes it into the input OK because it is lo<
        //      >ng<
        //    Registry Name:
        //      >This is the name of a really long registry that i am typing into TSO and<
        //      > I hope that the string makes it into the intput OK because it is long<
        //
        Map<String, RACMAPData> mapping = new java.util.HashMap<String, RACMAPData>();
        String currentLine = output.readLine();
        Utils.println("First line: " + currentLine);
        while (currentLine != null) {
            // Look for a label.
            if (currentLine.contains("Label:")) {
                String label = currentLine.substring(currentLine.indexOf(":") + 1).trim();
                Utils.println("Read label: " + label);

                // Now read the distributed identity (comes after the label).
                currentLine = output.readLine(); // Distributed Identity User Name Filter:
                currentLine = output.readLine(); //   >YYY<
                StringBuilder userdidfilter = new StringBuilder();
                while (currentLine.contains(">")) {
                    userdidfilter.append(currentLine.substring(currentLine.indexOf(">") + 1, currentLine.indexOf("<")));
                    currentLine = output.readLine();
                }

                // Now read the registry (comes after the distributed ID)
                currentLine = output.readLine(); //   >ZZZ<
                StringBuilder registry = new StringBuilder();
                while (currentLine.contains(">")) {
                    registry.append(currentLine.substring(currentLine.indexOf(">") + 1, currentLine.indexOf("<")));
                    currentLine = output.readLine();
                }
                Utils.println("Adding to map: " + label);
                mapping.put(label, new RACMAPData(userdidfilter.toString(), registry.toString()));
            } else {
                currentLine = output.readLine();
            }
        }
        return mapping;
    }

    /**
     * Gets the list of users in a group. Note that this parses the output of LISTGRP which
     * is discouraged in the documentation of LISTGRP.
     */
    static List<String> getUsersInGroup(String groupName) throws Exception {
        // First list the users in the group.  If the group does not exist, ignore it.
        List<String> usersInGroup = new LinkedList<String>();
        BufferedReader br = null;

        try {
            br = runTsoCmd("LISTGRP " + groupName);
            Utils.println("Called LISTGRP " + groupName);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg.toUpperCase().contains("ICH51003I NAME NOT FOUND") == false) {
                throw e;
            }
        }

        if (br != null) {
            String currentLine = br.readLine();
            Utils.println("First line: " + currentLine);
            while (currentLine != null) {
                // Look for the line before the first group is printed.  We'll assume that the
                // following line has the first user, and that each user after that will start
                // on the same index.
                if (currentLine.contains("USER(S)")) {
                    currentLine = br.readLine();
                    int leadingWhitespace = countLeadingWhitespace(currentLine);
                    // Exactly X leading whitespace, followed by 1-8 non-whitespace characters, followed
                    // by at least 1 whitespace character, followed by anything.
                    String patternString = "^[\\s]{" + leadingWhitespace + "}([^\\s]{1,8})[\\s]{1,}.*";
                    Utils.println("Using pattern: " + patternString);
                    Pattern p = Pattern.compile(patternString);
                    while (currentLine != null) {
                        Matcher m = p.matcher(currentLine);
                        if (m.matches()) {
                            usersInGroup.add(m.group(1));
                        }
                        currentLine = br.readLine();
                    }
                } else {
                    currentLine = br.readLine();
                }
            }
        }

        return usersInGroup;
    }

    private static int countLeadingWhitespace(String line) {
        int count = 0;
        for (int x = 0; x < line.length(); x++) {
            if (Character.isWhitespace(line.charAt(x))) {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    /**
     * Gets the access list for a particular profile.
     */
    public static Map<String, String> getAccessList(String resourceProfile, String safClass) throws Exception {
        BufferedReader output = runTsoCmd("RLIST " + safClass + " " + resourceProfile + " AUTHUSER");
        Map<String, String> authMap = new java.util.HashMap<String, String>();

        // Looking for something like this:
        // USER      ACCESS   ACCESS COUNT
        // ----      ------   ------ -----
        // IBMUSER   ALTER       000000
        // MSTONE1   READ        000000
        boolean parsingUsers = false;
        String currentLine = "";
        while ((currentLine = output.readLine()) != null) {
            if (parsingUsers == true) {
                StringTokenizer st = new StringTokenizer(currentLine);
                if (st.countTokens() == 3) {
                    String user = st.nextToken();
                    String access = st.nextToken();
                    if (authMap.put(user, access) != null) {
                        throw new RuntimeException("Duplicate entry for " + user);
                    }
                } else {
                    parsingUsers = false; // All done
                }
            }
            if ((currentLine.contains("USER")) && (currentLine.contains("ACCESS")) && (currentLine.contains("COUNT"))) {
                output.readLine(); // Skip the dashes line
                parsingUsers = true; // Start parsing users
            }
        }

        output.close();

        return authMap;
    }

    /**
     * Issue SETROPTS to activate the given class.
     *
     * @param safClass the class to activate
     * @throws Exception
     */
    private static void activateSafClass(String safClass, Cleanup c) throws Exception {
        runTsoCmd("SETROPTS CLASSACT(" + safClass + ")");
        c.addCleanup(new CleanupActiveSafClass(safClass));
    }

    private static void deactivateSafClass(String safClass, Cleanup c) throws Exception {
        runTsoCmd("SETROPTS NOCLASSACT(" + safClass + ")");
        c.addCleanup(new CleanupDeactiveSafClass(safClass));
    }

    /**
     * Refresh the given class.
     *
     * @param safClass the class to refresh
     * @throws Exception
     */
    public static void refreshClass(String safClass) throws Exception {
        runTsoCmd("SETROPTS GENERIC(" + safClass + ") REFRESH");
        runTsoCmd("SETROPTS RACLIST(" + safClass + ") REFRESH");
    }

    /**
     * REF: http://pic.dhe.ibm.com/infocenter/wasinfo/v8r5/index.jsp?topic=%2Fcom.ibm.websphere.wlp.nd.multiplatform.doc%2Fae%2Ftwlp_config_security_zos.html
     *
     * @param authorizedServices, e.g. SAFCRED, ZOSWLM, TXRRS, ZOSDUMP
     *
     * @return The profile name, "bbg.authmod.bbgzsafm.{authorizedServices}"
     */
    public static String getAuthorizedServicesProfileName(String authorizedServices) {
        return "bbg.authmod.bbgzsafm." + authorizedServices;
    }

    /**
     * Grant the given serverUserId READ access to the given authorizedServices. Note that the
     * profile that gives access to the resource is NOT cleaned up.
     *
     * @param authorizedServices, e.g. SAFCRED, ZOSWLM, TXRRS, ZOSDUMP
     * @param serverUserId - the userId of the server
     */
    public static void grantAuthorizedServices(String authorizedServices, String serverUserId) throws Exception {
        defineResource(getAuthorizedServicesProfileName(authorizedServices), "SERVER", "NONE", new Cleanup(), false);
        permitAccess(getAuthorizedServicesProfileName(authorizedServices), "SERVER", serverUserId, "READ");
    }

    /**
     * Revoke the given serverUserId's access to the given authorizedServices. Note that the
     * profile that gives access to the resource is NOT cleaned up.
     *
     * @param authorizedServices, e.g. SAFCRED, ZOSWLM, TXRRS, ZOSDUMP
     * @param serverUserId - the userId of the server
     */
    public static void revokeAuthorizedServices(String authorizedServices, String serverUserId) throws Exception {
        defineResource(getAuthorizedServicesProfileName(authorizedServices), "SERVER", "NONE", new Cleanup(), false);
        permitAccess(getAuthorizedServicesProfileName(authorizedServices), "SERVER", serverUserId, "NONE");
    }

    /**
     * Configure the penalty box (and more). This method does the following:
     *
     * 1) grants the serverId access to SAFCRED auth services
     * 2) defines and activates the BBGZDFLT profile in the APPL class and grants the userId
     * READ access
     * 3) creates the penalty box profile (BBG.SECPFX.<profilePrefix> in SERVER class) and grants
     * the serverId READ access.
     *
     * @param userId the user the server will authenticate
     * @param serverId the user ID of the server
     * @param profilePrefix profile prefix for the penalty box
     * @throws Exception
     */
    public static void setupPenaltyBox(String userId, String serverId, String profilePrefix, Cleanup c) throws Exception {
        setupPenaltyBox(Arrays.asList(userId), serverId, profilePrefix, c);
    }

    public static void setupPenaltyBox(List<String> userIdList, String serverId, String profilePrefix, Cleanup c) throws Exception {
        // Ensure the server has access to SAFCRED services.  This should be given
        // by default, we're just making sure.
        grantAuthorizedServices("SAFCRED", serverId);

        // Setup the APPL for initACEE authentication
        setupAPPL(profilePrefix, c);

        for (String userId : userIdList) {
            permitAccess(profilePrefix, "APPL", userId, "READ");
        }

        // Define the penalty box profile
        defineResource("BBG.SECPFX." + profilePrefix, "SERVER", "NONE", c, false);
        permitAccess("BBG.SECPFX." + profilePrefix, "SERVER", serverId, "READ");
    }

    /**
     * Define and activate the <profilePrefix> profile in the APPL class
     * and grant the given userId read access
     */
    private static void setupAPPL(String profilePrefix, Cleanup c) throws Exception {
        defineResource(profilePrefix, "APPL", "NONE", c, false);
        activateSafClass("APPL", c);
    }

    /**
     * Activate EJBROLE. The EJBROLE class should always be active on the system,
     * unless someone deactivates it to run a specific test. Therefore, we don't
     * clean up when someone activates this class, we just activate it and leave it
     * at that.
     */
    public static void activateEJBROLE() throws Exception {
        activateSafClass("EJBROLE", new Cleanup());
    }

    /**
     * Deactivate EJBROLE. This is typically done to run one or two tests that
     * show that we process correctly when EJBROLE is disabled. Since EJBROLE
     * should always be enabled on VICOM systems, we'll turn it back on during
     * cleanup.
     */
    public static void deactivateEJBROLE(Cleanup c) throws Exception {
        deactivateSafClass("EJBROLE", c);
    }

    /**
     * Issue RACMAP command to add a userid mapping
     *
     * @param user the user
     * @param userIdFilter the userid filter
     * @param registry the registry name
     * @throws Exception
     *
     */
    public static void addMap(String user, String userIdFilter, String filterLabel, String registry, Cleanup c, boolean ignoreCleanupError) throws Exception {
        // Attempt to remove the mapping first
        new CleanupMap(user, filterLabel, true).call();

        // Now add the mapping
        String safClass = "IDIDMAP";
        runTsoCmd("RACMAP MAP ID(" + user + ") USERDIDFILTER(NAME('" + userIdFilter + "')) WITHLABEL('" + filterLabel + "') REGISTRY(NAME('" + registry + "'))");
        c.addCleanup(new CleanupMap(user, filterLabel, ignoreCleanupError));

        runTsoCmd("SETROPTS CLASSACT(IDIDMAP) RACLIST(IDIDMAP)");
        c.addCleanup(new CleanupActiveSafClass("IDIDMAP"));

        refreshClass(safClass);
    }

    /**
     * Define a resource and refresh its class.
     * NOTE: This method is not for general use and it should only be used under special circumstances.
     * It is strongly recommended that you use alternate methods that use the cleanup
     * framework built into this class. Callers of this method must guarantee proper cleanup.
     *
     * @param resourceProfile The SAF resource to define.
     * @param safClass The SAF class under which the resource is to be defined.
     * @param uacc The universal access authority.
     *
     * @throws Exception
     */
    public static void defineResource(String resourceProfile, String safClass, String uacc) throws Exception {
        String command = "RDEFINE " + safClass + " " + resourceProfile + " UACC(" + uacc + ")";
        SAFConfigHelper.runTsoCmd(command);
        SAFConfigHelper.refreshClass(safClass);
    }

    /**
     * Deletes the specified resource under the specified class.
     * NOTE: When defining a resource, it is strongly recommended that you use alternate methods that use the cleanup
     * framework built into this class. If you follow that recommendation, you should not need to call this method.
     *
     * @param resourceProfile The SAF resource to delete.
     * @param safClass The SAF class under which the resource is defined.
     * @param ignoreError True to ignore resource not defined errors. False to report all errors.
     *
     * @throws Exception
     */
    public static void deleteResource(String resourceProfile, String safClass, boolean ignoreError) throws Exception {
        try {
            String command = "RDELETE " + safClass + " " + resourceProfile;
            SAFConfigHelper.runTsoCmd(command);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (!(ignoreError && msg.toUpperCase().contains("ICH12102I " + resourceProfile.toUpperCase() + " NOT DEFINED TO CLASS " + safClass.toUpperCase()))) {
                throw e;
            }
        }

        SAFConfigHelper.refreshClass(safClass);
    }

    /**
     * A cleanup helper. This makes sure that we clean up the mess we made, and not the mess
     * that we didn't make.
     */
    public static class Cleanup {
        /**
         * Keep a list of what needs to be done, in the order that it needs to be done.
         */
        private final Deque<Callable<Void>> cleanupStack = new LinkedList<Callable<Void>>();

        /**
         * Add something new to the cleanup list.
         */
        private final void addCleanup(Callable<Void> c) {
            cleanupStack.push(c);
        }

        /**
         * Do the cleanup
         *
         * @throws Exception If something goes wrong during cleanup, the first error will be
         *             thrown as an exception, after all cleanup steps have been completed.
         */
        public void doCleanup() throws Exception {
            final List<Exception> exceptions = new LinkedList<Exception>();

            for (Callable<Void> c : cleanupStack) {
                try {
                    try {
                        Log.info(SAFConfigHelper.c, "doCleanup", "Cleaning up: " + c.toString());
                    } catch (NoClassDefFoundError ncdfe) {
                        // Log class is not available in z/OS unit test environment.
                        System.out.println("SAFConfigHelper.doCleanup cleaning up: " + c.toString());
                    }
                    c.call();
                } catch (Throwable t) {
                    exceptions.add(new Exception("SAFCleanup caught while processing " + c.toString(), t));
                }
            }

            cleanupStack.clear();

            if (exceptions.size() > 0) {
                throw exceptions.get(0);
            }
        }
    }

    /**
     * Cleanup a group.
     */
    private static class CleanupGroup implements Callable<Void> {
        final String groupName;
        final boolean ignoreError;

        private CleanupGroup(String groupName, boolean ignoreCleanupError) {
            this.groupName = groupName;
            this.ignoreError = ignoreCleanupError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {

            // Delete all the users in the group first.  This may be 'more than intended' but
            // it gives the best change of success in deleting the group next.  Note we're
            // not ignoring the 'user not found' error since we're sure the user exists.
            List<String> usersInGroup = getUsersInGroup(groupName);
            for (String user : usersInGroup) {
                new CleanupUser(user, false).call();
            }

            // OK now try to delete the (hopefully empty) group.
            try {
                runTsoCmd("DELGROUP " + groupName);
            } catch (Exception e) {
                // Ignore the exception if it's complaining that the group
                // doesn't exist (i.e. "INVALID GROUP" is in the failure message).
                String msg = e.getMessage();
                if (ignoreError && msg.toUpperCase().contains("INVALID GROUP")) {
                    // ignore it.
                } else {
                    throw e;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupGroup: " + groupName + ", ignoreError = " + ignoreError);
        }
    }

    /**
     * Cleanup a user.
     */
    private static class CleanupUser implements Callable<Void> {
        final String userName;
        final boolean ignoreError;

        private CleanupUser(String userName, boolean ignoreCleanupError) {
            this.userName = userName;
            this.ignoreError = ignoreCleanupError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            // RACF doesn't like it when we delete a user that has RACMAPs associated
            // with it, so delete those first.
            try {
                Map<String, RACMAPData> racmapData = getRacmapData(userName);
                for (String label : racmapData.keySet()) {
                    new CleanupMap(userName, label, true).call();
                }
            } catch (Exception e) {
                // Ignore it.  We tried.
            }

            // Now proceed to delete the user.
            try {
                runTsoCmd("DELUSER " + userName);
            } catch (Exception e) {
                // Ignore the exception if it's complaining that the userid
                // doesn't exist.
                String msg = e.getMessage();
                if (ignoreError && msg.toUpperCase().contains("INVALID USERID")) {
                    // ignore it.
                } else {
                    throw e;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupUser: " + userName + ", ignoreError = " + ignoreError);
        }
    }

    /**
     * Cleanup a resource.
     */
    private static class CleanupResource implements Callable<Void> {
        final String safClass;
        final String profile;
        final boolean ignoreError;

        private CleanupResource(String safClass, String profile, boolean ignoreCleanupError) {
            this.safClass = safClass;
            this.profile = profile;
            this.ignoreError = ignoreCleanupError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            try {
                runTsoCmd("RDELETE " + safClass + " " + profile);
            } catch (Exception e) {
                // Ignore ICH12102I <dummy resource> NOT DEFINED TO CLASS EJBROLE
                String msg = e.getMessage();
                if (ignoreError && msg.toUpperCase().contains("ICH12102I " + profile.toUpperCase() + " NOT DEFINED TO CLASS " + safClass.toUpperCase())) {
                    // ignore it.
                } else {
                    throw e;
                }
            }
            refreshClass(safClass);
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupResource: CLASSS " + safClass + ", PROFILE = " + profile);
        }
    }

    /**
     * Deactivate a SAF class that we activated.
     */
    private static class CleanupActiveSafClass implements Callable<Void> {
        final String safClass;

        private CleanupActiveSafClass(String safClass) {
            this.safClass = safClass;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            runTsoCmd("SETROPTS NOCLASSACT(" + safClass + ")");
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupActiveSafClass: " + safClass);
        }
    }

    /**
     * Activates a SAF class that we deactivated.
     */
    private static class CleanupDeactiveSafClass implements Callable<Void> {
        final String safClass;

        private CleanupDeactiveSafClass(String safClass) {
            this.safClass = safClass;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            runTsoCmd("SETROPTS CLASSACT(" + safClass + ")");
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupDeactiveSafClass: " + safClass);
        }
    }

    /**
     * Cleanup a map.
     */
    private static class CleanupMap implements Callable<Void> {
        final String userId;
        final String filterLabel;
        final boolean ignoreError;

        private CleanupMap(String userId, String filterLabel, boolean ignoreCleanupError) {
            this.userId = userId;
            this.filterLabel = filterLabel;
            this.ignoreError = ignoreCleanupError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            try {
                String safClass = "IDIDMAP";
                runTsoCmd("RACMAP ID(" + userId + ") DELMAP(LABEL('" + filterLabel + "'))");
                refreshClass(safClass);
            } catch (Exception e) {
                // Ignore the exception if it's complaining that the userid
                // doesn't exist or if no information found for userid (Msg IRRW204I on map delete)
                String msg = e.getMessage();
                if ((ignoreError && msg.toUpperCase().contains("THE USER ID SPECIFIED IS NOT DEFINED TO RACF")) ||
                    (ignoreError && msg.contains("IRRW204I")) ||
                    (ignoreError && msg.contains("IRRW206I"))) {
                    // ignore it.
                } else {
                    throw e;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupMap: USER " + userId + ", ignoreError = " + ignoreError);
        }
    }

    /*
     * Create a keyring.
     */
    public static void createKeyring(String user, String keyringName, Cleanup c) throws Exception {
        // Attempt cleanup first, ignoring exceptions
        new CleanupCreateKeyring(user, keyringName, true).call();

        runTsoCmd("RACDCERT ID(" + user + ") ADDRING(" + keyringName + ")");
        c.addCleanup(new CleanupCreateKeyring(user, keyringName, false));
    }

    private static class CleanupCreateKeyring implements Callable<Void> {
        final String user;
        final String keyringName;
        final boolean ignoreError;

        private CleanupCreateKeyring(String user, String keyringName, boolean ignoreError) {
            this.user = user;
            this.keyringName = keyringName;
            this.ignoreError = ignoreError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            try {
                runTsoCmd("RACDCERT ID(" + user + ") DELRING(" + keyringName + ")");
            } catch (Exception e) {
                if (!ignoreError) {
                    throw e;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupCreateKeyring: user=" + user + " keyRingName=" + keyringName);
        }
    }

    /*
     * Generate a personal certificate.
     */
    public static void generateCertificate(String user, String label, int keySize, Cleanup c) throws Exception {
        // Attempt cleanup first, ignoring exceptions
        new CleanupGenerateCertificate(user, label, true).call();

        runTsoCmd("RACDCERT ID(" + user + ") GENCERT SUBJECTSDN(CN('" + user + label + "') O('IBM') C('US')) SIZE(" + keySize + ") WITHLABEL('" + label + "')");
        c.addCleanup(new CleanupGenerateCertificate(user, label, false));
    }

    private static class CleanupGenerateCertificate implements Callable<Void> {
        final String user;
        final String label;
        final boolean ignoreError;

        private CleanupGenerateCertificate(String user, String label, boolean ignoreError) {
            this.user = user;
            this.label = label;
            this.ignoreError = ignoreError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            try {
                runTsoCmd("RACDCERT ID(" + user + ") DELETE(LABEL('" + label + "')");
            } catch (Exception e) {
                if (!ignoreError) {
                    throw e;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupGenerateCertificate: user=" + user + " label=" + label);
        }
    }

    /*
     * Connect a personal certificate to a keyring.
     */
    public static void connectCertificate(String user, String label, String keyringName, Cleanup c) throws Exception {
        // Attempt cleanup first, ignoring exceptions
        new CleanupConnectCertificate(user, label, keyringName, true).call();

        runTsoCmd("RACDCERT ID(" + user + ") CONNECT(LABEL('" + label + "') RING(" + keyringName + ") DEFAULT USAGE(PERSONAL))");
        c.addCleanup(new CleanupConnectCertificate(user, label, keyringName, false));
    }

    private static class CleanupConnectCertificate implements Callable<Void> {
        final String user;
        final String label;
        final String keyringName;
        final boolean ignoreError;

        private CleanupConnectCertificate(String user, String label, String keyringName, boolean ignoreError) {
            this.user = user;
            this.label = label;
            this.keyringName = keyringName;
            this.ignoreError = ignoreError;
        }

        /** {@inheritDoc} */
        @Override
        public Void call() throws Exception {
            try {
                runTsoCmd("RACDCERT ID(" + user + ") REMOVE(LABEL('" + label + "') RING(" + keyringName + "))");
            } catch (Exception e) {
                if (!ignoreError) {
                    throw e;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new String("CleanupConnectCertificate: user=" + user + " label=" + label + " keyringName=" + keyringName);
        }
    }
}