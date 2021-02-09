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

package com.ibm.ws.security.audit.fat.common.tooling;

public class AuditMessageConstants {

	
	public static final String CWWKS0008I_SECURITY_SERVICE_READY = "CWWKS0008I";

	public static final String AUDIT_FEATURE_PACKAGE = "audit-1.0";
	public static final String CWWKF0012I_AUDIT_FEATURE_INSTALLED = "CWWKF0012I.*" + AUDIT_FEATURE_PACKAGE;  // wild card search will not work in lite buckets during language tests, see ReadOnlyRegistryAuditTest for alternate usage
	public static final String CWWKF0013I_AUDIT_FEATURE_REMOVED = "CWWKF0013I.*" + AUDIT_FEATURE_PACKAGE;
	
	public static final String CWWKS5850I_AUDIT_SERVICE_STARTING = "CWWKS5850I";
	public static final String CWWKS5851I_AUDIT_SERVICE_READY = "CWWKS5851I";
	
	public static final String CWWKS5806I_AUDIT_FILEHANDLER_STOPPED = "CWWKS5806I";
	public static final String CWWKS5804I_AUDIT_FILEHANDLER_STARTING = "CWWKS5804I";
	public static final String CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY = "CWWKS5805I";
	
    public static final String CWWKF0008I_FEATURE_UPDATE_COMPLETE = "CWWKF0008I: Feature update completed in ";
    public static final String CWWKE0036I_SERVER_STOPPED = "CWWKE0036I";
    
	public static final String CWWKS5852I_AUDIT_SERVICE_STOPPED = "CWWKS5852I";
	public static final String CWWKS5806I_AUDIT_FILEHANDLER_SERVICE_STOPPED = "CWWKS5806I";

	public static final String CWWKG0032W_AUDIT_UNEXPECTED_CONFIG_VALUE = "CWWKG0032W";

	public static final String CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED = "CWWKE0701E";
	
	public static final String CWWKE0700W_AUDIT_COLLECTOR_MANAGER_FAILED = "CWWKE0700W";

	public static final String CWWKS5853E_AUDIT_HANDLER_INVALID_EVENTNAME = "CWWKS5853E";
	public static final String CWWKS5854E_AUDIT_HANDLER_INVALID_OUTCOME = "CWWKS5854E";
	public static final String CWWKS5855E_AUDIT_INVALID_MISSING_EVENT = "CWWKS5855E";
	
	public static final String CWWKS5807E_AUDIT_HANDLER_INVALID_ENCRYPT_CONFIG = "CWWKS5807E";
	public static final String CWWKS5808E_AUDIT_HANDLER_INVALID_SIGN_CONFIG = "CWWKS5808E";
	public static final String CWWKS5809E_AUDIT_HANDLER_ENCRYPT_INIT_FAILED = "CWWKS5809E";
	public static final String CWWKS5810E_AUDIT_HANDLER_SIGN_INIT_FAILED = "CWWKS5810E";
	
	
	public static final String CWPKI0803A_SSL_DEFAULT_KEYSTORE_AND_CERT_CREATED = "CWPKI0803A";
	public static final String CWPKI0033E_KEYSTORE_FAILED_TO_LOAD_ERROR = "CWPKI0033E";
	public static final String CWPKI0809W_KEYSTORE_FAILED_TO_LOAD_SSL_CANNOT_INIT = "CWPKI0809W";

}
