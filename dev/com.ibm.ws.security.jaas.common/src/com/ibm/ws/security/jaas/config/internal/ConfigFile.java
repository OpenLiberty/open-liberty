/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.config.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.Security;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.security.auth.login.AppConfigurationEntry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class parses JAAS login configuration URL files and stores this information
 * in memory. The com.ibm.ws.security.auth.login.Configuration
 * class creates an instance of ConfigFile and delegates method calls to it.
 * This class is granted only package level access to prevent it from being used
 * by non-authorized classes. The only consumer of this class should be
 * com.ibm.ws.security.auth.login.Configuration.
 * 
 */
class ConfigFile extends Parser {
    private static final TraceComponent tc = Tr.register(ConfigFile.class);
    private static final String AUTHPROP = "java.security.auth.login.config";
    private HashMap _fileMap;
    private String _loginFile;
    private boolean singleLogInFile = false; // $A7
    private java.security.PrivilegedExceptionAction openLoginFileAction = null; // $A7

    class OpenFileAction implements java.security.PrivilegedExceptionAction {
        OpenFileAction(String name) {
            filename = "file:///" + name;
        }

        @Override
        public Object run() throws MalformedURLException, IOException {
            URL url = new URL(filename);
            return new BufferedReader(new InputStreamReader(url.openStream()));
        }

        private final String filename;
    }

    ConfigFile(String fn) {
        _loginFile = fn;
        if (_loginFile.charAt(0) == '=') {
            singleLogInFile = true;
            _loginFile = _loginFile.substring(1);
        }

        openLoginFileAction = new OpenFileAction(_loginFile);

        buildFileEntry();
    }

    public Map<String, List<AppConfigurationEntry>> getFileMap()
    {
        return _fileMap;
    }

    /**
     * This method returns an AppConfigurationEntry from internal storage based on the input key or alias.
     * 
     * @param config String that is used as the key or alias to fetch AppConfigurationEntry information for.
     * @return The AppConfigurationEntry for the supplied alias.
     * @see com.ibm.ws.security.auth.login.Configuration
     * @see javax.security.auth.login.Configuration
     * @see javax.security.auth.login.AppConfigurationEntry
     */
    AppConfigurationEntry[] getAppConfigurationEntry(String config)
    {
        Vector entry = null;
        if ((_fileMap != null) && (_fileMap.size() != 0)) {
            entry = (Vector) _fileMap.get(config);
        }

        if (entry == null || entry.size() == 0)
        {
            return null;
        }

        int appSize = entry.size();
        AppConfigurationEntry appConfig[] = new AppConfigurationEntry[appSize];
        Iterator it = entry.iterator();
        for (int i = 0; it.hasNext(); i++)
        {
            AppConfigurationEntry appCfg = (AppConfigurationEntry) it.next();
            appConfig[i] = new AppConfigurationEntry(appCfg.getLoginModuleName(), appCfg.getControlFlag(), appCfg.getOptions());
        }
        return appConfig;
    }

    /**
     * Parses one or more JAAS login configuration files and stores this configuration information
     * in memory.
     */
    private void buildFileEntry() {

        final HashMap result = new HashMap();
        Reader reader = null;
        boolean error1 = false;

        try {
            if (openLoginFileAction != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JAAS login configuration file: " + _loginFile + " singleLogInFile: " + singleLogInFile);
                }
                reader = (Reader) AccessController.doPrivileged(openLoginFileAction);
                Map<String, List<AppConfigurationEntry>> mapresult = parse(reader);
                if (mapresult != null) {
                    result.putAll(mapresult);
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JAAS login configuration file: " + _loginFile + " processed");
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "No JAAS login configuration file specified as " + AUTHPROP + " property");
                }
            }
        } catch (java.security.PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof MalformedURLException) {
                Tr.error(tc, "security.jaas.open.URL", _loginFile, e);
            } else if (e instanceof IOException) {
                Tr.error(tc, "security.jaas.create.URL", _loginFile, e);
            }
            error1 = true;
        } catch (IOException ioe) {
            Tr.error(tc, "security.jaas.create.URL", _loginFile, ioe);
            error1 = true;
        } catch (ParserException pe) {
            Tr.error(tc, "security.jaas.parser.URL", _loginFile, pe);
            error1 = true;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // It is OK to ignore this exception, since we are closing the reader
                }
            }
        }

        // Note that a login config file may be specified as
        // java.security.auth.login.config="=some file path". The second '='
        // sign is signficant.  It means only use the specified login file
        // and ingore others that may appear in java.security.
        Boolean error2 = Boolean.FALSE;

        if (!singleLogInFile) {
            // ckmason 5/9/02, the following doPrivilege is necessary because there may
            // be user code on the call stack that calls Configuration.refresh().  The only
            // permission required of the caller is javax.security.auth.AuthPermission("refreshLoginConfiguration").
            // CTS opened defect 128889 because of an unexpected ACE, e.g.:
            // SVR-ERROR: Configuration.refresh() caught SecurityException.
            // access denied (java.security.SecurityPermission getProperty.login.url.1)

            // $A8: Use ibm AccessController.doPrivileged for performance reasons
            error2 = (Boolean) AccessController.doPrivileged(new java.security.PrivilegedAction() {
                @Override
                public Object run() {
                    int i = 1;
                    String iLoginFile2 = null;
                    URL myUrl2 = null;
                    Reader retrdr2 = null;
                    boolean error = false;

                    while ((iLoginFile2 = Security.getProperty("login.url." + i)) != null) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "JAAS login configuration file (" + i + "): " + iLoginFile2);
                        }

                        try {
                            myUrl2 = new URL(iLoginFile2);
                            retrdr2 = new BufferedReader(new InputStreamReader(myUrl2.openStream()));
                            Map<String, List<AppConfigurationEntry>> tmpMap = parse(retrdr2);
                            if (tmpMap != null) {
                                result.putAll(tmpMap);
                            }

                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "JAAS login configuration file (" + i + "): " + iLoginFile2 + " processed");
                            }
                        } catch (MalformedURLException mue) {
                            Tr.error(tc, "security.jaas.open.URL", iLoginFile2, mue);
                            error = true;
                        } catch (IOException ioe) {
                            Tr.error(tc, "security.jaas.create.URL", iLoginFile2, ioe);
                            error = true;
                        } catch (ParserException pe) {
                            Tr.error(tc, "security.jaas.parser.URL", iLoginFile2, pe);
                            error = true;
                        }
                        i += 1;
                    }

                    return Boolean.valueOf(error);
//                    return new Boolean(error);
                }
            });
        }

        if ((error1 == false) && (error2.booleanValue() == false)) {
            _fileMap = result; // only replace where there is no error
        }
    }

    void refresh()
    {
        clearFileEntry();
        buildFileEntry();
    }

}
