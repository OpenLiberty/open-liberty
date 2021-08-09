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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Defines configuration attributes of the HTTP Session Database
 * 
 * @author Tim Burns
 */
public class HttpSessionDatabase extends ConfigElement {

    /**
     * Enumerates all allowed values for db2RowSize attribute
     */
    public static enum Db2RowSize {
        _4KB("4KB"),
        _8KB("8KB"),
        _16KB("16KB"),
        _32KB("32KB");
        private final String value;

        private Db2RowSize(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    /**
     * Enumerates all allowed values for writeFrequency attribute
     */
    public static enum WriteFrequency {
        END_OF_SERVLET_SERVICE,
        MANUAL_UPDATE,
        TIME_BASED_WRITE
    }

    /**
     * Enumerates all allowed values for writeContents attribute
     */
    public static enum WriteContents {
        ONLY_UPDATED_ATTRIBUTES,
        ALL_SESSION_ATTRIBUTES
    }

    private String dataSourceRef;
    private Boolean useMultiRowSchema;
    private String db2RowSize;
    private String tableSpaceName;
    private Boolean scheduleInvalidation;
    private Integer scheduleInvalidationFirstHour;
    private Integer scheduleInvalidationSecondHour;
    private String writeFrequency;
    private Integer writeInterval;
    private String writeContents;
    private Boolean noAffinitySwitchBack;
    private Boolean onlyCheckInCacheDuringPreInvoke;
    private Boolean optimizeCacheIdIncrements;
    private String tableName;
    private Boolean useInvalidatedId;
    private Boolean useOracleBlob;

    public void set(Db2RowSize db2RowSize) {
        this.setDb2RowSize(db2RowSize == null ? null : db2RowSize.toString());
    }

    public void set(WriteFrequency writeFrequency) {
        this.setWriteFrequency(writeFrequency == null ? null : writeFrequency.toString());
    }

    public void set(WriteContents writeContents) {
        this.setWriteContents(writeContents == null ? null : writeContents.toString());
    }

    @XmlTransient
    public void setDataSource(DataSource dataSource) {
        this.setDataSourceRef(dataSource == null ? null : dataSource.getId());
    }

    public String getDataSourceRef() {
        return this.dataSourceRef;
    }

    @XmlAttribute
    public void setDataSourceRef(String dataSourceRef) {
        this.dataSourceRef = dataSourceRef;
    }

    public Boolean getUseMultiRowSchema() {
        return useMultiRowSchema;
    }

    @XmlAttribute
    public void setUseMultiRowSchema(Boolean useMultiRowSchema) {
        this.useMultiRowSchema = useMultiRowSchema;
    }

    public String getDb2RowSize() {
        return this.db2RowSize;
    }

    @XmlAttribute
    public void setDb2RowSize(String db2RowSize) {
        this.db2RowSize = db2RowSize;
    }

    public String getTableSpaceName() {
        return this.tableSpaceName;
    }

    @XmlAttribute
    public void setTableSpaceName(String tableSpaceName) {
        this.tableSpaceName = tableSpaceName;
    }

    public Boolean getScheduleInvalidation() {
        return this.scheduleInvalidation;
    }

    @XmlAttribute
    public void setScheduleInvalidation(Boolean scheduleInvalidation) {
        this.scheduleInvalidation = scheduleInvalidation;
    }

    public Integer getScheduleInvalidationFirstHour() {
        return this.scheduleInvalidationFirstHour;
    }

    @XmlAttribute
    public void setScheduleInvalidationFirstHour(Integer scheduleInvalidationFirstHour) {
        this.scheduleInvalidationFirstHour = scheduleInvalidationFirstHour;
    }

    public Integer getScheduleInvalidationSecondHour() {
        return this.scheduleInvalidationSecondHour;
    }

    @XmlAttribute
    public void setScheduleInvalidationSecondHour(Integer scheduleInvalidationSecondHour) {
        this.scheduleInvalidationSecondHour = scheduleInvalidationSecondHour;
    }

    public String getWriteFrequency() {
        return this.writeFrequency;
    }

    @XmlAttribute
    public void setWriteFrequency(String writeFrequency) {
        this.writeFrequency = writeFrequency;
    }

    public Integer getWriteInterval() {
        return this.writeInterval;
    }

    @XmlAttribute
    public void setWriteInterval(Integer writeInterval) {
        this.writeInterval = writeInterval;
    }

    public String getWriteContents() {
        return this.writeContents;
    }

    @XmlAttribute
    public void setWriteContents(String writeContents) {
        this.writeContents = writeContents;
    }

    public Boolean getNoAffinitySwitchBack() {
        return noAffinitySwitchBack;
    }

    @XmlAttribute
    public void setNoAffinitySwitchBack(Boolean noAffinitySwitchBack) {
        this.noAffinitySwitchBack = noAffinitySwitchBack;
    }

    public Boolean getOnlyCheckInCacheDuringPreInvoke() {
        return onlyCheckInCacheDuringPreInvoke;
    }

    @XmlAttribute
    public void setOnlyCheckInCacheDuringPreInvoke(Boolean onlyCheckInCacheDuringPreInvoke) {
        this.onlyCheckInCacheDuringPreInvoke = onlyCheckInCacheDuringPreInvoke;
    }

    public Boolean getOptimizeCacheIdIncrements() {
        return optimizeCacheIdIncrements;
    }

    @XmlAttribute
    public void setOptimizeCacheIdIncrements(Boolean optimizeCacheIdIncrements) {
        this.optimizeCacheIdIncrements = optimizeCacheIdIncrements;
    }

    public String getTableName() {
        return tableName;
    }

    @XmlAttribute
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Boolean getUseInvalidatedId() {
        return useInvalidatedId;
    }

    @XmlAttribute
    public void setUseInvalidatedId(Boolean useInvalidatedId) {
        this.useInvalidatedId = useInvalidatedId;
    }

    public Boolean getUseOracleBlob() {
        return useOracleBlob;
    }

    @XmlAttribute
    public void setUseOracleBlob(Boolean useOracleBlob) {
        this.useOracleBlob = useOracleBlob;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("HttpSessionDatabase{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (dataSourceRef != null)
            buf.append("dataSourceRef=\"" + dataSourceRef + "\" ");
        if (useMultiRowSchema != null)
            buf.append("useMultiRowSchema=\"" + useMultiRowSchema + "\" ");
        if (db2RowSize != null)
            buf.append("db2RowSize=\"" + db2RowSize + "\" ");
        if (tableSpaceName != null)
            buf.append("tableSpaceName=\"" + tableSpaceName + "\" ");
        if (scheduleInvalidation != null)
            buf.append("scheduleInvalidation=\"" + scheduleInvalidation + "\" ");
        if (scheduleInvalidationFirstHour != null)
            buf.append("scheduleInvalidationFirstHour=\"" + scheduleInvalidationFirstHour + "\" ");
        if (scheduleInvalidationSecondHour != null)
            buf.append("scheduleInvalidationSecondHour=\"" + scheduleInvalidationSecondHour + "\" ");
        if (writeFrequency != null)
            buf.append("writeFrequency=\"" + writeFrequency + "\" ");
        if (writeInterval != null)
            buf.append("writeInterval=\"" + writeInterval + "\" ");
        if (writeContents != null)
            buf.append("writeContents=\"" + writeContents + "\" ");
        if (noAffinitySwitchBack != null)
            buf.append("noAffinitySwitchBack=\"" + noAffinitySwitchBack + "\" ");
        if (onlyCheckInCacheDuringPreInvoke != null)
            buf.append("onlyCheckInCacheDuringPreInvoke=\"" + onlyCheckInCacheDuringPreInvoke + "\" ");
        if (optimizeCacheIdIncrements != null)
            buf.append("optimizeCacheIdIncrements=\"" + optimizeCacheIdIncrements + "\" ");
        if (tableName != null)
            buf.append("tableName=\"" + tableName + "\" ");
        if (useInvalidatedId != null)
            buf.append("useInvalidatedId=\"" + useInvalidatedId + "\" ");
        if (useOracleBlob != null)
            buf.append("useOracleBlob=\"" + useOracleBlob + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
