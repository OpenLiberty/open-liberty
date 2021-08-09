/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi;

import java.util.ArrayList;
import java.io.PrintWriter;
import com.ibm.websphere.pmi.stat.StatConstants;

/**
 * This class represents the specification of an individual Statistic in a Stats object (PMI module).
 * 
 * @ibm-api
 */
public class PmiDataInfo implements java.io.Serializable, PmiConstants {

    private static final long serialVersionUID = -1609400918066043034L;

    private int id;

    private String name;

    private int type = TYPE_UNDEFINED;

    private String description = null;

    private String category = "all";

    private String unit = "unit.none"; // key for translation PII

    // level: low, medium, high, maximum
    // If not set, can be derived from type
    private int level = LEVEL_UNDEFINED;

    private boolean resettable = true; // default is true

    private boolean aggregatable = true; // default is true

    private boolean zosAggregatable = true; // default is true

    //** added for custom PMI: external/calculated_on_request
    private boolean onRequest = false; // default is false

    private String statSet = StatConstants.STATISTIC_SET_ALL; // indicates the statistic set to which this counter belongs
    private String platform = PLATFORM_ALL; // indicates the statistic set to which this counter belongs
    private String submoduleName = null;

    // participation and comment are not used in all cases.
    private String participation = null;
    private String comment = null;
    private ArrayList dependencyList = null;

    /**
     * Constructor
     * 
     * @param id Uniquely identifies a statistic in a Stats object
     */
    public PmiDataInfo(int id) {
        this.id = id;
    }

    /**
     * Constructor
     * 
     * @param id Uniquely identifies a statistic in a Stats objecy
     * @param name Name of the statistic
     * @param type Type of the statistic (defined in PmiConstants)
     * @param level Instrumentation level (defined in PmiConstants)
     * @param description Desription of the statistic
     */
    public PmiDataInfo(int id, String name, int type, int level, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        //this.participation = participation;
        this.level = level;
        this.description = description;
        //this.comment = comment;
        //this.resettable = resettable;

        // most of the Stat data are time counter in milliseconds unit.
        // You have to explicitly set the unit in the xml file if the counter is
        // not in "ms" unit.
        if (type == TYPE_STAT)
            unit = "unit.ms"; // key for translation PII
    }

    /**
     * Constructor
     * 
     * @param id Uniquely identifies a statistic in a Stats objecy
     * @param name Name of the statistic
     * @param unit Unit of the statistic
     * @param type Type of the statistic (defined in PmiConstants)
     * @param level Instrumentation level (defined in PmiConstants)
     * @param description Desription of the statistic
     * @param resettable Indicates if this statistic can be reset to zero in client side
     */
    public PmiDataInfo(int id, String name, String unit, String description, int type, int level, boolean resettable) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.description = description;
        this.type = type;
        this.level = level;
        this.resettable = resettable;
    }

    /** (WebSphere internal use only) */
    public void setName(String name) {
        this.name = name;
    }

    /** (WebSphere internal use only) */
    public void setType(int type) {
        this.type = type;
        if (level == LEVEL_UNDEFINED) {
            switch (type) {
                case TYPE_INT:
                case TYPE_LONG:
                case TYPE_DOUBLE:
                    level = LEVEL_LOW;
                    break;
                case TYPE_STAT:
                    level = LEVEL_MEDIUM;
                    unit = "unit.ms";
                    break;
                case TYPE_LOAD:
                    level = LEVEL_HIGH;
                    break;
                case TYPE_SUBMODULE:
                    level = LEVEL_MAX;
                    break;
                default:
                    break;
            }
        }
    }

    /** (WebSphere internal use only) */
    public void setParticipation(String participation) {
        this.participation = participation;
    }

    /** (WebSphere internal use only) */
    public void setDescription(String description) {
        this.description = description;
    }

    /** (WebSphere internal use only) */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /** (WebSphere internal use only) */
    public void setCategory(String category) {
        this.category = category;
    }

    /** (WebSphere internal use only) */
    public void setLevel(int level) {
        this.level = level;
    }

    /** (WebSphere internal use only) */
    public void setStatisticSet(String statSet) {
        this.statSet = statSet;
    }

    /** (WebSphere internal use only) */
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /** (WebSphere internal use only) */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** (WebSphere internal use only) */
    public void setResettable(boolean resettable) {
        this.resettable = resettable;
    }

    /** (WebSphere internal use only) */
    public void setAggregatable(boolean aggregatable) {
        this.aggregatable = aggregatable;
    }

    /** (WebSphere internal use only) */
    public void setZosAggregatable(boolean zosAggregatable) {
        this.zosAggregatable = zosAggregatable;
    }

    /** (WebSphere internal use only) */
    public void setOnRequest(boolean onRequest) {
        this.onRequest = onRequest;
    }

    /** (WebSphere internal use only) */
    public void setSubmoduleName(String submoduleName) {
        this.submoduleName = submoduleName;
    }

    /** (WebSphere internal use only) */
    public void addDependency(int id) {
        if (dependencyList == null) {
            dependencyList = new ArrayList(2);
        }

        dependencyList.add(new Integer(id));
    }

    /** Returns the list of statistics that this statistic depends on */
    public ArrayList getDependency() {
        return dependencyList;
    }

    /**
     * Returns the name of the statsitic
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the statistic ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the statistic type (defined in PmiConstants)
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return the statistic unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Return the applicable category of this statistic. For example, an EJB counter could be only applicable to entity bean.
     */
    public String getCategory() {
        return category;
    }

    /** @deprecated No replacement */
    public String getParticipation() {
        return participation;
    }

    /**
     * Returns the statistic instrumentaion level (List of levels defined in PmiConstants)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the statistic set that this statistic belongs to (Statistic sets defined in com.ibm.websphere.pmi.stat.StatConstants)
     */
    public String getStatisticSet() {
        return statSet;
    }

    /**
     * 
     * Returns the platform in which this statistic is supported (List of platforms defined in PmiConstants)
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Returns the comment string for this statistic
     */
    public String getComment() {
        return comment;
    }

    /**
     * Return true if it can be reset to zero in client side.
     */
    public boolean isResettable() {
        return resettable;
    }

    /**
     * Return true if this statistic can be aggregated by the parent
     */
    public boolean isAggregatable() {
        return aggregatable;
    }

    /**
     * Return true if the value of statistic from zos servant regions is aggregatable
     */
    public boolean isZosAggregatable() {
        return zosAggregatable;
    }

    /**
     * Return true if this statistic is available in the given platform
     * 
     * @param p - platform string defined in PmiConstants
     */
    public boolean isAvailableInPlatform(String p) {
        if (this.platform.equals(PLATFORM_ALL))
            return true;
        else
            return (this.platform.equals(p));
    }

    /**
     * Return true if this statistic is updated only on request
     */
    public boolean isUpdateOnRequest() {
        //System.out.println("Hellooooooo");
        return onRequest;
    }

    /**
     * Returns the PMI sub-module name to which this statistic belongs to.
     */
    public String getSubmoduleName() {
        return submoduleName;
    }

    /** @deprecated No replacement */
    void print(PrintWriter pw) {
        pw.println("id:" + id);
        pw.println("name:" + name);
        pw.println("type:" + type);
        pw.println("participation:" + participation);
        pw.println("level:" + level);
        pw.println("description:" + description);
        pw.println("comment:" + comment);
        pw.println("resettable:" + resettable);
        pw.println("submoduleName:" + submoduleName);
    }

    /**
     * Returns String representation of this object
     */
    public String toString() {
        StringBuffer b = new StringBuffer("{name=").append(name);
        b.append(", ID=").append(id);
        //b.append (", type=").append(com.ibm.ws.pmi.stat.StatsConfigHelper.getStatsType (type));
        b.append(", description=").append(description);
        b.append(", unit=").append(unit);
        //b.append (", level=").append(com.ibm.ws.pmi.stat.StatsConfigHelper.getLevelString (level));
        b.append(", statisticSet=").append(statSet);
        b.append(", resettable=").append(resettable);
        b.append(", aggregatable=").append(aggregatable);
        b.append(", zosAggregatable=").append(zosAggregatable);
        if (submoduleName != null)
            b.append(", submoduleName:").append(submoduleName);

        b.append("}");

        return b.toString();
    }

    /**
     * Creates a copy of this object
     * 
     * @return a copy of this object
     */
    public PmiDataInfo copy() {
        PmiDataInfo r = new PmiDataInfo(id);

        // name is translatable
        if (name != null)
            r.name = new String(name);

        // description is translatable
        if (description != null)
            r.description = new String(description);

        // unit is translatable
        if (unit != null)
            r.unit = new String(unit);

        r.category = category;
        r.type = type;
        r.level = level;
        r.resettable = resettable;
        r.aggregatable = aggregatable;
        r.zosAggregatable = zosAggregatable;
        r.onRequest = onRequest;
        r.statSet = statSet;
        r.platform = platform;
        r.submoduleName = submoduleName;
        r.participation = participation;
        r.comment = comment;
        r.dependencyList = dependencyList;

        return r;
    }
}
