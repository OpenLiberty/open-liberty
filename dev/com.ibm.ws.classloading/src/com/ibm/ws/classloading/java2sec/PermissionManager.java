/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.security.UnresolvedPermission;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.AuthPermission;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.url.URLStreamHandlerService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.security.PermissionsCombiner;
import com.ibm.ws.kernel.boot.security.WLPDynamicPolicy;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

public class PermissionManager implements PermissionsCombiner {
	
	/**
	 * The trace component
	 */
	private static final TraceComponent tc = Tr.register(PermissionManager.class);
	
	/**
	 * Class Loader
	 */
	private ClassLoadingService classLoadingService;

	/**
	 * These are the default filtered or restrictable permissions. These can be overridden by
	 * explicitly granting them in the server.xml
	 */
	private static Permission[] DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS = null;
	static {
		DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS = new Permission[4];
		DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS[0] = new RuntimePermission("exitVM");
		DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS[1] = new RuntimePermission("setSecurityManager");
		DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS[2] = new SecurityPermission("setPolicy");
		DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS[3] = new AuthPermission("setLoginConfiguration");
	}
	
	private static Permission[] DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS = null;
	static {
		DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS = new Permission[3];
		DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS[0] = new RuntimePermission("setSecurityManager");
		DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS[1] = new SecurityPermission("setPolicy");
		DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS[2] = new AuthPermission("setLoginConfiguration");
	}

	
    private boolean isServer = true;
    private boolean wsjarUrlStreamHandlerAvailable = false;
	
	/**
	 * The list of effective restrictable permissions. The effective permissions are merged from the
	 * default restrictable permissions, the granted permissions in server.xml and restrictable permissions
	 * in the server.xml
	 */
	private ArrayList<Permission> restrictablePermissions = new ArrayList<Permission>();
	
	/**
	 * The list of permissions granted in the server.xml
	 */
	private ArrayList<Permission> grantedPermissions = new ArrayList<Permission>();
	
	/**
	 * The binding key for permissions configurations.
	 */
	private static final String KEY_PERMISSION = "permission";

	private static final String INCORRECT_PERMISSION_CONFIGURATION = "INCORRECT_PERMISSION_CONFIGURATION";
	
	private static final String PERMISSION_CLASSNOTFOUND = "PERMISSION_CLASSNOTFOUND";

	private static final String SERVER_XML = "server.xml";
	
	private static final String CLIENT_XML = "client.xml";
	
	private String originationFile = null;

	/**
	 * The set of configured permissions.
	 */
    private final ConcurrentServiceReferenceSet<JavaPermissionsConfiguration> permissions 
            = new ConcurrentServiceReferenceSet<JavaPermissionsConfiguration>(KEY_PERMISSION);

	private Map<String, ArrayList<Permission>> codeBasePermissionMap = new HashMap<String, ArrayList<Permission>>();

	private Map<String, ArrayList<Permission>>  permissionXMLPermissionMap = new HashMap<String, ArrayList<Permission>>();

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        isServer = "server".equals(cc.getBundleContext().getProperty("wlp.process.type"));

    	permissions.activate(cc);
    	initializePermissions();
    	setAsDynamicPolicyPermissionCombiner(this);
    }

	private void setAsDynamicPolicyPermissionCombiner(PermissionsCombiner effectivePolicy) {
		Policy policy = AccessController.doPrivileged(new PrivilegedAction<Policy>() {
			@Override
			public Policy run() {
				return Policy.getPolicy();
			}
		});

		if (policy instanceof WLPDynamicPolicy) {
			((WLPDynamicPolicy) policy).setPermissionsCombiner(effectivePolicy);
		}
	}

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    	permissions.deactivate(cc);
    	clearPermissions();
    	setAsDynamicPolicyPermissionCombiner(null);
    }

    protected void setPermission(ServiceReference<JavaPermissionsConfiguration> permission) {
    	permissions.addReference(permission);
    }

    /*
     * Recreate permissions if the server is not shutting down.
     */
    protected synchronized void unsetPermission(ServiceReference<JavaPermissionsConfiguration> permission) {
    	permissions.removeReference(permission);
    	if (wsjarUrlStreamHandlerAvailable) {
    	    clearPermissions();
    	    initializePermissions();
    	}
    }
    
    protected synchronized void setWsjarURLStreamHandler(ServiceReference<URLStreamHandlerService> urlStreamHandlerServiceRef) {
    	wsjarUrlStreamHandlerAvailable = true;
    }

    protected synchronized void unsetWsjarURLStreamHandler(ServiceReference<URLStreamHandlerService> urlStreamHandlerServiceRef) {
    	wsjarUrlStreamHandlerAvailable = false;
    }
    
    protected synchronized void updatedConfiguration(ServiceReference<JavaPermissionsConfiguration> permission) {
    	permissions.removeReference(permission);
    	permissions.addReference(permission);
    	if (wsjarUrlStreamHandlerAvailable) {
    		clearPermissions();
        	initializePermissions();
    	}
    }
    
    private void clearPermissions() {
    	restrictablePermissions.clear();
    	grantedPermissions.clear();
    	codeBasePermissionMap.clear();
	}

    protected void setClassLoadingService(ClassLoadingService service) {
        classLoadingService = service;
    }

    protected void unsetClassLoadingService(ClassLoadingService service) {
        classLoadingService = null;
    }

	/**
	 * Initialize the permissions using the configuration and determine the effective permission
	 * by examining the grants and restrictions
	 */
    // @FFDCIgnore({ IllegalAccessException.class, InstantiationException.class, ClassNotFoundException.class, IllegalArgumentException.class,
    // 	InvocationTargetException.class, NoSuchMethodException.class, SecurityException.class})
    private void initializePermissions() {
    	// Set the default restrictable permissions
    	int count = 0;
    	if (tc.isDebugEnabled()) {
    		if (isServer)
    		    Tr.debug(tc, "running on server ");
    		else {
    			Tr.debug(tc, "running on client ");
    		}
    	}
    	if (isServer) {
    		count = DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS.length;
    		originationFile = SERVER_XML;
    	} else {
    		count = DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS.length;
    		originationFile = CLIENT_XML;
    	}
    	
    	for(int i = 0; i < count; i++) {
    		if (isServer) {
        		restrictablePermissions.add(DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS[i]);	
    		} else {
        		restrictablePermissions.add(DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS[i]);

    		}
    	}
    	
    	// Iterate through the configured Permissions.
    	if(permissions != null && !permissions.isEmpty()) {
    		Iterable<JavaPermissionsConfiguration> javaPermissions = permissions.services();
    		if(javaPermissions != null) {
    			for(JavaPermissionsConfiguration permission : javaPermissions) {
    				String permissionClass = String.valueOf(permission.get(JavaPermissionsConfiguration.PERMISSION));
    				String target = String.valueOf(permission.get(JavaPermissionsConfiguration.TARGET_NAME));
    				String action = String.valueOf(permission.get(JavaPermissionsConfiguration.ACTIONS));
    				String credential = String.valueOf(permission.get(JavaPermissionsConfiguration.SIGNED_BY));
    				String principalType = String.valueOf(permission.get(JavaPermissionsConfiguration.PRINCIPAL_TYPE));
    				String principalName = String.valueOf(permission.get(JavaPermissionsConfiguration.PRINCIPAL_NAME));
    				String codebase = normalize(String.valueOf(permission.get(JavaPermissionsConfiguration.CODE_BASE)));
    				
    				// Create the permission object
    				Permission perm = createPermissionObject(permissionClass, target, action, credential, principalType, principalName, originationFile);

    				boolean isRestriction = false;
    				// Is this a restriciton or a grant?
    				if (permission.get(JavaPermissionsConfiguration.RESTRICTION) != null) {
        				isRestriction = ((Boolean) permission.get(JavaPermissionsConfiguration.RESTRICTION)).booleanValue();    					
    				}
    				
    				if(isRestriction) {
    					// If this is a restriction
    					if(perm != null) {
   						    restrictablePermissions.add(perm);
    					}
    				}
    				else {
    					// If this is not a restriction, then set is a grant
    					if(perm != null) {
    						// if codebase is present, then set the permission on the shared lib classloader
    						if(codebase != null && !codebase.equalsIgnoreCase("null")) {
    							setCodeBasePermission(codebase, perm);
    						}
    						else
    						    grantedPermissions.add(perm);
    					}
    				}
    			}
    			
    			if(tc.isDebugEnabled()) {
        			Tr.debug(tc, "restrictablePermissions : " + restrictablePermissions);
        			Tr.debug(tc, "grantedPermissions : " + grantedPermissions);
    			}
    		}
    	}
    	
    	setSharedLibraryPermission();
    }

    private String normalize(String codebase) {
		if (codebase != null) {
			codebase = codebase.replace("\\", "/");
			codebase = codebase.replace("//", "/");
		}
		return codebase;
	}

	private void setCodeBasePermission(String codeBase, Permission permission) {
    	ArrayList<Permission> permissions = null;
    	
    	if(codeBasePermissionMap.containsKey(codeBase)) {
    		permissions = codeBasePermissionMap.get(codeBase);
    		permissions.add(permission);
    	}
    	else {
    		permissions = new ArrayList<Permission>();
    		permissions.add(permission);
    		codeBasePermissionMap.put(codeBase, permissions);
    	}
	}

	private void setSharedLibraryPermission() {
		Map<String, ProtectionDomain> protectionDomainMap = new HashMap<String, ProtectionDomain>();
		
		for(String codeBase : codeBasePermissionMap.keySet()) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "codeBase = " + codeBase);
			}
			ArrayList<Permission> permissions = codeBasePermissionMap.get(codeBase);
			
			if (tc.isDebugEnabled()) {
				for (int i = 0; i < permissions.size(); i++) {
					Tr.debug(tc, " permission: " + permissions.get(i));
				}				
			}
			
			ProtectionDomain protectionDomain = createProtectionDomain(codeBase, permissions);
			protectionDomainMap.put(codeBase, protectionDomain);
		}
		
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "protectionDomainMap.size = " + protectionDomainMap.size());
		}
		
		if(classLoadingService != null)
    	    classLoadingService.setSharedLibraryProtectionDomains(protectionDomainMap);
	}

	private ProtectionDomain createProtectionDomain(String codeBase, ArrayList<Permission> permissions) {
        PermissionCollection perms = new Permissions();

        if (!java2SecurityEnabled()) {
            perms.add(new AllPermission());
        }
        else {
        	for(Permission permission : permissions)
        	    perms.add(permission);
        }
        
        Certificate[] certs = null;
        CodeSource codeSource = null;
		try {
			codeSource = new CodeSource(new URL("wsjar:file:/"+codeBase), certs);
		} catch (MalformedURLException e) {
			if(tc.isDebugEnabled())
				Tr.debug(tc, "Unable to create code source for protection domain");
		}
		
        ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, perms);
        return protectionDomain;
	}

	/**
     * Create the permission object using the configuration data.
     * 
     * @param permissionClass
     * @param target
     * @param action
     * @param principalName 
     * @param principalType 
     * @param credential 
     * @return
     */
	public Permission createPermissionObject(String permissionClass, String target, String action, String credential, String principalType, String principalName, String fileName) {
		Permission permission = null;
		if ((target != null) && (target.equals("ALL FILES") || target.contains("ALL FILES"))) {
			target = "<<ALL FILES>>";
		}
		try {
			if(permissionClass != null && !permissionClass.equalsIgnoreCase("null")) {
				// If this is a PrivateCredentialPermission
				if(permissionClass.equalsIgnoreCase("javax.security.auth.PrivateCredentialPermission")) {
					if(target == null || target.equalsIgnoreCase("null")) {
						// Create target from the credential and principal details
						StringBuilder targetString = new StringBuilder();
						targetString.append(credential);
						targetString.append(" ");
						targetString.append(principalType);
						targetString.append(" \"");
						targetString.append(principalName);
						targetString.append("\"");
						
						permission = (Permission) Class.forName(permissionClass).getConstructor(String.class, String.class).newInstance(targetString.toString(), "read");
					}
					else {
						permission = (Permission) Class.forName(permissionClass).getConstructor(String.class, String.class).newInstance(target, "read");
					}
				}
				else {
					if(action == null || action.equalsIgnoreCase("null")) {
						if(target == null || target.equalsIgnoreCase("null")) {
							permission = (Permission) Class.forName(permissionClass).newInstance();
						}
						else {
							permission = (Permission) Class.forName(permissionClass).getConstructor(String.class).newInstance(target);
						}
					}
					else {
						permission = (Permission) Class.forName(permissionClass).getConstructor(String.class, String.class).newInstance(target, action);
					}
				}
			}
		} catch (Exception e) {
			if(tc.isWarningEnabled()) {
				String rootCause = null;
				
				if(e.getCause() != null)
					rootCause = e.getCause().getClass().getName() + "[" + e.getCause().getMessage() + "]";
				else if(e.getMessage() != null)
					rootCause = e.getClass().getName() + "[" + e.getMessage() + "]";

				if(rootCause == null)
					rootCause = "unknown reasons";
				if (e instanceof java.lang.ClassNotFoundException) {
	            	Tr.warning(tc, PERMISSION_CLASSNOTFOUND, permissionClass, rootCause, fileName);
				}
				else {
					Tr.warning(tc, INCORRECT_PERMISSION_CONFIGURATION, permissionClass, rootCause, fileName);
				}
			}
            if (e instanceof java.lang.ClassNotFoundException) {
            	permission = new UnresolvedPermission(permissionClass, target, action, null);
            }
		}

		return permission;
	}

	/**
	 * Return the effective restrictable permissions
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Permission> getRestrictablePermissions() {
		return (ArrayList<Permission>) restrictablePermissions.clone();
	}

	/**
	 * Return the effective granted permissions.
	 * 
	 * @param codeBase - The code base of the code to obtain the effective permissions for.
	 * @return the effective granted permissions.
	 */
	public ArrayList<Permission> getEffectivePermissions(String codeBase) {
		List<Permission> emptyPermissions = Collections.emptyList();
		return getEffectivePermissions(emptyPermissions, codeBase);
	}

	/**
	 * Return the effective granted permissions.
	 * 
	 * @param permissions - The permissions granted in the permissions.xml of the web component or static policy permissions.
	 * @param codeBase - The code base of the code to obtain the effective permissions for.
	 * @return the effective granted permissions
	 */
	public ArrayList<Permission> getEffectivePermissions(List<Permission> permissions, String codeBase) {
		ArrayList<Permission> effectivePermissions = new ArrayList<Permission>();
		
		// Add the granted permissions to an arraylist
		effectivePermissions.addAll(grantedPermissions);

		// Add the codebase specific permissions
		codeBase = normalize(codeBase);
		if(codeBasePermissionMap.containsKey(codeBase)) {
		    effectivePermissions.addAll(codeBasePermissionMap.get(codeBase));
		}

		// Add permissions.xml permissions
		if (permissionXMLPermissionMap.containsKey(codeBase)) {
			effectivePermissions.addAll(permissionXMLPermissionMap.get(codeBase));
		}

		// Iterate over the permissions and only add those that are not restricted
		for(Permission permission : permissions) {
			if(!isRestricted(permission)) {
				effectivePermissions.add(permission);
			}
		}

		return effectivePermissions;
	}

	/**
	 * Combine the static permissions with the server.xml and permissions.xml permissions, removing any restricted permission.
	 * This is called back from the dynamic policy to obtain the permissions for the JSP classes.
	 */
	@Override
	public PermissionCollection getCombinedPermissions(PermissionCollection staticPolicyPermissionCollection, CodeSource codesource) {
		Permissions effectivePermissions = new Permissions();
		List<Permission> staticPolicyPermissions = Collections.list(staticPolicyPermissionCollection.elements());
		String codeBase = codesource.getLocation().getPath(); // TODO: This should be using the CodeSource itself to compare with existing code sources
		if (codeBase.startsWith("/")) {
			codeBase =  codeBase.substring(1);
		}
		ArrayList<Permission> permissions = getEffectivePermissions(staticPolicyPermissions, codeBase);
		
		for (Permission permission : permissions) {
			effectivePermissions.add(permission);
		}

		return effectivePermissions;
	}

	/**
	 * Check if this is a restricted permission.
	 * 
	 * @param permission
	 * @return <code>true</code> if the permission is restricted
	 */
	private boolean isRestricted(Permission permission) {
		for(Permission restrictedPermission : restrictablePermissions) {
			if(restrictedPermission.implies(permission))
				return true;
		}
		return false;
	}

    private boolean java2SecurityEnabled() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            return true;
        else
            return false;
    }

    /**
	 * Adds a permission from the permissions.xml file for the given code base.
	 * 
	 * @param codeBase - The code base of the code the specified permission was granted to.
	 * @param permissions - The permissions granted in the permissions.xml of the application.
	 * @return the effective granted permissions
	 */
	public void addPermissionsXMLPermission(String codeBase, Permission permission) {
		ArrayList<Permission> permissions = null;

		codeBase = normalize(codeBase);
		if (!isRestricted(permission)) {
			if (permissionXMLPermissionMap.containsKey(codeBase)) {
				permissions = permissionXMLPermissionMap.get(codeBase);
				permissions.add(permission);
			} else {
				permissions = new ArrayList<Permission>();
				permissions.add(permission);
				permissionXMLPermissionMap.put(codeBase, permissions);
			}
		}
	}

}
