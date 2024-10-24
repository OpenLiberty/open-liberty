/*
 * Copyright (c) 1998, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
package org.eclipse.persistence.internal.databaseaccess;

import java.io.Serializable;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.internal.core.databaseaccess.CorePlatform;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.queries.Call;
import org.eclipse.persistence.queries.ValueReadQuery;
import org.eclipse.persistence.sequencing.Sequence;
import org.eclipse.persistence.sessions.Session;

/**
 * Platform is private to TopLink. It encapsulates behavior specific to a datasource platform
 * (eg. Oracle, Sybase, DB2, Attunity, MQSeries), and provides the interface for TopLink to access this behavior.
 *
 * @see DatasourcePlatform
 * @see DatabasePlatform
 * @see org.eclipse.persistence.eis.EISPlatform
 *
 * @since OracleAS TopLink 10<i>g</i> (10.0.3)
 */
public interface Platform extends CorePlatform<ConversionManager>, Serializable, Cloneable {
    Object clone();

    /**
     * Convert the object to the appropriate type by invoking the appropriate
     * ConversionManager method
     * @param sourceObject the object that must be converted
     * @param javaClass the class that the object must be converted to
     * @exception ConversionException all exceptions will be thrown as this type.
     * @return the newly converted object
     */
    @Override Object convertObject(Object sourceObject, Class javaClass) throws ConversionException;

    /**
     * Copy the state into the new platform.
     */
    void copyInto(Platform platform);

    /**
     * The platform hold its own instance of conversion manager to allow customization.
     */
    @Override ConversionManager getConversionManager();

    /**
     * The platform hold its own instance of conversion manager to allow customization.
     */
    void setConversionManager(ConversionManager conversionManager);

    /**
     * Return the driver version.
     */
    String getDriverVersion();

    /**
     * Return the qualifier for the table. Required by some
     * databases such as Oracle and DB2
     */
    String getTableQualifier();

    /**
     * Answer the timestamp from the server.
     */
    java.sql.Timestamp getTimestampFromServer(AbstractSession session, String sessionName);

    /**
     * This method can be overridden by subclasses to return a
     * query that will return the timestamp from the server.
     * return null if the time should be the local time.
     */
    ValueReadQuery getTimestampQuery();

    boolean isH2();

    boolean isAccess();

    boolean isAttunity();

    boolean isCloudscape();

    boolean isDerby();

    boolean isDB2();

    boolean isDB2Z();

    boolean isDBase();

    boolean isHANA();

    boolean isHSQL();

    boolean isInformix();

    boolean isMaxDB();

    boolean isMySQL();

    boolean isODBC();

    boolean isOracle();

    boolean isOracle9();

    boolean isOracle23();

    boolean isOracle12();

    boolean isPointBase();

    boolean isSQLAnywhere();

    boolean isSQLServer();

    boolean isSybase();

    boolean isSymfoware();

    boolean isTimesTen();

    boolean isTimesTen7();

    boolean isPostgreSQL();

    /**
     * Allow the platform to initialize itself after login/init.
     */
    void initialize();

    /**
     * Set the qualifier for the table. Required by some
     * databases such as Oracle and DB2
     */
    void setTableQualifier(String qualifier);

    /**
     * Can override the default query for returning a timestamp from the server.
     * See: getTimestampFromServer
     */
    void setTimestampQuery(ValueReadQuery tsQuery);

    /**
     * Add the parameter.
     * Convert the parameter to a string and write it.
     */
    void appendParameter(Call call, Writer writer, Object parameter);

    /**
     * Allow for the platform to handle the representation of parameters specially.
     */
    Object getCustomModifyValueForCall(Call call, Object value, DatabaseField field, boolean shouldBind);

    /**
     * Delimiter to use for fields and tables using spaces or other special values.
     *
     * Some databases use different delimiters for the beginning and end of the value.
     * This delimiter indicates the end of the value.
     */
    String getEndDelimiter();

    /**
     * Delimiter to use for fields and tables using spaces or other special values.
     *
     * Some databases use different delimiters for the beginning and end of the value.
     * This delimiter indicates the start of the value.
     */
    String getStartDelimiter();

    /**
     * Allow for the platform to handle the representation of parameters specially.
     */
    boolean shouldUseCustomModifyForCall(DatabaseField field);

    /**
     * Get default sequence.
     * Sequence name shouldn't be altered -
     * don't do: getDefaultSequence().setName(newName).
     */
    Sequence getDefaultSequence();

    /**
     * Set default sequence.
     * The sequence should have a unique name
     * that shouldn't be altered after the sequence has been set:
     * don't do: getDefaultSequence().setName(newName)).
     * Default constructors for Sequence subclasses
     * set name to "SEQ".
     */
    void setDefaultSequence(Sequence sequence);

    /**
     * Add sequence.
     * The sequence should have a unique name
     * that shouldn't be altered after the sequence has been added -
     * don't do: getSequence(name).setName(newName))
     * Don't use if the session is connected.
     */
    void addSequence(Sequence sequence);

    /**
     * Add sequence.
     * The sequence should have a unique name
     * that shouldn't be altered after the sequence has been added -
     * don't do: getSequence(name).setName(newName))
     * Use this method with isConnected parameter set to true
     * to add a sequence to connected session.
     * If sequencing is connected then the sequence is added only
     * if there is no sequence with the same name already in use.
     */
    void addSequence(Sequence sequence, boolean isConnected);

    /**
     * Get sequence corresponding to the name.
     * The name shouldn't be altered -
     * don't do: getSequence(name).setName(newName)
     */
    Sequence getSequence(String seqName);

    /**
     * Remove sequence corresponding to the name
     * (the sequence was added through addSequence method)
     * Don't use if the session is connected.
     */
    Sequence removeSequence(String seqName);

    /**
     * Remove all sequences that were added through addSequence method.
     */
    void removeAllSequences();

    /**
     * INTERNAL:
     * Returns a map of sequence names to Sequences (may be null).
     */
    Map getSequences();

    /**
     * INTERNAL:
     * Used only for writing into XML or Java.
     */
    Map getSequencesToWrite();

    /**
     * INTERNAL:
     * Used only for writing into XML or Java.
     */
    Sequence getDefaultSequenceToWrite();

    /**
     * INTERNAL:
     * Used only for reading from XML.
     */
    void setSequences(Map sequences);

    /**
     * INTERNAL:
     * Indicates whether defaultSequence is the same as platform default sequence.
     */
    boolean usesPlatformDefaultSequence();

    /**
     * INTERNAL:
     * Initialize platform specific identity sequences.
     * This method is called from {@code EntityManagerSetupImpl} after login and optional schema generation.
     * Method is also called from {@code TableCreator} class during tables creation and update..
     * @param session Active database session (in connected state).
     * @param defaultIdentityGenerator Default identity generator sequence name.
     * @since 2.7
     */
    void initIdentitySequences(final Session session, final String defaultIdentityGenerator);

    /**
     * INTERNAL:
     * Remove platform specific identity sequences for specified tables. Default identity sequences are restored.
     * Method is also called from {@code TableCreator} class during tables removal.
     * @param session Active database session (in connected state).
     * @param defaultIdentityGenerator Default identity generator sequence name.
     * @param tableNames Set of table names to check for identity sequence removal.
     * @since 2.7
     */
    void removeIdentitySequences(
            final Session session, final String defaultIdentityGenerator, final Set<String> tableNames);

}
