/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * MongoDB config element
 */
public class MongoDBElement extends ConfigElement {
    private String databaseName, jndiName, mongoRef;

    private MongoElement nestedMongo;

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @param databaseName the databaseName to set
     */
    @XmlAttribute
    public void setDatabaseName(String d) {
        databaseName = d;
    }

    /**
     * @return the jndiName
     */

    public String getJndiName() {
        return jndiName;
    }

    /**
     * @param jndiName the jndiName to set
     */
    @XmlAttribute
    public void setJndiName(String j) {
        jndiName = j;
    }

    /**
     * @return the mongoRef
     */
    public String getMongoRef() {
        return mongoRef;
    }

    /**
     * @param mongoRef the mongoRef to set
     */
    @XmlAttribute
    public void setMongoRef(String m) {
        mongoRef = m;
    }

    public MongoElement getMongo() {
        return nestedMongo;
    }

    @XmlElement
    public void setMongo(MongoElement mongo) {
        nestedMongo = mongo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MongoDBElement [databaseName=" + databaseName + ", jndiName=" + jndiName + ", mongoRef=" + mongoRef + ", nestedMongo=" + nestedMongo + "]";
    }

}
