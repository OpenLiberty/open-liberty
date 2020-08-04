/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.kerberos.auth;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSCredential;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.kerberos.auth.internal.LRUCache;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = KerberosService.class,
           configurationPid = "com.ibm.ws.security.kerberos.auth.KerberosService",
           immediate = true,
           property = "service.vendor=IBM")
public class KerberosService {

    private static final TraceComponent tc = Tr.register(KerberosService.class);

    private static final String KRB5_CONFIG_PROPERTY = "java.security.krb5.conf";

    static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private Path keytab;
    private Path configFile;
    private final LRUCache subjectCache = new LRUCache(100);

    @Activate
    protected void activate(ComponentContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "activate", ctx.getProperties());
        }

        String rawKeytab = (String) ctx.getProperties().get("keytab");
        String rawConfigFile = (String) ctx.getProperties().get("configFile");

        if (rawKeytab != null) {
            keytab = Paths.get(rawKeytab);
            if (keytab.toFile().exists()) {
                if (tc.isInfoEnabled()) {
                    Tr.info(tc, "KRB5_FILE_FOUND_CWWKS4346I", "keytab", keytab.toAbsolutePath());
                }
            } else {
                Tr.error(tc, "KRB5_FILE_NOT_FOUND_CWWKS4345E", "keytab", "<kerberos>", keytab.toAbsolutePath());
            }
        }
        if (rawConfigFile != null) {
            configFile = Paths.get(rawConfigFile);
            String originalConfigFile = priv.getProperty(KRB5_CONFIG_PROPERTY);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting system property " + KRB5_CONFIG_PROPERTY + "=" + configFile.toAbsolutePath().toString() +
                             "  Previous value was: " + originalConfigFile);
            }
            priv.setProperty(KRB5_CONFIG_PROPERTY, configFile.toAbsolutePath().toString());

            if (configFile.toFile().exists()) {
                if (tc.isInfoEnabled()) {
                    Tr.info(tc, "KRB5_FILE_FOUND_CWWKS4346I", "configFile", configFile.toAbsolutePath());
                }
            } else {
                Tr.error(tc, "KRB5_FILE_NOT_FOUND_CWWKS4345E", "configFile", "<kerberos>", configFile.toAbsolutePath());
            }
        }
    }

    public Path getConfigFile() {
        return configFile;
    }

    /**
     * @return The path of the keytab file, or null if unspecified
     */
    public Path getKeytab() {
        return keytab;
    }

    /**
     * Checks the Subject cache for an existing Subject matching the supplied principal.
     * A simple LRU cache is used to store cache Subjects
     * If a valid subject is not found in the cache, then a new Kerberos login is performed and
     * the resulting Subject is cached.
     *
     * @param principal The principal to obtain a subject for
     * @return A valid subject for the supplied principal
     */
    public Subject getOrCreateSubject(String principal) throws LoginException {
        KerberosPrincipal krb5Principal = new KerberosPrincipal(principal);
        Subject cachedSubject = subjectCache.get(krb5Principal);
        if (cachedSubject != null) {
            return cachedSubject;
        }

        Subject createdSubject = doKerberosLogin(principal);

        subjectCache.put(krb5Principal, createdSubject);
        return createdSubject;
    }

    private Subject doKerberosLogin(String principal) throws LoginException {
        Subject subject = new Subject();
        Krb5LoginModuleWrapper krb5 = new Krb5LoginModuleWrapper();
        Map<String, String> options = new HashMap<String, String>();
        Map<String, Object> sharedState = new HashMap<String, Object>();

        options.put("isInitiator", "true");
        options.put("refreshKrb5Config", "true");
        options.put("doNotPrompt", "true");
        options.put("useKeyTab", "true");
        // If no keytab path specified, still set useKeyTab=true because then the
        // default JDK or default OS locations will be checked
        if (keytab != null) {
            options.put("keyTab", keytab.toAbsolutePath().toString());
        }
        options.put("principal", principal);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            options.put("debug", "true");
            Tr.debug(tc, "All kerberos config properties are: " + options);
        }

        krb5.initialize(subject, null, sharedState, options);
        krb5.login();
        krb5.commit();

        // If the created Subject does not have a GSSCredential, then create one and
        // associate it with the Subject
        Set<GSSCredential> gssCreds = subject.getPrivateCredentials(GSSCredential.class);
        if (gssCreds == null || gssCreds.size() == 0) {
            GSSCredential gssCred = SubjectHelper.createGSSCredential(subject);
            if (System.getSecurityManager() == null) {
                subject.getPrivateCredentials().add(gssCred);
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        subject.getPrivateCredentials().add(gssCred);
                        return null;
                    }
                });
            }
        }

        return subject;
    }

}
