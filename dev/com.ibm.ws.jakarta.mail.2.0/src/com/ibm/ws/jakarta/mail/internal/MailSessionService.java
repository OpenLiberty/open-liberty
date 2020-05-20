/*******************************************************************************
 * Copyright (c) 2014, 2015, 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jakarta.mail.internal;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;


import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.jakarta.mail.MailSessionRegistrar;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * MailSessionService serves as both the component for the jakartaMail-2.0 feature
 * and is an implementation of a ResourceFactory that will create a Session object
 * that has been defined in the server.xml file with the <mailSession> element.
 *
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.jakarta.mail.mailSession", property = "creates.objectClass=jakarta.mail.Session")
public class MailSessionService implements ResourceFactory {
    private static final TraceComponent tc = Tr.register(MailSessionService.class);
    /**
     * Service reference to this instance.
     */
    private final AtomicReference<ComponentContext> cctx = new AtomicReference<ComponentContext>();

    private static final String KEY_MAIL_SESSION_REGISTRAR = "MailSessionRegistrar";
    private final AtomicServiceReference<MailSessionRegistrar> mailSessionRegistrarRef = new AtomicServiceReference<MailSessionRegistrar>(KEY_MAIL_SESSION_REGISTRAR);
    private ServiceRegistration<?> mbeanServiceReg;

    /**
     * The properties class that contain the attributes defined
     * by inside of server.xml, which are used to create the (mail)
     * Session by passing
     */
    private final Properties sessionProperties = new Properties();
    /**
     * Strings used to access the various attributes that are
     * defined in the <mailSession> and that are subsequently
     * extracted from the ComponentContext to be placed in the Properties
     * object
     */
    public static final String MAILSESSIONID = "mailSessionID";
    public static final String JNDI_NAME = "jndiName";
    public static final String DESCRIPTION = "description";
    public static final String STOREPROTOCOL = "storeProtocol";
    public static final String TRANSPORTPROTOCOL = "transportProtocol";
    public static final String HOST = "host";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String FROM = "from";
    public static final String STOREPROTOCOLCLASSNAME = "storeProtocolClassName";
    public static final String TRANSPORTPROTOCOLCLASSNAME = "transportProtocolClassName";
    public static final String PROPERTY = "property";
    public static final String NAME = "name";
    protected static final String VALUE = "value";

    /**
     * An array of the attribute strings that is used to traverse
     * them later on in the code.
     */
    private final String propertiesArray[] = { MAILSESSIONID, JNDI_NAME, DESCRIPTION, STOREPROTOCOL,
                                               TRANSPORTPROTOCOL, HOST, USER, PASSWORD, FROM, STOREPROTOCOLCLASSNAME,
                                               TRANSPORTPROTOCOLCLASSNAME, PROPERTY };

    /**
     * The List that contains all the nested attributes defined in the config
     */
    List<Map<String, Object>> listOfPropMap;

    @Activate
    protected void activate(ComponentContext context) throws Exception {

        cctx.set(context);
        mailSessionRegistrarRef.activate(context);
        modified(context);
        Tr.info(tc, "CWWKX0957I", "For the mailSession with the mailSessionID: " + sessionProperties.getProperty(MAILSESSIONID));
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        deregisterJavaMailMBean();
        mailSessionRegistrarRef.deactivate(context);
    }

    /**
     * The Modified method is how the properties that are defined in the
     * mailSession object of the server.xml are extracted and stored in the
     * sessionProperties.
     *
     * @param context
     * @param bundleContext
     * @throws Exception
     */
    @Modified
    protected void modified(ComponentContext context) throws Exception {

        processProperties(context.getProperties());
        deregisterJavaMailMBean();
        registerJavaMailMBean();
    }

    public void processProperties(Dictionary<String, Object> properties) {
        for (String i : propertiesArray) {
            // Gets the predefined properties from the ComponentContext
            if (properties.get(i) != null)
                sessionProperties.put(i, properties.get(i));
            // Uses the Nester class to get the name, value pairs out of n
            // properties defined in the server.xml
            if (i == PROPERTY)
                if (Nester.nest(PROPERTY, properties) != null)
                    listOfPropMap = Nester.nest(PROPERTY, properties);
        }
    }

    /**
     * The createResource uses the sessionProperties to create a jakarta.mail.Session
     * object and return it to the caller. The caller uses JNDI look up with the
     * JNDI name defined in the server.xml.
     */
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        Properties propertyNames = createPropertyNames();
        Properties props = createProperties(propertyNames);

        // The listOfPropMap is how the nested properties (name, value pairs) are
        // pulled from the Nester class. This iterates the map and stores them in the
        // props object
        if (listOfPropMap != null) {
            for (Map<String, Object> nestedProperties : listOfPropMap) {
                props.put(nestedProperties.get(NAME), nestedProperties.get(VALUE));
            }
        }

        Session session = createSession(props);

        return session;
    }

    /**
     * The createProperties method will iterate through the sessionProperties and put them into the
     * props object so that it can be used to create the jakarta.mail.Session
     *
     * @param propertyNames
     * @param props
     */
    private Properties createProperties(Properties propertyNames) {
        Properties props = new Properties();
        for (String key : propertiesArray) {
            if (sessionProperties.get(key) != null) {
                if (!key.equalsIgnoreCase(STOREPROTOCOLCLASSNAME) && !key.equalsIgnoreCase(TRANSPORTPROTOCOLCLASSNAME)) {
                    props.put(key, sessionProperties.get(key));
                    if (propertyNames.getProperty(key) != null)
                        props.put(propertyNames.getProperty(key), sessionProperties.get(key));
                } else {
                    if (key.equalsIgnoreCase(STOREPROTOCOLCLASSNAME))
                        props.put("mail." + props.getProperty(STOREPROTOCOL) + ".class", sessionProperties.get(key));
                    else
                        props.put("mail." + props.getProperty(TRANSPORTPROTOCOL) + ".class", sessionProperties.get(key));
                }
            }
        }
        return props;
    }

    /**
     * The createPropertyName method sets a property for five special strings that can be set
     * in a different method such as mail.host. These are different way to set certain property values
     * and are unique to these five
     */

    private Properties createPropertyNames() {

        Properties propertyNames = new Properties();

        propertyNames.setProperty(HOST, "mail.host");
        propertyNames.setProperty(USER, "mail.user");
        propertyNames.setProperty(FROM, "mail.from");
        propertyNames.setProperty(TRANSPORTPROTOCOL, "mail.transport.protocol");
        propertyNames.setProperty(STOREPROTOCOL, "mail.store.protocol");

        return propertyNames;
    }

    /**
     * The createSession method creates a session using the props, if the password is
     * specified in the server.xml then a session is creating using the password. If it
     * is not specified then session is created with out a authenticator.
     *
     * @param props
     * @return session
     * @throws UnsupportedCryptoAlgorithmException
     * @throws InvalidPasswordDecodingException
     */

    private Session createSession(Properties props) throws InvalidPasswordDecodingException, UnsupportedCryptoAlgorithmException {

        Session session = null;

        // Since the password attribute in the server.xml is masked
        // the decryption algorythm is needed to before it can be put
        // it into the props object.

        if (sessionProperties.get(PASSWORD) != null && sessionProperties.get(USER) != null) {

            SerializableProtectedString password = (SerializableProtectedString) sessionProperties.get(PASSWORD);
            String pwdStr = password == null ? null : String.valueOf(password.getChars());
            pwdStr = PasswordUtil.getCryptoAlgorithm(pwdStr) == null ? pwdStr : PasswordUtil.decode(pwdStr);
            final String pwd = pwdStr;

            sessionProperties.put("mail.password", pwdStr);
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication((String) sessionProperties.get(USER), pwd);
                }
            });

        } else {
            session = Session.getInstance(props, null);
        }

        return session;
    }

    /**
     * Declarative Services method for setting mail session registrar
     */
    @Reference(service = MailSessionRegistrar.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(component.name=com.ibm.ws.javamail.management.j2ee.MailSessionRegistrarImpl)")
    protected void setMailSessionRegistrar(ServiceReference<MailSessionRegistrar> ref) {
        mailSessionRegistrarRef.setReference(ref);
        registerJavaMailMBean();
    }

    /**
     * Declarative Services method for unsetting mail session registrar
     */
    protected void unsetMailSessionRegistrar(ServiceReference<MailSessionRegistrar> ref) {
        deregisterJavaMailMBean();
        mailSessionRegistrarRef.unsetReference(ref);
    }

    private void registerJavaMailMBean() {
        MailSessionRegistrar msr = mailSessionRegistrarRef.getService();
        if (msr != null && mbeanServiceReg == null) {
            mbeanServiceReg = msr.registerJavaMailMBean(String.valueOf(sessionProperties.get(MAILSESSIONID)));
        }
    }

    private void deregisterJavaMailMBean() {
        if (mbeanServiceReg != null) {
            try {
                mbeanServiceReg.unregister();
            } catch (IllegalStateException ise) {
                //ignore since service is already deregistered
            } finally {
                mbeanServiceReg = null;
            }
        }
    }

}
