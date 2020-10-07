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
package com.ibm.oauth.core.api.audit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;

/**
 * OAuth audit handler that writes the audit entries to local file in simple
 * format. This audit handler may be configured into the component by suppling
 * the classname
 * <code>com.ibm.oauth.core.api.audit.SimpleFileOAuthAuditHandler</code> for the
 * configuration property: <code>oauth20.audithandler.classname</code>. If you
 * choose to use this audit handler class your configuration provider must also
 * provide a value for the configuration property:
 * <code>simpleFileAuditHandler.filename</code> with the filename of the audit
 * file to write to. If your application environment supports more than one
 * instance of the component, each component instance should have a unique
 * filename. <br>
 * Here is an example of an audit record from successfully issuing and
 * authorization code: <br>
 * 
 * <pre>
 * 2011-11-03T04:10:04Z
 * {{name: request_type type: urn:ibm:names:oauth:request values: [authorization]},{name: username type: urn:ibm:names:oauth:request values: [shane]},{name: client_id type: urn:ibm:names:query:param values: [key]},{name: redirect_uri type: urn:ibm:names:query:param values: [https://localhost:9443/oauthclient/redirect.jsp]},{name: response_type type: urn:ibm:names:query:param values: [code]},{name: state type: urn:ibm:names:query:param values: [2LPcdVTBwJ9WHOrMEM8r]},{name: scope type: urn:ibm:names:oauth:request values: [scope1,scope2]},{name: code type: urn:ibm:names:oauth:response:attribute values: [JngBWAUEroFbuwvjB36QxhHD9uJQDQ]},{name: authorization_code_id type: urn:ibm:names:oauth:response:metadata values: [JngBWAUEroFbuwvjB36QxhHD9uJQDQ]},{name: state_id type: urn:ibm:names:oauth:state values: [ff4afde8-ca73-4fd5-a86e-3b693a7b9d9e]},{name: state type: urn:ibm:names:oauth:response:attribute values: [2LPcdVTBwJ9WHOrMEM8r]}}
 * </pre>
 * 
 * <br>
 * Here is an example of an audit record from an error at the token endpoint
 * when the client secret was wrong: <br>
 * 
 * <pre>
 * 2011-11-03T04:10:13Z
 * invalid_client       An invalid client secret was presented for client: key
 * {{name: request_type type: urn:ibm:names:oauth:request values: [access_token]},{name: client_id type: urn:ibm:names:oauth:param values: [key]},{name: client_secret type: urn:ibm:names:body:param values: [secret2]},{name: redirect_uri type: urn:ibm:names:body:param values: [https://localhost:9443/oauthclient/redirect.jsp]},{name: grant_type type: urn:ibm:names:body:param values: [authorization_code]},{name: code type: urn:ibm:names:body:param values: [B2xHPjWgs1ajBy1B4788cJyeb1wGlM]},{name: client_id type: urn:ibm:names:body:param values: [key]}}
 * </pre>
 */
public class SimpleFileOAuthAuditHandler implements OAuthAuditHandler {
    final static String CLASS = SimpleFileOAuthAuditHandler.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    final static SimpleDateFormat DATE_FORMAT;
    final static String SDF_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    final static String TZ_UTC = "UTC";

    final static String DEFAULT_AUDIT_ENTRY_FORMAT = "%1$s\n%2$s";
    final static String ERROR_AUDIT_ENTRY_FORMAT = "%1$s\n%3$-20s %4$s\n%2$s";

    /**
     * A configuration attribute name which must be found in the component
     * configuration and point to the string filename that will be used to write
     * audit records.
     */
    public static final String FILENAME = "simpleFileAuditHandler.filename";

    static {
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
        sdf.setLenient(false);
        sdf.setTimeZone(TimeZone.getTimeZone(TZ_UTC));
        DATE_FORMAT = sdf;
    }

    private boolean initialized = false;
    private PrintWriter output = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.oauth.core.api.audit.OAuthAuditHandler#init(com.ibm.oauth.core
     * .api.config.OAuthComponentConfiguration)
     */
    public void init(OAuthComponentConfiguration config) {
        String filename = config.getConfigPropertyValue(FILENAME);
        if (filename != null) {
            File actualFile = new File(filename);
            try {
                output = new PrintWriter(new BufferedWriter(new FileWriter(
                        actualFile, true)));
                initialized = true;
            } catch (IOException e) {
                _log.warning("Cannot get writer for " + filename
                        + ", audit handler disabled");
            }
        } else {
            _log.warning(FILENAME + " config is null, audit handler disabled");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.oauth.core.api.audit.OAuthAuditHandler#writeEntry(com.ibm.oauth
     * .core.api.audit.OAuthAuditEntry)
     */
    public void writeEntry(OAuthAuditEntry entry) throws OAuthException {
        if (initialized) {
            String text = "";
            if (entry.getError() == null) {
                text = String.format(DEFAULT_AUDIT_ENTRY_FORMAT, DATE_FORMAT
                        .format(entry.getTimeStamp()), entry.getAttributes());
            } else {
                OAuthException error = entry.getError();
                text = String.format(ERROR_AUDIT_ENTRY_FORMAT, DATE_FORMAT
                        .format(entry.getTimeStamp()), entry.getAttributes(),
                        error.getError(), error.getMessage());
            }
            synchronized (output) {
                output.println(text);
                output.flush();
            }
        }
    }
}
