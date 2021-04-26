/*******************************************************************************
 * Copyright (c) 2018, 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.InvalidConfigurationException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.logging.collector.LogFieldConstants;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.KeyValueStringPair;
import com.ibm.ws.security.audit.encryption.AuditEncryptionImpl;
import com.ibm.ws.security.audit.encryption.AuditSigningImpl;
import com.ibm.ws.security.audit.event.AuditMgmtEvent;
import com.ibm.ws.security.audit.logutils.FileLog;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.SynchronousHandler;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.audit.AuditEncryptionException;
import com.ibm.wsspi.security.audit.AuditService;
import com.ibm.wsspi.security.audit.AuditSigningException;


/**
 * This class is a collector manager Handler that takes audit events from the
 * BufferManager and writes them to local files. (copied from com.ibm.ws.logging temporary)
 */
@Component(service = Handler.class, configurationPid = "com.ibm.ws.security.audit.file.handler", configurationPolicy = ConfigurationPolicy.OPTIONAL, property = "service.vendor=IBM", immediate = true)
public class AuditFileHandler implements SynchronousHandler {

    private static final TraceComponent tc = Tr.register(AuditFileHandler.class);

    private static final String AUDIT_FILE_LOG_DEFAULT_NAME = "audit.log";

    private volatile CollectorManager collectorMgr;
    //private volatile BufferManagerImpl auditLogConduit;

    private static final String KEY_EXECUTOR_SERVICE = "executorSrvc";
    private final AtomicServiceReference<ExecutorService> executorSrvcRef = new AtomicServiceReference<ExecutorService>(KEY_EXECUTOR_SERVICE);

    static final String KEY_KEYSTORE_SERVICE_REF = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE_REF);

    public static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);
    private static final Object KEY_ID = "id";

    private volatile Future<?> handlerTaskRef = null;

    private volatile BufferManager bufferMgr = null;

    protected volatile FileLog auditLog = null;

    private final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private static final String KEY_AUDIT_SERVICE = "auditService";
    protected final AtomicServiceReference<AuditService> auditServiceRef = new AtomicServiceReference<AuditService>(KEY_AUDIT_SERVICE);

    private final List<String> sourceIds = new ArrayList<String>() {
        {
            add(AuditService.AUDIT_SOURCE_NAME + AuditService.AUDIT_SOURCE_SEPARATOR + AuditService.AUDIT_SOURCE_LOCATION);
            //add("com.ibm.ws.audit.source.auditsource");
        }
    };

    private boolean encrypt = false;
    private boolean sign = false;
    private String encryptAlias = null;
    private String signerAlias = null;
    private final String encryptKeyStoreRef = null;
    private String encryptKeyStoreId = null;
    private final String signerKeyStoreRef = null;
    private String signerKeyStoreId = null;
    private String wrapBehavior = null;
    private String logDirectory = null;
    private Integer maxFiles = -1;
    private Integer maxFileSize = -1;
    private String[] events = null;
    private boolean compact = false;
    private Map<String, Object> thisConfiguration;
    private String encryptKeyStoreLocation = null;
    private String signerKeyStoreLocation = null;
    private final Certificate encryptCert = null;
    private java.security.Key sharedKey = null;
    private String sharedKeyAlias = null;
    private java.security.Key publicKey = null;
    private byte[] encryptedSharedKey = null;
    private java.security.cert.X509Certificate cert = null;
    private AuditService auditService = null;
    private java.security.Key signedSharedKey = null;
    private java.security.Key publicSignerKey = null;
    private java.security.Key privateSignerKey = null;
    private byte[] encryptedSignerSharedKey = null;
    private java.security.cert.X509Certificate signerCert = null;
    private byte[] signerCertBytes = null;

    private final static String encryptionOpenTag = "<EncryptionInformation>\n";
    private final static String encryptionCloseTag = "</EncryptionInformation>\n";
    private final static String encryptedSharedKeyOpenTag = "   <encryptedSharedKey>";
    private final static String encryptedSharedKeyCloseTag = "</encryptedSharedKey>\n";

    private final static String encryptionCertAliasOpenTag = "   <encryptionCertAlias>";
    private final static String encryptionCertAliasCloseTag = "</encryptionCertAlias>\n";

    private final static String signingCertAliasOpenTag = "   <signingCertAlias>";
    private final static String signingCertAliasCloseTag = "</signingCertAlias>\n";

    private final static String encryptionKeyStoreOpenTag = "   <encryptionKeyStore>";
    private final static String encryptionKeyStoreCloseTag = "</encryptionKeyStore>\n";

    private final static String signingKeyStoreOpenTag = "   <signingKeyStore>";
    private final static String signingKeyStoreCloseTag = "</signingKeyStore>\n";

    private final static String keyStoreNameOpenTag = "   <keyStoreName>";
    private final static String keyStoreNameCloseTag = "</keyStoreName>\n";

    private final static String encryptionCertificateOpenTag = "   <encryptionCertificate>";
    private final static String encryptionCertificateCloseTag = "</encryptionCertificate>\n";

    private final static String signingCertificateOpenTag = "   <signingCertificate>";
    private final static String signingCertificateCloseTag = "</signingCertificate>\n";

    private final static String scopeOpenTag = "   <scope>";
    private final static String scopeCloseTag = "</scope>\n";

    private final static String signatureOpenTag = "<signature>";
    private final static String signatureCloseTag = "</signature>";

    private final static String signingOpenTag = "<SigningInformation>\n";
    private final static String signingCloseTag = "</SigningInformation>\n";

    private final static String signingSharedKeyOpenTag = "   <signingSharedKey>";
    private final static String signingSharedKeyCloseTag = "</signingSharedKey>\n";

    private final String signerKeyStoreName = null;
    private final String signerKeyStoreScope = null;
    private final String signerCertAlias = null;
    private final String signerKeyFileLocation = null;

    private final static String newLine = "\n";

    private final static String begin = "<auditRecord>";
    private final static String end = "</auditRecord>";

    List<Map<String, Object>> configuredEvents = null;

    AuditEncryptionImpl ae = null;
    AuditSigningImpl as = null;

    boolean encryptHeaderEmitted = false;
    boolean signerHeaderEmitted = false;

    private final int eventSequenceNumber = 0;

    byte[] signedEncryptedAuditRecord = null;
    byte[] signedAuditRecord = null;
    byte[] mergedByteRecord = null;
    byte[] er = null;
    ByteArrayOutputStream baos = null;

    private static Object syncObject = new Object();
    private static Object syncSeqNum = new Object();

    private String bundleLocation;

    @Activate
    protected void activate(ComponentContext cc) throws KeyStoreException, AuditEncryptionException, AuditSigningException {
        Tr.info(tc, "AUDIT_FILEHANDLER_STARTING");
        locationAdminRef.activate(cc);
        executorSrvcRef.activate(cc);
        auditServiceRef.activate(cc);
        configAdminRef.activate(cc);
        this.bundleLocation = cc.getBundleContext().getBundle().getLocation();

        Map<String, Object> configuration = (Map) cc.getProperties();
        thisConfiguration = configuration;

        if (configuration != null && !configuration.isEmpty()) {
            for (Map.Entry<String, Object> entry : configuration.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key.equals(AuditConstants.MAX_FILES)) {
                    setMaxFiles(value);
                } else if (key.equals(AuditConstants.MAX_FILE_SIZE)) {
                    setMaxFileSize(value);
                } else if (key.equals(AuditConstants.ENCRYPT)) {
                    setEncrypt(value);
                } else if (key.equals(AuditConstants.SIGN)) {
                    setSign(value);
                } else if (key.equals(AuditConstants.WRAP_BEHAVIOR)) {
                    setWrapBehavior(value);
                } else if (key.equals(AuditConstants.LOG_DIRECTORY)) {
                    setLogDirectory(value);
                } else if (key.equals(AuditConstants.COMPACT)) {
                    setCompact(value);
                } else if (key.equals(AuditConstants.ENCRYPT_ALIAS)) {
                    setEncryptAlias(value);
                } else if (key.equals(AuditConstants.SIGNING_ALIAS)) {
                    setSignerAlias(value);
                } else if (key.equals(AuditConstants.ENCRYPT_KEYSTORE_REF)) {
                    setEncryptKeyStoreRef(value);
                } else if (key.equals(AuditConstants.SIGNING_KEYSTORE_REF)) {
                    setSignerKeyStoreRef(value);
                }
            }
        }

        configuredEvents = Nester.nest("events", configuration);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "configuredEvents being sent to AuditService: " + configuredEvents.toString());
        }

        if (getEncrypt() || getSign())
            keyStoreServiceRef.activate(cc);

        auditService = auditServiceRef.getService();
        try {
            auditService.registerEvents(getHandlerName(), configuredEvents);
        } catch (InvalidConfigurationException e) {
            locationAdminRef.deactivate(cc);
            executorSrvcRef.deactivate(cc);
            auditServiceRef.deactivate(cc);
            keyStoreServiceRef.deactivate(cc);
            configAdminRef.deactivate(cc);
            cc.disableComponent((String) configuration.get(org.osgi.framework.Constants.SERVICE_PID));
            Tr.info(tc, "AUDIT_FILEHANDLER_STOPPED");
            throw new ComponentException("Caught invalidConfigurationException");
        }

        auditLog = FileLog.createFileLogHolder(null,
                                               logDirectory != null ? new File(logDirectory) : new File(getLogDir()),
                                               AUDIT_FILE_LOG_DEFAULT_NAME,
                                               maxFiles != -1 ? maxFiles : 100,
                                               maxFileSize != -1 ? maxFileSize * 1024L * 1024L : 20 * 1024L * 1024L);

        if (getEncrypt()) {
            setEncryptionKeys();
        }

        if (getSign()) {
            setSignerKeys();
        }
        //auditLogConduit = new BufferManagerImpl(10000, "audit|server");
        //auditLogConduit.addSyncHandler(this);

        Tr.info(tc, "AUDIT_FILEHANDLER_READY");

    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {

        auditService.unRegisterEvents(getHandlerName());

        locationAdminRef.deactivate(cc);
        executorSrvcRef.deactivate(cc);
        auditServiceRef.deactivate(cc);
        if (getEncrypt() || getSign())
            keyStoreServiceRef.deactivate(cc);
        configAdminRef.deactivate(cc);
        this.bundleLocation = null;

        auditLog.close();
        //auditLogConduit.removeSyncHandler(this);
        Tr.info(tc, "AUDIT_FILEHANDLER_STOPPED");

    }

    @Reference(service = WsLocationAdmin.class, name = KEY_LOCATION_ADMIN)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.setReference(reference);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> reference) {
        locationAdminRef.unsetReference(reference);
    }

    @Reference(service = AuditService.class, name = KEY_AUDIT_SERVICE)
    protected void setAuditService(ServiceReference<AuditService> reference) {
        auditServiceRef.setReference(reference);
    }

    protected void unsetAuditService(ServiceReference<AuditService> reference) {
        auditServiceRef.unsetReference(reference);
    }

    @Reference(service = ExecutorService.class, name = KEY_EXECUTOR_SERVICE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setExecutorSrvc(ServiceReference<ExecutorService> service) {
        executorSrvcRef.setReference(service);
    }

    protected void unsetExecutorSrvc(ServiceReference<ExecutorService> service) {
        executorSrvcRef.unsetReference(service);
    }

    @Reference(name = KEY_KEYSTORE_SERVICE_REF, service = KeyStoreService.class)
    protected void setKeyStoreService(ServiceReference<KeyStoreService> reference) {
        keyStoreServiceRef.setReference(reference);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> reference) {
        keyStoreServiceRef.unsetReference(reference);
    }

    @Reference(name = KEY_CONFIGURATION_ADMIN, service = ConfigurationAdmin.class)
    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    /** {@inheritDoc} */
    @Override
    public String getHandlerName() {
        return AuditService.AUDIT_FILE_HANDLER_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public void init(CollectorManager collectorMgr) {
        try {
            this.collectorMgr = collectorMgr;
            this.collectorMgr.subscribe(this, sourceIds);
        } catch (Exception e) {

        }

    }

    /** {@inheritDoc} */

    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        auditService.sendEvent(null);
    }

    /** {@inheritDoc} */

    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {
        if (auditService.isAuditRequired(AuditConstants.SECURITY_AUDIT_MGMT,
                                         AuditConstants.SUCCESS)) {
            AuditMgmtEvent av = new AuditMgmtEvent(thisConfiguration, "AuditHandler:" + auditService.AUDIT_FILE_HANDLER_NAME, "stop");
            auditService.sendEvent(av);
            av = new AuditMgmtEvent(thisConfiguration, "AuditService", "stop");
            auditService.sendEvent(av);
        }

    }

    /**
     * Get the default directory for logs
     *
     * @return full path String of logs directory
     */

    private String getLogDir() {
        StringBuffer output = new StringBuffer();
        WsLocationAdmin locationAdmin = locationAdminRef.getService();
        output.append(locationAdmin.resolveString("${server.output.dir}").replace('\\', '/')).append("/logs");
        return output.toString();
    }

    /**
     * Produce a JSON String for the given audit event
     *
     * @return
     */
    private String mapToJSONString(Map<String, Object> eventMap) {
        JSONObject jsonEvent = new JSONObject();
        String jsonString = null;
        map2JSON(jsonEvent, eventMap);
        try {
            if (!compact) {
                jsonString = jsonEvent.serialize(true).replaceAll("\\\\/", "/");
            } else {
                jsonString = jsonEvent.toString();
            }
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected error converting AuditEvent to JSON String", e);
            }
        }
        return jsonString;
    }

    /**
     * Given a Map, add the corresponding JSON to the given JSONObject.
     *
     * @param jo - JSONObject
     * @param map - Java Map object
     */
    private JSONObject map2JSON(JSONObject jo, Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            String subkeys = null;
            String key = null;
            Object value = entry.getValue();
            int i = entry.getKey().indexOf(".");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "raw key, index", new Object[] { entry.getKey(), i });
            }
            if (i > -1) {
                subkeys = entry.getKey().substring(i + 1);
                key = entry.getKey().substring(0, i);
            } else {
                key = entry.getKey();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "key, subkeys", new Object[] { key, subkeys });
            }
            if (subkeys == null) { // simple key
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "simple key: " + entry.getKey());
                }
                if (value == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "value is null");
                    }
                    jo.put(key, "null");
                } else if (value instanceof Map) { // value is a Map
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "value is a Map, calling map2JSON", value);
                    }
                    jo.put(key, map2JSON(new JSONObject(), (Map<String, Object>) value));
                } else if (value.getClass().isArray()) { // value is an array
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "value is an array, calling array2JSON", value);
                    }
                    jo.put(key, array2JSON(new JSONArray(), (Object[]) value));
                } else { // else value is a "simple" value
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "simple value, adding to jo", value);
                    }
                    jo.put(key, value);
                }
            } else { // compound key
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "compound key: " + entry.getKey());
                }
                JSONObject jsonSubstruc = (JSONObject) jo.get(key);
                if (jsonSubstruc == null) {
                    jsonSubstruc = new JSONObject();
                    jo.put(key, jsonSubstruc);
                }
                Map<String, Object> submap = new TreeMap<String, Object>();
                submap.put(subkeys, value);
                map2JSON(jsonSubstruc, submap);
            }
        }
        return jo;
    }

    /**
     * Given a Java array, add the corresponding JSON to the given JSONArray object
     *
     * @param ja - JSONArray object
     * @param array - Java array object
     */
    private JSONArray array2JSON(JSONArray ja, Object[] array) {
        for (int i = 0; i < array.length; i++) {
            // array entry is a Map
            if (array[i] instanceof Map) {
                //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //    Tr.debug(tc, "array entry is a Map, calling map2JSON", array[i]);
                //}
                ja.add(map2JSON(new JSONObject(), (Map<String, Object>) array[i]));
            }
            // array entry is an array
            else if (array[i].getClass().isArray()) {
                //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //    Tr.debug(tc, "array entry is a array, calling array2JSON", array[i]);
                //}
                ja.add(array2JSON(new JSONArray(), (Object[]) array[i]));
            }
            // else array entry is a "simple" value
            else {
                //if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //    Tr.debug(tc, "array entry is a simple value, adding to ja", array[i]);
                //}
                ja.add(array[i]);
            }
        }
        return ja;
    }

    /** {@inheritDoc} */
    public void setMaxFiles(Object value) {
        this.maxFiles = (Integer) value;
    }

    /** {@inheritDoc} */
    public Integer getMaxFiles() {
        return this.maxFiles;
    }

    /** {@inheritDoc} */
    public void setMaxFileSize(Object value) {
        this.maxFileSize = (Integer) value;
    }

    /** {@inheritDoc} */
    public Integer getMaxFileSize() {
        return this.maxFileSize;
    }

    /** {@inheritDoc} */
    public void setLogDirectory(Object value) {
        this.logDirectory = (String) value;
    }

    /** {@inheritDoc} */
    public String getLogDirectory() {
        return this.logDirectory;
    }

    /** {@inheritDoc} */
    public void setWrapBehavior(Object value) {
        this.wrapBehavior = (String) value;
    }

    /** {@inheritDoc} */
    public String getWrapBehavior() {
        return this.wrapBehavior;
    }

    /** {@inheritDoc} */
    public void setEncrypt(Object value) {
        this.encrypt = (Boolean) value;
    }

    /** {@inheritDoc} */
    public Boolean getEncrypt() {
        return this.encrypt;
    }

    /** {@inheritDoc} */
    public void setSign(Object value) {
        this.sign = (Boolean) value;
    }

    /** {@inheritDoc} */
    public Boolean getSign() {
        return this.sign;
    }

    /** {@inheritDoc} */
    public void setCompact(Object value) {
        this.compact = (Boolean) value;
    }

    /** {@inheritDoc} */
    public Boolean getCompact() {
        return this.compact;
    }

    /** {@inheritDoc} */
    public void setEncryptAlias(Object value) {
        this.encryptAlias = (String) value;
    }

    /** {@inheritDoc} */
    public String getEncryptAlias() {
        return this.encryptAlias;
    }

    /** {@inheritDoc} */
    public void setSignerAlias(Object value) {
        this.signerAlias = (String) value;
    }

    /** {@inheritDoc} */
    public String getSignerAlias() {
        return this.signerAlias;
    }

    /** {@inheritDoc} */
    public void setEncryptKeyStoreRef(Object value) {
        String id = getKeyStoreId((String) value);
        this.encryptKeyStoreId = id;
    }

    /** {@inheritDoc} */
    public String getEncryptKeyStoreRef() {
        return this.encryptKeyStoreId;
    }

    /** {@inheritDoc} */
    public void setSignerKeyStoreRef(Object value) {
        String id = getKeyStoreId((String) value);
        this.signerKeyStoreId = id;
    }

    /** {@inheritDoc} */
    public String getSignerKeyStoreRef() {
        return this.signerKeyStoreId;
    }

    /** {@inheritDoc} */
    public void setEvents(Object value) {
        this.events = ((String) value).split(", ");
    }

    /** {@inheritDoc} */
    public String[] getEvents() {
        return this.events;
    }

    @FFDCIgnore(KeyStoreException.class)
    public void setSignerKeys() throws KeyStoreException, AuditSigningException {
        KeyStoreService service = null;
        int retries = 0;
        if (getSign().booleanValue()) {
            service = keyStoreServiceRef.getService();
            try {
                signerKeyStoreLocation = service.getKeyStoreLocation(signerKeyStoreId);
            } catch (KeyStoreException e) {
                retries++;
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    // ignore it
                }
                if (retries < 6) {
                    try {
                        signerKeyStoreLocation = service.getKeyStoreLocation(signerKeyStoreId);
                    } catch (KeyStoreException ee) {
                        // ignore it until we've exhausted our retries
                    }
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception with keystore.", e.getMessage());
                Tr.error(tc, "FAILURE_INITIALIZING_SIGNING_CONFIGURATION", new Object[] { e.getMessage() });
                throw new KeyStoreException(e);
            }
        }

        try {
            as = new AuditSigningImpl(signerKeyStoreId, signerKeyStoreLocation, null, null, null, signerAlias);

        } catch (AuditSigningException ase) {
            Tr.error(tc, "FAILURE_INITIALIZING_SIGNING_CONFIGURATION", new Object[] { ase.getMessage() });
            throw new AuditSigningException(ase);
        }

        // Generate a second shared key to sign the audit records with.  Use the public key from securityAdmin certificate
        // that was generated with Audit initialized to encrypt this shared key.

        try {
            signedSharedKey = as.generateSharedKey();
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Error generating key.", new Object[] { e });
            Tr.error(tc, "FAILURE_INITIALIZING_SIGNING_CONFIGURATION", new Object[] { e.getMessage() });
            throw new AuditSigningException(e.getMessage(), e);
        }

        try {
            signerCert = service.getX509CertificateFromKeyStore(signerKeyStoreId, signerAlias);
            publicSignerKey = signerCert.getPublicKey();
            privateSignerKey = service.getPrivateKeyFromKeyStore(signerKeyStoreId, signerAlias, null);
            encryptedSignerSharedKey = as.encryptSharedKey(signedSharedKey, publicSignerKey);
        } catch (java.io.IOException ioe) {
            Tr.error(tc, "security.audit.keystore.open.error", new Object[] { ioe });
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            Tr.error(tc, "FAILURE_INITIALIZING_SIGNING_CONFIGURATION", new Object[] { ioe.getMessage() });
            throw new AuditSigningException(ioe.getMessage());
        } catch (CertificateException ce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception with certificate.", ce.getMessage());
            Tr.error(tc, "INCORRECT_AUDIT_SIGNING_CONFIGURATION", new Object[] { signerAlias, signerKeyStoreId });
            throw new AuditSigningException(ce.getMessage());
        } catch (KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception with keystore.", ke.getMessage());
            Tr.error(tc, "INCORRECT_AUDIT_ENCRYPTION_CONFIGURATION", new Object[] { signerAlias, signerKeyStoreId });
            throw new AuditSigningException(ke.getMessage());
        } catch (java.lang.Exception e) {
            Tr.error(tc, "security.audit.retrieve.signer.error", new Object[] { e });
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Failed to retrieve the signer information.", e.getMessage());
            Tr.error(tc, "FAILURE_INITIALIZING_SIGNING_CONFIGURATION", new Object[] { e.getMessage() });
            throw new AuditSigningException(e.getMessage());

        }

    }

    @FFDCIgnore(KeyStoreException.class)
    public void setEncryptionKeys() throws KeyStoreException, AuditEncryptionException {

        final int MAX_RETRIES = 10;
        KeyStoreService service = null;
        if (getEncrypt().booleanValue()) {
            service = keyStoreServiceRef.getService();

            for (int retries = 0; retries < MAX_RETRIES; retries++) {
                try {

                    encryptKeyStoreLocation = service.getKeyStoreLocation(encryptKeyStoreId);
                    break;

                } catch (KeyStoreException e) {
                    if (retries == MAX_RETRIES - 1) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Exception with keystore.", e.getMessage());
                        Tr.error(tc, "FAILURE_INITIALIZING_ENCRYPTION_CONFIGURATION", new Object[] { e.getMessage() });
                        throw new KeyStoreException(e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // ignore it
                    }
                }
            }

        }

        try {
            ae = new AuditEncryptionImpl(encryptKeyStoreId, encryptKeyStoreLocation, null, null, null, encryptAlias);
        } catch (AuditEncryptionException aee) {
            Tr.error(tc, "FAILURE_INITIALIZING_ENCRYPTION_CONFIGURATION", new Object[] { aee.getMessage() });
            throw new AuditEncryptionException(aee);
        }

        try {
            sharedKey = ae.generateSharedKey();
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Error generating key.", new Object[] { e });
            Tr.error(tc, "FAILURE_INITIALIZING_ENCRYPTION_CONFIGURATION", new Object[] { e.getMessage() });
            throw new AuditEncryptionException(e.getMessage(), e);
        }

        sharedKeyAlias = ae.generateAliasForSharedKey();

        try {
            cert = service.getX509CertificateFromKeyStore(encryptKeyStoreId, encryptAlias);
            publicKey = cert.getPublicKey();
            encryptedSharedKey = ae.encryptSharedKey(sharedKey, publicKey);

        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            Tr.error(tc, "FAILURE_INITIALIZING_ENCRYPTION_CONFIGURATION", new Object[] { ioe.getMessage() });
            throw new AuditEncryptionException(ioe.getMessage());
        } catch (CertificateException ce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception with certificate.", ce.getMessage());
            Tr.error(tc, "INCORRECT_AUDIT_ENCRYPTION_CONFIGURATION", new Object[] { encryptAlias, encryptKeyStoreId });
            throw new AuditEncryptionException(ce.getMessage());
        } catch (KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception with keystore.", ke.getMessage());
            Tr.error(tc, "INCORRECT_AUDIT_ENCRYPTION_CONFIGURATION", new Object[] { encryptAlias, encryptKeyStoreId });
            throw new AuditEncryptionException(ke.getMessage());
        }
    }

    public String buildEncSignerHeader() {
        String header = null;

        if (getEncrypt()) {
            String encHeader = buildEncryptionHeader();
            header = encHeader;
        }
        if (getSign()) {
            String signHeader = buildSignerHeader();
            if (header != null) {
                header = header.concat(signHeader);
            } else {
                header = signHeader;
            }
        }
        return header;
    }

    public String buildEncryptionHeader() {
        String header = null;

        byte[] certBytes = publicKey.toString().getBytes();

        header = encryptionOpenTag;

        header = header.concat(encryptedSharedKeyOpenTag);

        byte[] x = Base64Coder.base64Encode(encryptedSharedKey);
        header = header.concat(new String(x));

        header = header.concat(encryptedSharedKeyCloseTag);

        header = header.concat(encryptionCertAliasOpenTag);
        header = header.concat(encryptAlias);
        header = header.concat(encryptionCertAliasCloseTag);

        header = header.concat(encryptionKeyStoreOpenTag);
        header = header.concat(encryptKeyStoreLocation);
        header = header.concat(encryptionKeyStoreCloseTag);

        header = header.concat(encryptionCertificateOpenTag);
        header = header.concat(new String(certBytes));
        header = header.concat(newLine);
        header = header.concat(encryptionCertificateCloseTag);

        header = header.concat(encryptionCloseTag);

        return header;

    }

    public String buildSignerHeader() {
        String header = null;
        signerCertBytes = publicSignerKey.toString().getBytes();

        if (header == null) {
            header = signingOpenTag;
        } else {
            header = header.concat(signingOpenTag);
        }

        header = header.concat(signingSharedKeyOpenTag);

        byte[] x = Base64Coder.base64Encode(encryptedSignerSharedKey);

        header = header.concat(new String(x));
        header = header.concat(signingSharedKeyCloseTag);

        header = header.concat(signingCertAliasOpenTag);
        header = header.concat(signerAlias);
        header = header.concat(signingCertAliasCloseTag);

        header = header.concat(signingKeyStoreOpenTag);
        header = header.concat(signerKeyStoreLocation);
        header = header.concat(signingKeyStoreCloseTag);

        header = header.concat(signingCertificateOpenTag);
        header = header.concat(new String(signerCertBytes));

        header = header.concat(newLine);
        header = header.concat(signingCertificateCloseTag);

        header = header.concat(signingCloseTag);

        return header;

    }

    /** {@inheritDoc} */
    @Override
    public void synchronousWrite(Object arg) {
        synchronized (syncSeqNum) {

            if (getEncrypt() && !encryptHeaderEmitted) {
                String header = buildEncryptionHeader();
                auditLog.writeRecord(header);
                encryptHeaderEmitted = true;
            }

            if (getSign() && !signerHeaderEmitted) {
                String header = buildSignerHeader();
                auditLog.writeRecord(header);
                signerHeaderEmitted = true;
            }

            try {
                // AuditEvent event = (AuditEvent) arg;
                AuditEvent event = new AuditEvent();
                GenericData gdo = (GenericData) arg;

                for (KeyValuePair kvp : gdo.getPairs()) {
                    if (kvp instanceof KeyValueStringPair) {
                        if (!kvp.getKey().equals(LogFieldConstants.IBM_DATETIME) &&
                            (!kvp.getKey().equals(LogFieldConstants.IBM_SEQUENCE))) {
                            event.set(kvp.getKey(), kvp.getStringValue());
                        }
                    }
                }

                //event.set(AuditConstants.EVENT_SEQUENCE_NUMBER, eventSequenceNumber++);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received event " + event, this);
                }
                AuditService auditService = auditServiceRef.getService();
                if (auditService != null) {

                    String en = (String) event.getMap().get(AuditEvent.EVENTNAME);
                    String eo = (String) event.getMap().get(AuditEvent.OUTCOME);
                    if (auditService.isAuditRequired(en, eo)) {
                        if (getEncrypt()) {
                            String jsonEvent = mapToJSONString(event.getMap());

                            byte[] er = null;
                            byte[] encryptedAuditRecord = null;
                            byte[] eventBytes = jsonEvent.getBytes("UTF-8");
                            String z = new String(eventBytes);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "eventBytes: " + z + "eventBytes.length: " + eventBytes.length);
                            er = ae.encrypt(eventBytes, sharedKey);
                            encryptedAuditRecord = new byte[er.length];

                            System.arraycopy(er, 0, encryptedAuditRecord, 0, er.length);

                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "length of er: " + er.length + " length of encryptedAuditRecord: " + encryptedAuditRecord.length);
                                Tr.debug(tc, "encryptedAuditRecord: " + new String(encryptedAuditRecord));
                                Tr.debug(tc, "er: " + new String(er));
                            }

                            if (getSign()) {
                                signedEncryptedAuditRecord = as.sign(er, signedSharedKey);

                                byte[] sopen = signatureOpenTag.getBytes();
                                byte[] sclose = signatureCloseTag.getBytes();

                                baos = new ByteArrayOutputStream(er.length + sopen.length + signedEncryptedAuditRecord.length + sclose.length);
                                baos.write(er, 0, er.length);
                                baos.write(sopen, 0, sopen.length);
                                baos.write(signedEncryptedAuditRecord, 0, signedEncryptedAuditRecord.length);
                                baos.write(sclose, 0, sclose.length);
                                mergedByteRecord = new byte[baos.toByteArray().length];
                                mergedByteRecord = baos.toByteArray();

                                byte[] tmpPrivate = Base64Coder.base64Encode(mergedByteRecord);

                                synchronized (syncObject) {

                                    long total_to_add_length = begin.getBytes().length + tmpPrivate.length + end.getBytes().length;
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "total_to_add_length: " + total_to_add_length);
                                    long currentFileSize = auditLog.getCurrentCountStream();
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "currentFileSize: " + currentFileSize);
                                    if (currentFileSize != 0) {
                                        if (getMaxFileSize() != 0) {
                                            long max = (long) ((getMaxFileSize())) * 1024L * 1024L;
                                            if (tc.isDebugEnabled())
                                                Tr.debug(tc, "maxFileSize: " + max);

                                            if ((currentFileSize + total_to_add_length + 2) >= max) {
                                                if (tc.isDebugEnabled())
                                                    Tr.debug(tc, "adding padding to roll into new log");
                                                byte[] padding = new byte[(int) (max - currentFileSize)];
                                                auditLog.writeRecord(padding, buildEncSignerHeader());
                                                // this should get us to roll over to a new log
                                            }
                                        }
                                        auditLog.writeRecord(begin);
                                        auditLog.writeRecord(tmpPrivate, buildEncSignerHeader());
                                        auditLog.writeRecord(end);
                                    }

                                }
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "mergedByteRecord: " + new String(tmpPrivate));
                                }

                            } else {
                                byte[] tmpPrivate = Base64Coder.base64Encode(er);

                                synchronized (syncObject) {

                                    long total_to_add_length = begin.getBytes().length + tmpPrivate.length + end.getBytes().length;
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "total_to_add_length: " + total_to_add_length);
                                    long currentFileSize = auditLog.getCurrentCountStream();
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "currentFileSize: " + currentFileSize);
                                    if (currentFileSize != 0) {
                                        if (getMaxFileSize() != 0) {
                                            long max = (long) ((getMaxFileSize())) * 1024L * 1024L;
                                            if (tc.isDebugEnabled())
                                                Tr.debug(tc, "maxFileSize: " + max);

                                            if ((currentFileSize + total_to_add_length + 2) >= max) {
                                                if (tc.isDebugEnabled())
                                                    Tr.debug(tc, "adding padding to roll into new log");
                                                byte[] padding = new byte[(int) (max - currentFileSize)];
                                                auditLog.writeRecord(padding, buildEncSignerHeader());
                                                // this should get us to roll over to a new log
                                            }

                                        }

                                        auditLog.writeRecord(begin);
                                        auditLog.writeRecord(tmpPrivate, buildEncSignerHeader());
                                        auditLog.writeRecord(end);

                                    }

                                }
                            }

                        } // if encrypted, and maybe if signed
                        if (getSign() && !getEncrypt()) {
                            // only if signed

                            String jsonEvent = mapToJSONString(event.getMap());

                            byte[] eventBytes = jsonEvent.getBytes("UTF-8");

                            signedAuditRecord = as.sign(eventBytes, signedSharedKey);

                            byte[] sopen = signatureOpenTag.getBytes();
                            byte[] sclose = signatureCloseTag.getBytes();

                            baos = new ByteArrayOutputStream(eventBytes.length + sopen.length + signedAuditRecord.length + sclose.length);
                            baos.write(eventBytes, 0, eventBytes.length);
                            baos.write(sopen, 0, sopen.length);
                            baos.write(signedAuditRecord, 0, signedAuditRecord.length);
                            baos.write(sclose, 0, sclose.length);

                            mergedByteRecord = new byte[baos.toByteArray().length];
                            mergedByteRecord = baos.toByteArray();

                            byte[] tmpPrivate = Base64Coder.base64Encode(mergedByteRecord);

                            synchronized (syncObject) {

                                long total_to_add_length = begin.getBytes().length + tmpPrivate.length + end.getBytes().length;
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "total_to_add_length: " + total_to_add_length);
                                long currentFileSize = auditLog.getCurrentCountStream();
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "currentFileSize: " + currentFileSize);
                                if (currentFileSize != 0) {
                                    if (getMaxFileSize() != 0) {
                                        long max = (long) ((getMaxFileSize())) * 1024L * 1024L;
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, "maxFileSize: " + max);

                                        if ((currentFileSize + total_to_add_length + 2) >= max) {
                                            if (tc.isDebugEnabled())
                                                Tr.debug(tc, "adding padding to roll into new log");
                                            byte[] padding = new byte[(int) (max - currentFileSize)];
                                            auditLog.writeRecord(padding, buildEncSignerHeader());
                                            // this should get us to roll over to a new log
                                        }
                                    }
                                    auditLog.writeRecord(begin);
                                    auditLog.writeRecord(tmpPrivate, buildEncSignerHeader());
                                    auditLog.writeRecord(end);
                                }

                            }

                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "mergedByteRecord: " + new String(tmpPrivate));
                            }

                        }
                        if (!getEncrypt() && !getSign()) {
                            auditLog.writeRecord(mapToJSONString(event.getMap()));
                        }
                    }

                }
            } catch (Exception e) {

            }
        }
    }

    private String getKeyStoreId(String keyStoreRef) {
        if (keyStoreRef == null || keyStoreRef.isEmpty())
            return null;
        Configuration config = null;
        ConfigurationAdmin configAdmin = configAdminRef.getService();
        try {
            if (configAdmin != null)
                config = configAdmin.getConfiguration(keyStoreRef, bundleLocation);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid authFilterRef configuration", e.getMessage());
            }
            return null;
        }
        if (config == null)
            return null;
        Dictionary<String, Object> props = config.getProperties();
        if (props == null)
            return null;
        String id = (String) props.get(KEY_ID);
        return id;
    }

}
