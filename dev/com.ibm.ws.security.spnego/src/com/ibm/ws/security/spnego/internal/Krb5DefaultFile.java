/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * Represents security configurable options for SPNEGO web.
 */
public class Krb5DefaultFile {

    private static final TraceComponent tc = Tr.register(Krb5DefaultFile.class);

    @Trivial
    protected static enum OS {
        AIX, HP, LINUX, ISERIES, MAC, SOLARIS, WINDOWS, ZOS, UNKNOWN
    };

    @Trivial
    protected static enum KRB5 {
        CONF, KEYTAB
    };

    public static final String WINNT_INI = "c:/winnt/krb5.ini";
    public static final String WINNT_KEYTAB = "c:/winnt/krb5.keytab";

    public static final String WINDOWS_INI = "c:/windows/krb5.ini";
    public static final String WINDOWS_KEYTAB = "c:/windows/krb5.keytab";

    public static final String LINUX_CONF = "/etc/krb5.conf";
    public static final String LINUX_KEYTAB = "/etc/krb5.keytab";

    public static final String ISERIES_CONF = "/QIBM/UserData/OS400/NetworkAuthentication/krb5.conf";
    public static final String ISERIES_KEYTAB = "/QIBM/UserData/OS400/NetworkAuthentication/krb5.keytab";

    public static final String ZOS_AND_OTHER_UNIX_CONF = "/etc/krb5/krb5.conf";
    public static final String ZOS_AND_OTHER_UNIX_KEYTAB = "/etc/krb5/krb5.keytab";

    private static final String[] WIN_KRB5_INI = { WINNT_INI, WINDOWS_INI };
    private static final String[] WIN_KRB5_KEYTAB = { WINNT_KEYTAB, WINDOWS_KEYTAB };

    private static final String[] LINUX_KRB5_CONF = { LINUX_CONF };
    private static final String[] LINUX_KRB5_KEYTAB = { LINUX_KEYTAB };

    private static final String[] ISERIES_KRB5_CONF = { ISERIES_CONF };
    private static final String[] ISERIES_KRB5_KEYTAB = { ISERIES_KEYTAB };

    private static final String[] ZOS_AND_OTHER_UNIX_KRB5_CONF = { ZOS_AND_OTHER_UNIX_CONF };
    private static final String[] ZOS_AND_OTHER_UNIX_KRB5_KEYTAB = { ZOS_AND_OTHER_UNIX_KEYTAB };

    private String[] krb5ConfigFiles = null;
    private String[] krb5KeytabFiles = null;

    private final WsLocationAdmin locationAdmin;

    Krb5DefaultFile(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
        resolveDefaultKrb5FileWithPath();
    }

    public String getDefaultKrb5ConfigFile() {
        return getDefaultKrb5File(KRB5.CONF);
    }

    public String getDefaultKrb5KeytabFile() {
        return getDefaultKrb5File(KRB5.KEYTAB);
    }

    protected String getDefaultKrb5File(KRB5 file) {
        String[] fns = krb5ConfigFiles;
        if (file == KRB5.KEYTAB) {
            fns = krb5KeytabFiles;
        }

        List<String> fileNotFound = new ArrayList<String>();
        for (String fn : fns) {
            WsResource wsResource = locationAdmin.resolveResource(fn);
            if (wsResource != null && wsResource.exists()) {
                if (file == KRB5.KEYTAB) {
                    Tr.info(tc, "SPNEGO_USE_DEFAULT_KRB5_KEYTAB_FILE", fn);
                } else {
                    Tr.info(tc, "SPNEGO_USE_DEFAULT_KRB5_CONFIG_FILE", fn);
                }
                return fn;
            } else {
                fileNotFound.add(fn);
            }
        }
        if (file == KRB5.KEYTAB) {
            Tr.error(tc, "SPNEGO_DEFAULT_KRB5_KEYTAB_FILE_NOT_FOUND", fileNotFound);
        } else {
            Tr.error(tc, "SPNEGO_DEFAULT_KRB5_CONF_FILE_NOT_FOUND", fileNotFound);
        }

        return null;
    }

    protected void resolveDefaultKrb5FileWithPath() {
        OS os = getOperatingSystem();
        if (os == OS.WINDOWS) {
            krb5ConfigFiles = WIN_KRB5_INI;
            krb5KeytabFiles = WIN_KRB5_KEYTAB;
        } else if (os == OS.LINUX) {
            krb5ConfigFiles = LINUX_KRB5_CONF;
            krb5KeytabFiles = LINUX_KRB5_KEYTAB;
        } else if (os == OS.ISERIES) {
            krb5ConfigFiles = ISERIES_KRB5_CONF;
            krb5KeytabFiles = ISERIES_KRB5_KEYTAB;
        } else { //(os == OS.ZOS || os == OS.AIX || os == OS.HP || os == OS.SOLARIS || os == OS.MAC) 
            krb5ConfigFiles = ZOS_AND_OTHER_UNIX_KRB5_CONF;
            krb5KeytabFiles = ZOS_AND_OTHER_UNIX_KRB5_KEYTAB;
        }
    }

    protected OS getOperatingSystem() {
        String osName = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("os.name");
            }
        });

        if (osName != null) {
            return resolvePlatform(osName);
        }
        return OS.UNKNOWN;
    }

    /**
     * @param platform
     * @param osName
     * @return
     */
    protected OS resolvePlatform(String osName) {
        OS platform = OS.UNKNOWN;
        osName = osName.toLowerCase();
        if (osName.indexOf("win") != -1) {
            platform = OS.WINDOWS;
        } else if (osName.indexOf("os/390") != -1 || osName.indexOf("z/os") != -1 || osName.indexOf("zos") != -1) {
            platform = OS.ZOS;
        } else if (osName.indexOf("400") != -1) {
            platform = OS.ISERIES;
        } else if (osName.indexOf("linux") != -1) {
            platform = OS.LINUX;
        } else if (osName.indexOf("aix") != -1) {
            platform = OS.AIX;
        } else if (osName.indexOf("hp") != -1) {
            platform = OS.HP;
        } else if (osName.indexOf("solaris") != -1 || osName.indexOf("sun") != -1) {
            platform = OS.SOLARIS;
        } else if (osName.indexOf("mac os") != -1 || osName.indexOf("darwin") != -1) {
            platform = OS.MAC;
        }
        return platform;
    }
}
