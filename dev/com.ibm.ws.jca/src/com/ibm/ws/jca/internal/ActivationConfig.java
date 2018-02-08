/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Properties;

import com.ibm.ejs.j2c.J2CConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Objects of this class are serialized and used to store the information required to recreate an activation
 * spec during recovery
 */
public class ActivationConfig implements Serializable {

    private static final long serialVersionUID = -3812882135080246095L;

    private static final TraceComponent tc = Tr.register(ActivationConfig.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);
    private Properties activationConfigProps = null;

    private String destinationRef = null;

    private String authenticationAlias = null;

    private String applicationName = null;

    private String qmid = null;

    /**
     * List of fields that will be serialized when the writeObject method
     * is called. We do this so that the implementation of this class can
     * change without breaking the serialization process.
     */

    static private final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                                new ObjectStreamField("activationConfigProps", Properties.class),
                                                                                                new ObjectStreamField("destinationRef", String.class),
                                                                                                new ObjectStreamField("authenticationAlias", String.class),
                                                                                                new ObjectStreamField("applicationName", String.class),
                                                                                                new ObjectStreamField("qmid", String.class)
    };

    /**
     * @param activationConfigProps
     * @param destinationRef
     * @param authenticationAlias
     */
    public ActivationConfig(Properties activationConfigProps, String destinationRef, String authenticationAlias, String appName) {
        this.activationConfigProps = activationConfigProps;
        this.destinationRef = destinationRef;
        this.authenticationAlias = authenticationAlias;
        this.applicationName = appName;
    }

    /*
     * This method rereates the CMConfigData Object from a stream - all the members will be
     * re-initialized.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

        /*
         * Since this is a simple class all we have to do is read each member from the stream.
         * the order has to remain identical to writeObject, so I've just gone alphabetically.
         *
         * If we change the serialversionUID, we may need to modify this method as well.
         */

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "readObject", stream);
        }

        ObjectInputStream.GetField getField = stream.readFields();

        if (tc.isDebugEnabled()) {

            for (int i = 0; i < serialPersistentFields.length; i++) {

                String fieldName = serialPersistentFields[i].getName();

                if (getField.defaulted(fieldName))
                    Tr.debug(this, tc, "Could not de-serialize field " + fieldName + " in class " +
                                       getClass().getName() + "; default value will be used");

            }

        }

        activationConfigProps = (Properties) getField.get("activationConfigProps", null);
        destinationRef = (String) getField.get("destinationRef", null);
        authenticationAlias = (String) getField.get("authenticationAlias", null);
        applicationName = (String) getField.get("applicationName", null);
        qmid = (String) getField.get("qmid", null);

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "readObject activation config - " + this.toString());
    }

    /*
     * this method writes the CMConfigDataOjbect to a stream
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        /*
         * Since this is a simple class all we have to do is write each member to the stream.
         * the order has to remain identical to readObject, so I've just gone alphabetically.
         *
         * If we change the serialversionUID, we may need to modify this method as well.
         */

        if (tc.isEntryEnabled()) {
            Tr.entry(this, tc, "writeObject", stream);
        }

        ObjectOutputStream.PutField putField = stream.putFields();

        putField.put("activationConfigProps", activationConfigProps);
        putField.put("destinationRef", destinationRef);
        putField.put("authenticationAlias", authenticationAlias);
        putField.put("applicationName", applicationName);
        putField.put("qmid", qmid);

        stream.writeFields();

        if (tc.isEntryEnabled()) {
            Tr.exit(this, tc, "writeObject activation config - " + this.toString());
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ActivationConfig [destinationRef=" + destinationRef + ", authenticationAlias=" + authenticationAlias
               + ", applicationName=" + applicationName + ", qmid=" + qmid + "]";
    }

    /**
     * @return the activationConfigProps
     */
    public Properties getActivationConfigProps() {
        return activationConfigProps;
    }

    /**
     * @return id of a destination
     */
    public String getDestinationRef() {
        return destinationRef;
    }

    /**
     * @return the authenticationAlias
     */
    public String getAuthenticationAlias() {
        return authenticationAlias;
    }

    /**
     * @return the applicationName
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @return the qmid
     */

    public String getQmid() {
        return qmid;
    }

    /**
     * @param qmid the qmid to set
     */
    public void setQmid(String qmid) {
        this.qmid = qmid;
    }
}
