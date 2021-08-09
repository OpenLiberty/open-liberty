/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class JCAGeneratedProperties extends ConfigElement {
    // attributes
    private String clientID;
    private String createDatabase;
    private String databaseName;
    private String date;
    private String derbyRef;
    private String destinationRef;
    private String destinationType;
    private String firstDayOfWeek;
    private String lenient;
    private String loginTimeout;
    private String messageFilterMax;
    private String messageFilterMin;
    private String minimalDaysInFirstWeek;
    private String month;
    private String password;
    private String queueName;
    private String tableName;
    private String timeInMillis;
    private String topicName;
    private String userName;
    private String year;

    // nested elements
    @XmlElement(name = "jmsDestination")
    private ConfigElementList<JMSDestination> jmsDestinations;

    @XmlElement(name = "jmsQueue")
    private ConfigElementList<JMSQueue> jmsQueues;

    @XmlElement(name = "jmsTopic")
    private ConfigElementList<JMSTopic> jmsTopics;

    public String getClientID() {
        return this.clientID;
    }

    public String getCreateDatabase() {
        return createDatabase;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDate() {
        return date;
    }

    public String getDerbyRef() {
        return derbyRef;
    }

    public String getDestinationRef() {
        return this.destinationRef;
    }

    public String getDestinationType() {
        return this.destinationType;
    }

    public ConfigElementList<JMSDestination> getJMSDestinations() {
        return jmsDestinations == null ? (jmsDestinations = new ConfigElementList<JMSDestination>()) : jmsDestinations;
    }

    public ConfigElementList<JMSQueue> getJMSQueues() {
        return jmsQueues == null ? (jmsQueues = new ConfigElementList<JMSQueue>()) : jmsQueues;
    }

    public ConfigElementList<JMSTopic> getJMSTopics() {
        return jmsTopics == null ? (jmsTopics = new ConfigElementList<JMSTopic>()) : jmsTopics;
    }

    public String getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    public String getLenient() {
        return lenient;
    }

    public String getLoginTimeout() {
        return loginTimeout;
    }

    public String getMessageFilterMax() {
        return messageFilterMax;
    }

    public String getMessageFilterMin() {
        return messageFilterMin;
    }

    public String getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }

    public String getMonth() {
        return month;
    }

    public String getPassword() {
        return this.password;
    }

    public String getQueueName() {
        return this.queueName;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getTimeInMillis() {
        return this.timeInMillis;
    }

    public String getTopicName() {
        return this.topicName;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getYear() {
        return year;
    }

    @XmlAttribute(name = "clientID")
    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    @XmlAttribute(name = "createDatabase")
    public void setCreateDatabase(String createDatabase) {
        this.createDatabase = createDatabase;
    }

    @XmlAttribute(name = "databaseName")
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    @XmlAttribute(name = "date")
    public void setDate(String date) {
        this.date = date;
    }

    @XmlAttribute(name = "derbyRef")
    public void setDerbyRef(String derbyRef) {
        this.derbyRef = derbyRef;
    }

    @XmlAttribute(name = "destinationRef")
    public void setDestinationRef(String destinationRef) {
        this.destinationRef = destinationRef;
    }

    @XmlAttribute(name = "destinationType")
    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    @XmlAttribute(name = "firstDayOfWeek")
    public void setFirstDayOfWeek(String firstDayOfWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
    }

    @XmlAttribute(name = "lenient")
    public void setLenient(String lenient) {
        this.lenient = lenient;
    }

    @XmlAttribute(name = "loginTimeout")
    public void setLoginTimeout(String loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    @XmlAttribute(name = "messageFilterMax")
    public void setMessageFilterMax(String messageFilterMax) {
        this.messageFilterMax = messageFilterMax;
    }

    @XmlAttribute(name = "messageFilterMin")
    public void setMessageFilterMin(String messageFilterMin) {
        this.messageFilterMin = messageFilterMin;
    }

    @XmlAttribute(name = "minimalDaysInFirstWeek")
    public void setMinimalDaysInFirstWeek(String minimalDaysInFirstWeek) {
        this.minimalDaysInFirstWeek = minimalDaysInFirstWeek;
    }

    @XmlAttribute(name = "month")
    public void setMonth(String month) {
        this.month = month;
    }

    @XmlAttribute(name = "password")
    public void setPassword(String password) {
        this.password = password;
    }

    @XmlAttribute(name = "queueName")
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @XmlAttribute(name = "tableName")
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @XmlAttribute(name = "timeInMillis")
    public void setTimeInMillis(String timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    @XmlAttribute(name = "topicName")
    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @XmlAttribute(name = "userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @XmlAttribute(name = "year")
    public void setYear(String year) {
        this.year = year;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Properties_FAT1{");
        if (clientID != null)
            buf.append("clientID=\"" + clientID + "\" ");
        if (createDatabase != null)
            buf.append("createDatabase=\"" + createDatabase + "\" ");
        if (databaseName != null)
            buf.append("databaseName=\"" + databaseName + "\" ");
        if (date != null)
            buf.append("date=\"" + date + "\" ");
        if (derbyRef != null)
            buf.append("derbyRef=\"" + derbyRef + "\" ");
        if (destinationRef != null)
            buf.append("destinationRef=\"" + destinationRef + "\" ");
        if (destinationType != null)
            buf.append("destinationType=\"" + destinationType + "\" ");
        if (firstDayOfWeek != null)
            buf.append("firstDayOfWeek=\"" + firstDayOfWeek + "\" ");
        if (lenient != null)
            buf.append("lenient=\"" + lenient + "\" ");
        if (loginTimeout != null)
            buf.append("loginTimeout=\"" + loginTimeout + "\" ");
        if (messageFilterMax != null)
            buf.append("messageFilterMax=\"" + messageFilterMax + "\" ");
        if (messageFilterMin != null)
            buf.append("messageFilterMin=\"" + messageFilterMin + "\" ");
        if (minimalDaysInFirstWeek != null)
            buf.append("minimalDaysInFirstWeek=\"" + minimalDaysInFirstWeek + "\" ");
        if (month != null)
            buf.append("month=\"" + month + "\" ");
        if (password != null)
            buf.append("password=\"" + password + "\" ");
        if (queueName != null)
            buf.append("queueName=\"" + queueName + "\" ");
        if (tableName != null)
            buf.append("tableName=\"" + tableName + "\" ");
        if (timeInMillis != null)
            buf.append("timeInMillis=\"" + timeInMillis + "\" ");
        if (topicName != null)
            buf.append("topicName=\"" + topicName + "\" ");
        if (userName != null)
            buf.append("userName=\"" + userName + "\" ");
        if (year != null)
            buf.append("year=\"" + year + "\" ");

        List<?> nestedElementsList = Arrays.asList(
                                                   jmsDestinations,
                                                   jmsQueues,
                                                   jmsTopics
                        );
        for (ConfigElementList<?> nestedElements : (List<ConfigElementList<?>>) nestedElementsList)
            if (nestedElements != null && nestedElements.size() > 0)
                for (Object o : nestedElements)
                    buf.append(", " + o);

        buf.append("}");
        return buf.toString();
    }
}