/*******************************************************************************
 * Copyright (c) 2015, 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;

import java.io.File;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;

import javax.security.auth.AuthPermission;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
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
    
    private BundleContext bundleContext;

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
    
    private static String os_name = System.getProperty("os.name");
    private static String os_version = System.getProperty("os.version");

    

    private String originationFile = null;

    /**
     * The set of configured permissions.
     */
    private final ConcurrentServiceReferenceSet<JavaPermissionsConfiguration> permissions = new ConcurrentServiceReferenceSet<JavaPermissionsConfiguration>(KEY_PERMISSION);

    private Map<String, ArrayList<Permission>> codeBasePermissionMap = new HashMap<String, ArrayList<Permission>>();

    private Map<String, ArrayList<Permission>> permissionXMLPermissionMap = new HashMap<String, ArrayList<Permission>>();

    @Activate
    protected void activate(ComponentContext cc) {
        bundleContext = cc.getBundleContext();
        isServer = "server".equals(bundleContext.getProperty("wlp.process.type"));

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
            if (isServer) {
                Tr.debug(tc, "running on server ");
            } else {
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

        for (int i = 0; i < count; i++) {
            if (isServer) {
                restrictablePermissions.add(DEFAULT_SERVER_RESTRICTABLE_PERMISSIONS[i]);
            } else {
                restrictablePermissions.add(DEFAULT_CLIENT_RESTRICTABLE_PERMISSIONS[i]);
            }
        }

        // Iterate through the configured Permissions.
        if (permissions != null && !permissions.isEmpty()) {
            Iterable<JavaPermissionsConfiguration> javaPermissions = permissions.services();
            if (javaPermissions != null) {
                for (JavaPermissionsConfiguration permission : javaPermissions) {
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

                    if (isRestriction) {
                        // If this is a restriction
                        if (perm != null) {
                            restrictablePermissions.add(perm);
                        }
                    } else {
                        // If this is not a restriction, then set is a grant
                        if (perm != null) {
                            // if codebase is present, then set the permission on the shared lib classloader
                            if (codebase != null && !codebase.equalsIgnoreCase("null")) {
                                setCodeBasePermission(codebase, perm);
                            } else {
                                grantedPermissions.add(perm);
                            }
                        }
                    }
                }

                if (tc.isDebugEnabled()) {
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

        if (codeBasePermissionMap.containsKey(codeBase)) {
            permissions = codeBasePermissionMap.get(codeBase);
            permissions.add(permission);
        } else {
            permissions = new ArrayList<Permission>();
            permissions.add(permission);
            codeBasePermissionMap.put(codeBase, permissions);
        }
    }

    private void setSharedLibraryPermission() {
        Map<String, ProtectionDomain> protectionDomainMap = new HashMap<String, ProtectionDomain>();

        for (String codeBase : codeBasePermissionMap.keySet()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "codeBase = " + codeBase);
            }
            ArrayList<Permission> permissions = codeBasePermissionMap.get(codeBase);

            if (tc.isDebugEnabled()) {
                for (int i = 0; i < permissions.size(); i++) {
                    Tr.debug(tc, " permission: " + permissions.get(i));
                }
            }
            
            CodeSource codeSource = createCodeSource(codeBase);
            ProtectionDomain protectionDomain = createProtectionDomain(codeSource, permissions);
            protectionDomainMap.put(codeBase, protectionDomain);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "protectionDomainMap.size = " + protectionDomainMap.size());
        }

        if (classLoadingService != null) {
            classLoadingService.setSharedLibraryProtectionDomains(protectionDomainMap);
        }
    }

    private CodeSource createCodeSource(String codeBase) {
        Certificate[] certs = null;
        CodeSource codeSource = null;
        try {
            //if (codeBase != null) {
            //    codeBase = codeBase.replace(":/", "/");
            //}
            
            codeSource = new CodeSource(new URL("wsjar:file:/" + codeBase), certs);
        } catch (MalformedURLException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to create code source for protection domain");
            }
        }
        return codeSource;
    }

    private ProtectionDomain createProtectionDomain(CodeSource codeSource, ArrayList<Permission> permissions) {
        PermissionCollection perms = new Permissions();

        if (!java2SecurityEnabled()) {
            perms.add(new AllPermission());
        } else {
            for (Permission permission : permissions) {
                perms.add(permission);
            }
        }

        return new ProtectionDomain(codeSource, perms);
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
            if (permissionClass != null && !permissionClass.equalsIgnoreCase("null")) {
                // If this is a PrivateCredentialPermission
                if (permissionClass.equalsIgnoreCase("javax.security.auth.PrivateCredentialPermission")) {
                    if (target == null || target.equalsIgnoreCase("null")) {
                        // Create target from the credential and principal details
                        StringBuilder targetString = new StringBuilder();
                        targetString.append(credential);
                        targetString.append(" ");
                        targetString.append(principalType);
                        targetString.append(" \"");
                        targetString.append(principalName);
                        targetString.append("\"");

                        permission = (Permission) getPermissionClass(permissionClass).getConstructor(String.class, String.class).newInstance(targetString.toString(), "read");
                    } else {
                        permission = (Permission) getPermissionClass(permissionClass).getConstructor(String.class, String.class).newInstance(target, "read");
                    }
                } else {
                    if (action == null || action.equalsIgnoreCase("null")) {
                        if (target == null || target.equalsIgnoreCase("null")) {
                            permission = (Permission) getPermissionClass(permissionClass).newInstance();
                        } else {
                            permission = (Permission) getPermissionClass(permissionClass).getConstructor(String.class).newInstance(target);
                        }
                    } else {
                        permission = (Permission) getPermissionClass(permissionClass).getConstructor(String.class, String.class).newInstance(target, action);
                    }
                }
            }
        } catch (Exception e) {
            if (tc.isWarningEnabled()) {
                String rootCause = null;

                if (e.getCause() != null) {
                    rootCause = e.getCause().getClass().getName() + "[" + e.getCause().getMessage() + "]";
                } else if (e.getMessage() != null) {
                    rootCause = e.getClass().getName() + "[" + e.getMessage() + "]";
                }

                if (rootCause == null) {
                    rootCause = "unknown reasons";
                }
                if (e instanceof java.lang.ClassNotFoundException) {
                    Tr.warning(tc, PERMISSION_CLASSNOTFOUND, permissionClass, rootCause, fileName);
                } else {
                    Tr.warning(tc, INCORRECT_PERMISSION_CONFIGURATION, permissionClass, rootCause, fileName);
                }
            }
            if (e instanceof java.lang.ClassNotFoundException) {
                permission = new UnresolvedPermission(permissionClass, target, action, null);
            }
        }

        return permission;
    }

    private Class<?> getPermissionClass(String className) throws ClassNotFoundException {
        Class<?> permissionClass = getPermissionClassUsingBundleClassLoader(className);

        if (permissionClass == null) {
            permissionClass = Class.forName(className);
        }

        return permissionClass;
    }

    private Class<?> getPermissionClassUsingBundleClassLoader(String className) throws ClassNotFoundException {
        Class<?> permissionClass = null;
        ClassLoader classloader = getBundleClassLoader(className);

        if (classloader != null) {
            permissionClass = classloader.loadClass(className);
        }

        return permissionClass;
    }

    private ClassLoader getBundleClassLoader(String className) {
        Collection<BundleCapability> bundleCapabilities = getBundlesProvidingPackage(className.substring(0, className.lastIndexOf(".")));
        BundleWiring providerBundleWiring = getBundleWiring(bundleCapabilities);

        return providerBundleWiring != null ? providerBundleWiring.getClassLoader() : null;
    }

    private Collection<BundleCapability> getBundlesProvidingPackage(final String classPackage) {
        FrameworkWiring frameworkWiring = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);

        Collection<BundleCapability> matchingCapabilities = frameworkWiring.findProviders(new org.osgi.resource.Requirement() {
            public org.osgi.resource.Resource getResource() {
                return null;
            }

            public String getNamespace() {
                return org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;
            }

            public Map<String, String> getDirectives() {
                return Collections.singletonMap(org.osgi.resource.Namespace.REQUIREMENT_FILTER_DIRECTIVE,
                                                "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + classPackage + ")");
            }

            public Map<String, Object> getAttributes() {
                return Collections.emptyMap();
            }
        });
        return matchingCapabilities;
    }

    private BundleWiring getBundleWiring(Collection<BundleCapability> bundleCapabilities) {
        BundleCapability bundleCapability = null;
        for (BundleCapability bc : bundleCapabilities) {
            if (bundleCapability != null && 
                bc.getRevision().getBundle().getBundleId() == 0) {
                // if more than one bundle exports the same package, prefer the non-system bundle
                break;
            }
            bundleCapability = bc;
        }

        return bundleCapability != null ? bundleCapability.getRevision().getWiring() : null;
    }

    /**
     * Return the effective restrictable permissions
     * 
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
        String original_codeBase = codeBase;

        // Add the granted permissions to an arraylist
        effectivePermissions.addAll(grantedPermissions);

        // Add the codebase specific permissions
        codeBase = normalize(codeBase);
        

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "os_name: "+ os_name + " os_version: " + os_version);
        }
        // Windows 10 adds another / to the front of the codesource that needs to removed, else
        // the codebases will not match between the permissions being specified and the effective perms
        if (os_name.contains("Windows") && (os_version.equals("10.0"))) {
            if (codeBase.startsWith("/")) {
                codeBase = codeBase.substring(1);
            }

        }     
        
        if (tc.isDebugEnabled()) {
            Set k = codeBasePermissionMap.keySet();
            Iterator<String> it = codeBasePermissionMap.keySet().iterator();      
            while(it.hasNext())  
            {  
                String key=(String)it.next();
                Tr.debug(tc, "codebase key: "  + key);
            }
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "codeBase: " + codeBase + " original_codeBase: " + original_codeBase);
        }
        
        if (codeBasePermissionMap.containsKey(codeBase)) {
            effectivePermissions.addAll(codeBasePermissionMap.get(codeBase));
        } else if (codeBasePermissionMap.containsKey(original_codeBase)) {
            effectivePermissions.addAll(codeBasePermissionMap.get(original_codeBase));
           
        }

        // Add permissions.xml permissions
        if (permissionXMLPermissionMap.containsKey(codeBase)) {
            effectivePermissions.addAll(permissionXMLPermissionMap.get(codeBase));
        } else if (permissionXMLPermissionMap.containsKey(original_codeBase)) {
            effectivePermissions.addAll(permissionXMLPermissionMap.get(original_codeBase));
        }

        // Iterate over the permissions and only add those that are not restricted
        for (Permission permission : permissions) {
            if (!isRestricted(permission)) {
                effectivePermissions.add(permission);
            }
        }
        
        if (tc.isDebugEnabled()) {
            for (int i = 0; i < effectivePermissions.size(); i++)
            {
                    Permission element = effectivePermissions.get(i);
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
        for (Permission restrictedPermission : restrictablePermissions) {
            if (restrictedPermission.implies(permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean java2SecurityEnabled() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds a permission from the permissions.xml file for the given CodeSource.
     * 
     * @param codeSource - The CodeSource of the code the specified permission was granted to.
     * @param permissions - The permissions granted in the permissions.xml of the application.
     * @return the effective granted permissions
     */
    public void addPermissionsXMLPermission(CodeSource codeSource, Permission permission) {
        ArrayList<Permission> permissions = null;
        String codeBase = codeSource.getLocation().getPath();
        String fileName = codeSource.getLocation().getFile();
        int last = fileName.lastIndexOf("/");
        fileName = fileName.substring(last + 1);
        
        File installRoot = new File(getInstallRoot());
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " getInstallRoot: " + getInstallRoot() + " fileName: " + fileName + " codeBase: " + codeBase);
        }
        
        if (!isRestricted(permission)) {
            if (permissionXMLPermissionMap.containsKey(codeBase)) {
                permissions = permissionXMLPermissionMap.get(codeBase);
                permissions.add(permission);
            } else {
                permissions = new ArrayList<Permission>();
                permissions.add(permission);
                permissionXMLPermissionMap.put(codeBase, permissions);
                // Calling recursive method 
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, " added new perm to codebase: " + codeBase + ", calling recursive find on filename: " + fileName + " codeBase: " + codeBase);
                }
                RecursiveFind(installRoot, fileName, codeBase, permissions);

            }
        }
    }

    /**
     * Returns the installation root. If it wasn't detected, use current directory.
     */
    public static String getInstallRoot() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                String output = System.getProperty("server.config.dir");
                if (output == null) {
                    output = System.getenv("SERVER_CONFIG_DIR");
                }
                if (output == null) {
                        output = ".";
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The install root is " + output);
                }
                return output;
            }
        });
    }
    
    private void RecursiveArchiveFind(File dir, String individualArchive, String codeBase, ArrayList<Permission> permissions) {
        File [] files = dir.listFiles();

        // for every file in the current directory, see if it matches any of the individual archive files
        for (File file : files) {
            if (file.isFile()) {
                String newcodebase = file.getPath().replace("\\", "/");
                newcodebase = "/".concat(newcodebase);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, ".....RecursiveFind: found file: " + file.getName() + " individualArchive: " + individualArchive + "   file.getPath(): " + 
                                    file.getPath() + " codeBase: " + codeBase + " newcodebase: " + newcodebase);              
                };
                

                if (file.getName().equals(individualArchive)) {
                    if (newcodebase.equals(codeBase)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "       the filenames and codebases matched, keep searching");
                        // this is the same file, keep searching 
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "          the file names matched, but the codebase didnt, so let's see if it's in cache");
                        }
                        // diff codebase, check for cache, add perm
                        if (newcodebase.contains("workarea") && newcodebase.contains("data") && newcodebase.contains("cache")) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "                        newcodebase contains workarea, adding perm to cached entry");
                            }
                            permissionXMLPermissionMap.put(newcodebase, permissions);
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, " ... cached file, adding permissions");
                            }
                        }                    
                    }
                }
            }  
            // for sub-directories 
            else if (file.isDirectory()) 
            {          
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, ".....RecursiveFind: found directory: " + file.getName());
                }
                RecursiveArchiveFind(file, individualArchive, codeBase, permissions);                
            } 
        }
    }
    
    
    private void RecursiveFind(File dir, String fileName, String codeBase, ArrayList<Permission> permissions)  
    { 
        
        // take the archive referenced by fileName apart to its individual archives
        ZipFile z = null;
        if (!codeBase.contains("expanded")) {
            try {
                z = new ZipFile(codeBase);
               
            } catch (java.io.IOException ioe) {
                // should never get here
            }
            ZipEntry ze = null;
            if (z != null) {
                Enumeration <? extends ZipEntry> zenum = z.entries();
                while (zenum.hasMoreElements()) {
                    ze = (ZipEntry)zenum.nextElement();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "for every  enumerated archive name: " + ze.getName());
                    }
                    String individualArchive = ze.getName();
                    RecursiveArchiveFind(dir, individualArchive, codeBase, permissions);
                    
                }         
            }    
        }

    } 

}