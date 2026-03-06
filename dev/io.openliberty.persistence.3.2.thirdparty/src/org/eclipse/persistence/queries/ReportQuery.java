/*
 * Copyright (c) 1998, 2025 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.queries;

import org.eclipse.persistence.descriptors.VersionLockingPolicy;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.internal.descriptors.OptimisticLockingPolicy;
import org.eclipse.persistence.internal.expressions.ConstantExpression;
import org.eclipse.persistence.internal.expressions.FunctionExpression;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.NonSynchronizedVector;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.queries.ExpressionQueryMechanism;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.queries.ReportItem;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.internal.sessions.remote.RemoteSessionController;
import org.eclipse.persistence.internal.sessions.remote.Transporter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * <b>Purpose</b>: Query for information about a set of objects instead of the objects themselves.
 * This supports select single attributes, nested attributes, aggregation functions and group bys.<p>
 *
 * <b>Attribute Types</b>:<ol>
 * <li>addAttribute("directQueryKey") is a short cut method to add an attribute with the same name as its corresponding direct query key.
 * <li>addAttribute("attributeName", expBuilder.get("oneToOneMapping").get("directQueryKey")) is the full approach for get values through joined 1:1 relationships.
 * <li>addAttribute("attributeName", expBuilder.getField("TABLE.FIELD")) allows the addition of raw values or values which were not mapped in the object model directly (i.e. FK attributes).
 * <li>addAttribute("attributeName", null) Leave a place holder (NULL) value in the result (used for included values from other systems or calculated values).
 * </ol>
 * <b>Retrieving Primary Keys</b>:     It is possible to retrieve the primary key raw values within each result, but stored in a separate (internal) vector. This
 *                                            primary key vector can later be used to retrieve the real object.
 *                                            @see #retrievePrimaryKeys()
 *                                            If the values are wanted in the result array then they must be added as attributes. For primary keys which are not mapped directly
 *                                            you can add them as DatabaseFields (see above).
 *
 * @author Doug Clarke
 * @since TOPLink/Java 2.0
 */
public class ReportQuery extends ReadAllQuery {

    /** Default, returns ReportQueryResult objects. */
    public static final int ShouldReturnReportResult = 0;

    /** Simplifies the result by only returning the first result. */
    public static final int ShouldReturnSingleResult = 1;

    /** Simplifies the result by only returning one value. */
    public static final int ShouldReturnSingleValue = 2;

    /** Simplifies the result by only returning the single attribute(as opposed to wrapping in a
    ReportQueryResult). */
    public static final int ShouldReturnSingleAttribute = 3;

    /** For EJB 3 support returns results without using the ReportQueryResult */
    public static final int ShouldReturnWithoutReportQueryResult = 4;

    /** For EJB 3 support returns results as an Object array. */
    public static final int ShouldReturnArray = 5;

    /** For example, ... EXISTS( SELECT 1 FROM ... */
    public static final int ShouldSelectValue1 = 6;

    /** For example, ... SELECT ID(e) FROM Entity e... */
    public static final int ShouldReturnPKClassInstance = 7;

    /** Specifies whether to retrieve primary keys, first primary key, or no primary key.*/
    public static final int FULL_PRIMARY_KEY = 2;
    public static final int FIRST_PRIMARY_KEY = 1;
    public static final int NO_PRIMARY_KEY = 0;

    //GF_ISSUE_395
    private enum ResultStatus { IGNORED };
    //end GF_ISSUE

    /** Flag indicating whether the primary key values should also be retrieved for the reference class. */
    protected int shouldRetrievePrimaryKeys;

    /** Collection of names for use by results. */
    protected List<String> names;

    /** Items to be selected, these could be attributes or aggregate functions. */
    protected List<ReportItem> items;

    /** Expressions representing fields to be used in the GROUP BY clause. */
    protected List<Expression> groupByExpressions;

    /** Expression representing the HAVING clause. */
    protected Expression havingExpression;

    /** Can be one of (ShouldReturnSingleResult, ShouldReturnSingleValue, ShouldReturnSingleAttribute)
     ** Simplifies the result by only returning the first result, first value, or all attribute values
     */
    protected int returnChoice;

    /** flag to allow items to be added to the last ConstructorReportItem **/
    protected boolean addToConstructorItem;

    /* GF_ISSUE_395 this attribute stores a set of unique keys that identity results.
     * Used when distinct has been set on the query.  For use in TCK
     */
    protected Set<Object> returnedKeys;

    private boolean hasIDFunctionSelectItemOnly = false;

    //Optional flag which says which type (Java class instance) should be produced as a result from query execution
    private Class<?> resultClass;

    /**
     * INTERNAL:
     * The builder should be provided.
     */
    public ReportQuery() {
        this.queryMechanism = new ExpressionQueryMechanism(this);
        this.items = new ArrayList<>();
        this.shouldRetrievePrimaryKeys = NO_PRIMARY_KEY;
        this.addToConstructorItem = false;

        // overwrite the lock mode to NO_LOCK, this prevents the report query to lock
        // when DEFAULT_LOCK_MODE and a pessimistic locking policy are used.
        setLockMode(ObjectBuildingQuery.NO_LOCK);
        this.shouldUseSerializedObjectPolicy = false;
    }

    public ReportQuery(Class<?> javaClass, Expression expression) {
        this();
        this.defaultBuilder = expression.getBuilder();
        setReferenceClass(javaClass);
        setSelectionCriteria(expression);
    }

    /**
     * PUBLIC:
     * The report query is require to be constructor with an expression builder.
     * This build must be used for the selection critiera, any item expressions, group bys and order bys.
     */
    public ReportQuery(Class<?> javaClass, ExpressionBuilder builder) {
        this();
        this.defaultBuilder = builder;
        setReferenceClass(javaClass);
    }

    /**
     * PUBLIC:
     * The report query is require to be constructor with an expression builder.
     * This build must be used for the selection critiera, any item expressions, group bys and order bys.
     */
    public ReportQuery(ExpressionBuilder builder) {
        this();
        this.defaultBuilder = builder;
    }

    /**
     * PUBLIC:
     * Add the attribute from the reference class to be included in the result.
     * EXAMPLE: reportQuery.addAttribute("firstName");
     */
    public void addAttribute(String itemName) {
        addItem(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the attribute to be included in the result.
     * EXAMPLE: reportQuery.addAttribute("city", expBuilder.get("address").get("city"));
     */
    public void addAttribute(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression);
    }

    /**
     * PUBLIC:
     * Add the attribute to be included in the result.  Return the result as the provided class
     * EXAMPLE: reportQuery.addAttribute("city", expBuilder.get("period").get("startTime"), Time.class);
     */
    public void addAttribute(String itemName, Expression attributeExpression, Class<?> type) {
        addItem(itemName, attributeExpression, type);
    }

    /**
     * PUBLIC:
     * Add the average value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addAverage("salary");
     */
    public void addAverage(String itemName) {
        addAverage(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the average value of the attribute to be included in the result and
     * return it as the specified resultType.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addAverage("salary", Float.class);
     */
    public void addAverage(String itemName, Class<?> resultType) {
        addAverage(itemName, getExpressionBuilder().get(itemName), resultType);
    }

    /**
     * PUBLIC:
     * Add the average value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addAverage("managerSalary", expBuilder.get("manager").get("salary"));
     */
    public void addAverage(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.average());
    }

    /**
     * PUBLIC:
     * Add the average value of the attribute to be included in the result and
     * return it as the specified resultType.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addAverage("managerSalary", expBuilder.get("manager").get("salary"), Double.class);
     */
    public void addAverage(String itemName, Expression attributeExpression, Class<?> resultType) {
        addItem(itemName, attributeExpression.average(), resultType);
    }

    /**
     * PUBLIC:
     * Add a ConstructorReportItem to this query's set of return values.
     * @param item used to specify a class constructor and values to pass in from this query
     * @see ConstructorReportItem
     */
    public void addConstructorReportItem(ConstructorReportItem item){
        addItem(item);
    }

    /**
     * PUBLIC:
     * Include the number of rows returned by the query in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE:
     * Java:
     *     reportQuery.addCount();
     * SQL:
     *     SELECT COUNT (*) FROM ...
     * @see #addCount(java.lang.String)
     */
    public void addCount() {
        addCount("COUNT", getExpressionBuilder());
    }

    /**
     * PUBLIC:
     * Include the number of rows returned by the query in the result, where attributeExpression is not null.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * <p>Example:
     * <blockquote><pre>
     * TopLink:    reportQuery.addCount("id");
     * SQL: SELECT COUNT (t0.EMP_ID) FROM EMPLOYEE t0, ...
     * </pre></blockquote>
     * @param attributeName the number of rows where attributeName is not null will be returned.
     * @see #addCount(java.lang.String, org.eclipse.persistence.expressions.Expression)
     */
    public void addCount(String attributeName) {
        addCount(attributeName, getExpressionBuilder().get(attributeName));
    }

    /**
     * PUBLIC:
     * Include the number of rows returned by the query in the result, where attributeExpression is not null.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * Set the count to be returned as the specified resultType.
     * <p>Example:
     * <blockquote><pre>
     * TopLink:    reportQuery.addCount("id", Long.class);
     * SQL: SELECT COUNT (t0.EMP_ID) FROM EMPLOYEE t0, ...
     * </pre></blockquote>
     * @param attributeName the number of rows where attributeName is not null will be returned.
     * @see #addCount(java.lang.String, org.eclipse.persistence.expressions.Expression)
     */
    public void addCount(String attributeName, Class<?> resultType) {
        addCount(attributeName, getExpressionBuilder().get(attributeName), resultType);
    }

    /**
     * PUBLIC:
     * Include the number of rows returned by the query in the result, where attributeExpression
     * is not null.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * <p>Example:
     * <blockquote><pre>
     * TopLink:    reportQuery.addCount("Count", getExpressionBuilder().get("id"));
     * SQL: SELECT COUNT (t0.EMP_ID) FROM EMPLOYEE t0, ...
     * </pre></blockquote>
     * <p>Example: counting only distinct values of an attribute.
     * <blockquote><pre>
     *  TopLink: reportQuery.addCount("Count", getExpressionBuilder().get("address").distinct());
     *  SQL: SELECT COUNT (DISTINCT t0.ADDR_ID) FROM EMPLOYEE t0, ...
     * </pre></blockquote>
     * objectAttributes can be specified also, even accross many to many
     * mappings.
     * @see #addCount()
     */
    public void addCount(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.count());
    }

    /**
     * PUBLIC:
     * Include the number of rows returned by the query in the result, where attributeExpression
     * is not null.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * Set the count to be returned as the specified resultType.
     * <p>Example:
     * <blockquote><pre>
     * TopLink:    reportQuery.addCount("Count", getExpressionBuilder().get("id"), Integer.class);
     * SQL: SELECT COUNT (t0.EMP_ID) FROM EMPLOYEE t0, ...
     * </pre></blockquote>
     * <p>Example: counting only distinct values of an attribute.
     * <blockquote><pre>
     *  TopLink: reportQuery.addCount("Count", getExpressionBuilder().get("address").distinct());
     *  SQL: SELECT COUNT (DISTINCT t0.ADDR_ID) FROM EMPLOYEE t0, ...
     * </pre></blockquote>
     * objectAttributes can be specified also, even accross many to many
     * mappings.
     * @see #addCount()
     */
    public void addCount(String itemName, Expression attributeExpression, Class<?> resultType) {
        addItem(itemName, attributeExpression.count(), resultType);
    }

    /**
     * ADVANCED:
     * Add the function against the attribute expression to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * Example: reportQuery.addFunctionItem("average", expBuilder.get("salary"), "average");
     */
    public void addFunctionItem(String itemName, Expression attributeExpression, String functionName) {
        Expression functionExpression = attributeExpression;
        functionExpression = attributeExpression.getFunction(functionName);

        ReportItem item = new ReportItem(itemName, functionExpression);
        addItem(item);

        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Add the attribute to the group by expressions.
     * This will group the result set on that attribute and is normally used in conjunction with aggregation functions.
     * Example: reportQuery.addGrouping("lastName")
     */
    public void addGrouping(String attributeName) {
        addGrouping(getExpressionBuilder().get(attributeName));
    }

    /**
     * PUBLIC:
     * Add the attribute expression to the group by expressions.
     * This will group the result set on that attribute and is normally used in conjunction with aggregation functions.
     * Example: reportQuery.addGrouping(expBuilder.get("address").get("country"))
     */
    public void addGrouping(Expression expression) {
        getGroupByExpressions().add(expression);
        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Add the expression to the query to be used in the HAVING clause.
     * This epression will be used to filter the result sets after they are grouped.  It must be used in conjunction with the GROUP BY clause.
     * <p>Example:
     * <p>reportQuery.setHavingExpression(expBuilder.get("address").get("country").equal("Canada"))
     */
    public void setHavingExpression(Expression expression) {
        havingExpression = expression;
        setIsPrepared(false);
    }

    /**
     * INTERNAL:
     * Method used to abstract addToConstructorItem behavour from the public addItem methods
     */
    private void addItem(ReportItem item){
        if (this.addToConstructorItem && (!getItems().isEmpty()) && (getItems().get(getItems().size() - 1).isConstructorItem())) {
            ((ConstructorReportItem)getItems().get(getItems().size() - 1)).addItem(item);
        } else {
            getItems().add(item);
        }
        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * ADVANCED:
     * Add the expression value to be included in the result.
     * EXAMPLE: reportQuery.addItem("name", expBuilder.get("firstName").toUpperCase());
     */
    public void addItem(String itemName, Expression attributeExpression) {
        ReportItem item = new ReportItem(itemName, attributeExpression);
        addItem(item);

        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * ADVANCED:
     * Add the expression value to be included in the result.
     * EXAMPLE: reportQuery.addItem("name", expBuilder.get("firstName").toUpperCase());
     */
    public void addItem(String itemName, Expression attributeExpression, List joinedExpressions) {
        ReportItem item = new ReportItem(itemName, attributeExpression);
        if (joinedExpressions != null && ! joinedExpressions.isEmpty()){
            item.getJoinedAttributeManager().setJoinedAttributeExpressions_(joinedExpressions);
        }
        addItem(item);
    }

    /**
     * INTERNAL:
     * Add the expression value to be included in the result.
     * EXAMPLE: reportQuery.addItem("name", expBuilder.get("firstName").toUpperCase());
     * The resultType can be specified to support EJBQL that adheres to the
     * EJB 3.0 spec.
     */
    protected void addItem(String itemName, Expression attributeExpression, Class<?> resultType) {
        ReportItem item = new ReportItem(itemName, attributeExpression);
        item.setResultType(resultType);
        addItem(item);
    }

    /**
     * PUBLIC:
     * Add the maximum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMaximum("salary");
     */
    public void addMaximum(String itemName) {
        addMaximum(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the maximum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMaximum("salary", Integer.class);
     */
    public void addMaximum(String itemName, Class<?> resultType) {
        addMaximum(itemName, getExpressionBuilder().get(itemName), resultType);
    }

    /**
     * PUBLIC:
     * Add the maximum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMaximum("managerSalary", expBuilder.get("manager").get("salary"));
     */
    public void addMaximum(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.maximum());
    }

    /**
     * PUBLIC:
     * Add the maximum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMaximum("managerSalary", expBuilder.get("manager").get("salary"), Integer.class);
     */
    public void addMaximum(String itemName, Expression attributeExpression, Class<?> resultType) {
        addItem(itemName, attributeExpression.maximum(), resultType);
    }

    /**
     * PUBLIC:
     * Add the minimum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMinimum("salary");
     */
    public void addMinimum(String itemName) {
        addMinimum(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the minimum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMinimum("salary", Integer.class);
     */
    public void addMinimum(String itemName, Class<?> resultType) {
        addMinimum(itemName, getExpressionBuilder().get(itemName), resultType);
    }

    /**
     * PUBLIC:
     * Add the minimum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMinimum("managerSalary", expBuilder.get("manager").get("salary"));
     */
    public void addMinimum(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.minimum());
    }

    /**
     * PUBLIC:
     * Add the minimum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addMinimum("managerSalary", expBuilder.get("manager").get("salary"), Integer.class);
     */
    public void addMinimum(String itemName, Expression attributeExpression, Class<?> resultType) {
        addItem(itemName, attributeExpression.minimum(), resultType);
    }

    /**
     * PUBLIC:
     * Add the standard deviation value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addStandardDeviation("salary");
     */
    public void addStandardDeviation(String itemName) {
        addStandardDeviation(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the standard deviation value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addStandardDeviation("managerSalary", expBuilder.get("manager").get("salary"));
     */
    public void addStandardDeviation(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.standardDeviation());
    }

    /**
     * PUBLIC:
     * Add the sum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addSum("salary");
     */
    public void addSum(String itemName) {
        addSum(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the sum value of the attribute to be included in the result and
     * return it as the specified resultType.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addSum("salary", Float.class);
     */
    public void addSum(String itemName, Class<?> resultType) {
        addSum(itemName, getExpressionBuilder().get(itemName), resultType);
    }

    /**
     * PUBLIC:
     * Add the sum value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addSum("managerSalary", expBuilder.get("manager").get("salary"));
     */
    public void addSum(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.sum());
    }

    /**
     * PUBLIC:
     * Add the sum value of the attribute to be included in the result and
     * return it as the specified resultType.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addSum("managerSalary", expBuilder.get("manager").get("salary"), Float.class);
     */
    public void addSum(String itemName, Expression attributeExpression, Class<?> resultType) {
        addItem(itemName, attributeExpression.sum(), resultType);
    }

    /**
     * PUBLIC:
     * Add the variance value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addVariance("salary");
     */
    public void addVariance(String itemName) {
        addVariance(itemName, getExpressionBuilder().get(itemName));
    }

    /**
     * PUBLIC:
     * Add the variance value of the attribute to be included in the result.
     * Aggregation functions can be used with a group by, or on the entire result set.
     * EXAMPLE: reportQuery.addVariance("managerSalary", expBuilder.get("manager").get("salary"));
     */
    public void addVariance(String itemName, Expression attributeExpression) {
        addItem(itemName, attributeExpression.variance());
    }

    /**
     * PUBLIC: Call a constructor for the given class with the results of this query.
     */
    public ConstructorReportItem beginAddingConstructorArguments(Class<?> constructorClass){
        ConstructorReportItem citem = new ConstructorReportItem(constructorClass.getName());
        citem.setResultType(constructorClass);
        //add directly to avoid addToConstructorItem behavior
        getItems().add(citem);
        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
        this.addToConstructorItem=true;
        return citem;
    }
    /**
     * PUBLIC: Call a constructor for the given class with the results of this query.
     * @param constructorArgTypes - sets the argument types to be passed to the constructor.
     */
    public ConstructorReportItem beginAddingConstructorArguments(Class<?> constructorClass, Class<?>[] constructorArgTypes){
        ConstructorReportItem citem =beginAddingConstructorArguments(constructorClass);
        citem.setConstructorArgTypes(constructorArgTypes);
        return citem;
    }

    /**
     * INTERNAL:
     * By default return the row.
     * Used by cursored stream.
     */
    @Override
    public Object buildObject(AbstractRecord row) {
        //Bug 445132 : Avoid NPE.
        Vector v = new Vector();
        v.add(row);
        return buildObject(row, v);
    }

    /**
     * INTERNAL:
     * Construct a result from a row. Either return a ReportQueryResult or just the attribute.
     * @param toManyJoinData All rows fetched by query.  It is required to be not null.
     */
    public Object buildObject(AbstractRecord row, Vector toManyJoinData) {
        ReportQueryResult reportQueryResult = new ReportQueryResult(this, row, toManyJoinData);
        //GF_ISSUE_395
        if (this.returnedKeys != null){
            if (this.returnedKeys.contains(reportQueryResult.getResultKey())){
                return ResultStatus.IGNORED; //distinguish between null values and thrown away duplicates
            } else {
                this.returnedKeys.add(reportQueryResult.getResultKey());
            }
        }
        //end GF_ISSUE_395

        
        if (!shouldReturnPKClassInstance()) {
            if (getReferenceClass() != null && getDescriptor() != null &&
                getDescriptor().getCMPPolicy() != null) {
                List<DatabaseField> pkFields = getDescriptor().getPrimaryKeyFields();
                // If composite key (multiple PK fields) and result size matches PK field count
                if (pkFields != null && pkFields.size() > 1 &&
                    reportQueryResult.getResults().size() == pkFields.size()) {
                    // Composite key detected - switch to PK class instance mode
                    setReturnPKClassInstance();
                }
            }
        }

        //handle case of TypedQuery like
        //TypedQuery<IdClassPK> query = em.createQuery("SELECT ID(THIS) FROM IdClassEntity WHERE (name = :nameParam) ORDER BY population DESC", IdClassPK.class);
        //where query return only SELECT ID(e) FROM .... and expected return type (some @IdClass) is specified
        if (shouldReturnPKClassInstance()) {
            int[] elementIndex;
            Object[] elements;
            if (getDescriptor().getCMPPolicy().getPKClass().isRecord()) {
                return getDescriptor().getObjectBuilder().buildNewRecordInstance(getDescriptor().getCMPPolicy().getPKClass(), getDescriptor().getObjectBuilder().getPrimaryKeyMappings(), row, session);
            } else {
                //Prepare data for POJO (accessors are used)
                elementIndex = new int[reportQueryResult.getResults().size()];
                elements = new Object[reportQueryResult.getResults().size()];
                for (int i = 0; i < reportQueryResult.getResults().size(); i++) {
                    elementIndex[i] = i;
                    elements[i] = reportQueryResult.getResults().get(i);
                }
            }
            return getDescriptor().getCMPPolicy().createPrimaryKeyInstanceFromPrimaryKeyValues(session, elementIndex, elements);
        } else if (shouldReturnSingleAttribute()) {
            return convertSingleQueryResult(reportQueryResult.getResults().get(0));
        } else if (shouldReturnArray()) {
            return reportQueryResult.toArray();
        } else if (shouldReturnWithoutReportQueryResult()) {
            if (reportQueryResult.size() == 1) {
                return convertSingleQueryResult(reportQueryResult.getResults().get(0));
            } else {
                return reportQueryResult.toArray();
            }
        } else if (shouldReturnSingleValue()) {
            return convertSingleQueryResult(reportQueryResult.getResults().get(0));
        } else {
            return reportQueryResult;
        }
    }

    private Object convertSingleQueryResult(Object queryResult) {
        if (this.getResultClass() != null) {
            return session.getPlatform().convertObject(queryResult, this.getResultClass());
        } else {
            return queryResult;
        }
    }

    /**
     * INTERNAL:
     * Construct a container of ReportQueryResult from the rows.
     * If only one result or value was asked for only return that.
     */
    public Object buildObjects(Vector rows) {
        if (shouldReturnSingleResult() || shouldReturnSingleValue()) {
            if (rows.isEmpty()) {
                return null;
            }
            return buildObject((AbstractRecord)rows.get(0), rows);
        }

        ContainerPolicy containerPolicy = getContainerPolicy();
        int size = rows.size();
        Object reportResults = containerPolicy.containerInstance(size);
        // GF_ISSUE_395
        if (shouldDistinctBeUsed()){
            this.returnedKeys = new HashSet(size);
        }
        //end GF_ISSUE
        //If only the attribute is desired, then buildObject will only get the first attribute each time
        for (int index = 0; index < size; index++) {
            // GF_ISSUE_395
            Object result = buildObject((AbstractRecord)rows.get(index), rows);
            if (result != ResultStatus.IGNORED) {
                containerPolicy.addInto(result, reportResults, this.session);
            }
            //end GF_ISSUE
        }
        if (shouldCacheQueryResults()) {
            setTemporaryCachedQueryResults(reportResults);
        }
        return reportResults;
    }

    /**
     * INTERNAL:
     * The cache check is done before the prepare as a hit will not require the work to be done.
     */
    @Override
    protected Object checkEarlyReturnLocal(AbstractSession session, AbstractRecord translationRow) {
        // Check for in-memory only query.
        if (shouldCheckCacheOnly()) {
            throw QueryException.cannotSetShouldCheckCacheOnlyOnReportQuery();
        } else {
            return null;
        }
    }

    /**
     * INTERNAL:
     * Check to see if a custom query should be used for this query.
     * This is done before the query is copied and prepared/executed.
     * null means there is none.
     */
    @Override
    protected DatabaseQuery checkForCustomQuery(AbstractSession session, AbstractRecord translationRow) {
        return null;
    }

    /**
     * INTERNAL:
     * Clone the query.
     */
    @Override
    public Object clone() {
        ReportQuery cloneQuery = (ReportQuery)super.clone();
        cloneQuery.items = new ArrayList<>(this.items.size());

        for (ReportItem item : this.items) {
            ReportItem newItem = (ReportItem)item.clone();
            if (item.getJoinedAttributeManagerInternal() != null){
                JoinedAttributeManager manager = item.getJoinedAttributeManager().clone();
                manager.setBaseQuery(cloneQuery);
                newItem.setJoinedAttributeManager(manager);
            }
            cloneQuery.addItem(newItem);
        }

        if (this.groupByExpressions != null) {
            cloneQuery.groupByExpressions = new ArrayList<>(this.groupByExpressions);
        }

        return cloneQuery;
    }

    /**
     * INTERNAL: Required for a very special case of bug 2612185:
     * ReportItems from parallelExpressions, on a ReportQuery which is a subQuery,
     * which is being batch read.
     * In a batch query the selection criteria is effectively cloned twice, meaning
     * the ReportItems need to be cloned an extra time also to stay in sync.
     * Each call to copiedVersionFrom() will take O(1) time as the expression was
     * already cloned.
     */
    public void copyReportItems(Map<Expression, Expression> alreadyDone) {
        this.items = new ArrayList<>(this.items);
        for (int i = this.items.size() - 1; i >= 0; i--) {
            ReportItem item = this.items.get(i);
            if (item.isConstructorItem()) {
                this.items.set(i, copyConstructorReportItem((ConstructorReportItem) item, alreadyDone));
            } else {
                this.items.set(i, copyReportItem(item, alreadyDone));
            }
        }
        if (this.groupByExpressions != null) {
            this.groupByExpressions = new ArrayList<>(this.groupByExpressions);
            for (int i = this.groupByExpressions.size() - 1; i >= 0; i--) {
                Expression item = this.groupByExpressions.get(i);
                if (alreadyDone.get(item.getBuilder()) != null) {
                    this.groupByExpressions.set(i, item.copiedVersionFrom(alreadyDone));
                }
            }
        }
        if (this.havingExpression != null) {
            this.havingExpression = this.havingExpression.copiedVersionFrom(alreadyDone);
        }
        if (this.orderByExpressions != null) {
            for (int i = this.orderByExpressions.size() - 1; i >= 0; i--) {
                Expression item = this.orderByExpressions.get(i);
                if (alreadyDone.get(item.getBuilder()) != null) {
                    this.orderByExpressions.set(i, item.copiedVersionFrom(alreadyDone));
                }
            }
        }
    }

    // copyReportItems helper
    private static ConstructorReportItem copyConstructorReportItem(ConstructorReportItem reportItem, Map<Expression, Expression> alreadyDone) {
        // Copy ReportItems list if exists
        List<ReportItem> reportItems = reportItem.getReportItems();
        List<ReportItem> newReportItems = reportItems != null ? new ArrayList<>(reportItems.size()) : null;
        if (reportItems != null) {
            for (ReportItem item : reportItems) {
                newReportItems.add(copyReportItem(item, alreadyDone));
            }
        }
        // Create new ConstructorReportItem
        ConstructorReportItem newItem = new ConstructorReportItem(reportItem.getName());
        newItem.setConstructor(reportItem.getConstructor());
        newItem.setResultType(reportItem.getResultType());
        newItem.setReportItems(newReportItems);
        newItem.setAttributeExpression(copyAttributeExpression(reportItem, alreadyDone));
        return newItem;
    }

    // copyReportItems helper
    private static Expression copyAttributeExpression(ReportItem reportItem, Map<Expression, Expression> alreadyDone) {
        Expression expression = reportItem.getAttributeExpression();
        if ((expression != null) && (alreadyDone.get(expression.getBuilder()) != null)) {
            expression = expression.copiedVersionFrom(alreadyDone);
        }
        return expression;
    }

    // copyReportItems helper
    private static ReportItem copyReportItem(ReportItem reportItem, Map<Expression, Expression> alreadyDone) {
        return new ReportItem(reportItem.getName(), copyAttributeExpression(reportItem, alreadyDone));
    }

    /**
     * PUBLIC:
     * Set if the query results should contain the primary keys or each associated object.
     * This make retrieving the real object easier.
     * By default they are not retrieved.
     */
    public void dontRetrievePrimaryKeys() {
        setShouldRetrievePrimaryKeys(false);
        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Don't simplify the result by returning the single attribute. Wrap in a ReportQueryResult.
     */
    public void dontReturnSingleAttribute() {
        if (shouldReturnSingleAttribute()) {
            this.returnChoice = 0;
        }
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning the first result.
     * This can be used if it known that only one row is returned by the report query.
     */
    public void dontReturnSingleResult() {
        if (shouldReturnSingleResult()) {
            this.returnChoice = 0;
        }
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public void dontReturnSingleValue() {
        if (shouldReturnSingleValue()) {
            this.returnChoice = 0;
        }
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public void dontReturnWithoutReportQueryResult() {
        if (shouldReturnWithoutReportQueryResult()) {
            this.returnChoice = 0;
        }
    }

    /**
     * PUBLIC:
     * Used in conjunction with beginAddingConstructorArguments to signal that expressions should no longer be
     * be added to the collection used in the constructor.
     */
    public void endAddingToConstructorItem(){
        this.addToConstructorItem = false;
    }

    /**
     * INTERNAL:
     * Execute the query.
     * Get the rows and build the objects or report data from the rows.
     * @exception  DatabaseException - an error has occurred on the database
     * @return either collection of objects, or report data resulting from execution of query.
     */
    @Override
    public Object executeDatabaseQuery() throws DatabaseException {
        // ensure a pessimistic locking query will go down the write connection
       if (isLockQuery() && getSession().isUnitOfWork()) {
            UnitOfWorkImpl unitOfWork = (UnitOfWorkImpl)getSession();
            // Note if a nested unit of work this will recursively start a
            // transaction early on the parent also.
            if (isLockQuery()) {
                if ((!unitOfWork.getCommitManager().isActive()) && (!unitOfWork.wasTransactionBegunPrematurely())) {
                    unitOfWork.beginTransaction();
                    unitOfWork.setWasTransactionBegunPrematurely(true);
                }
            }
        }
        if (getContainerPolicy().overridesRead()) {
            return getContainerPolicy().execute();
        }

        if (getQueryId() == 0) {
            setQueryId(getSession().getNextQueryId());
        }
        setExecutionTime(System.currentTimeMillis());
        if (getDescriptor().isDescriptorForInterface()) {
            return getDescriptor().getInterfacePolicy().selectAllObjectsUsingMultipleTableSubclassRead(this);
        }

        List<AbstractRecord> rows = getQueryMechanism().selectAllReportQueryRows();
        if ((this.batchFetchPolicy != null) && this.batchFetchPolicy.isIN()) {
            this.batchFetchPolicy.setDataResults(rows);
        }

        return buildObjects((Vector) rows);
    }

    /**
     * INTERNAL:
     * Extract the correct query result from the transporter.
     */
    @Override
    public Object extractRemoteResult(Transporter transporter) {
        return transporter.getObject();
    }

    /**
     * INTERNAL:
     * Return the group bys.
     */
    public List<Expression> getGroupByExpressions() {
        if (this.groupByExpressions == null) {
            this.groupByExpressions = new ArrayList<>();
        }
        return this.groupByExpressions;
    }

    /**
     * INTERNAL:
     * Return if any group bys exist, allow lazy initialization.
     * This should be called before calling getGroupByExpressions().
     */
    public boolean hasGroupByExpressions() {
        return (this.groupByExpressions != null) && (!this.groupByExpressions.isEmpty());
    }

    /**
     * INTERNAL:
     * Set the group bys.
     */
    public void setGroupByExpressions(List<Expression> groupByExpressions) {
        this.groupByExpressions = groupByExpressions;
    }

    /**
     * INTERNAL:
     * Return the Having expression.
     */
    public Expression getHavingExpression() {
        return havingExpression;
    }

    /**
     * INTERNAL:
     * return a collection of expressions if PK's are used.
     */
    public Vector getQueryExpressions() {
        Vector fieldExpressions = NonSynchronizedVector.newInstance(getItems().size());

        if (shouldSelectValue1()) {
            Expression one = new ConstantExpression(1, new ExpressionBuilder());
            this.addItem("one", one);
            this.dontUseDistinct();
            fieldExpressions.add(one);
        } else
        // For bug 3115576 and an EXISTS subquery only need to return a single field.
        if (shouldRetrieveFirstPrimaryKey()) {
            if (!getDescriptor().getPrimaryKeyFields().isEmpty()) {
                fieldExpressions.add(getDescriptor().getPrimaryKeyFields().get(0));
            }
        }
        if (shouldRetrievePrimaryKeys()) {
            fieldExpressions.addAll(getDescriptor().getPrimaryKeyFields());
        }

        return fieldExpressions;
    }

    /**
     * INTERNAL:
     * Returns the specific default redirector for this query type.  There are numerous default query redirectors.
     * See ClassDescriptor for their types.
     */
    @Override
    protected QueryRedirector getDefaultRedirector(){
        return descriptor.getDefaultReportQueryRedirector();
    }

    /**
     * INTERNAL:
     * @return ReportItems defining the attributes to be read.
     */
    public List<ReportItem> getItems() {
        return items;
    }

    /**
     * INTERNAL:
     * @return ReportItems with the name
     */
    public ReportItem getItem(String name) {
        for (ReportItem item : this.items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    /**
     * INTERNAL:
     * Set the ReportQueryItems defining the attributes to be read.
     */
    public void setItems(List<ReportItem> items) {
        this.items = items;
    }

    /**
     * INTERNAL:
     * Sets a {@code jakarta.persistence.LockModeType} to used with this queries execution.
     * <p><br>
     * The valid types are:<ul>
     * <li>{@code WRITE}</li>
     * <li>{@code READ}</li>
     * <li>{@code OPTIMISTIC}</li>
     * <li>{@code OPTIMISTIC_FORCE_INCREMENT}</li>
     * <li>{@code PESSIMISTIC}</li>
     * <li>{@code PESSIMISTIC_FORCE_INCREMENT}</li>
     * <li>{@code NONE}</li></ul>
     * Setting a null type will do nothing.
     * @return returns a failure flag indicating that we were UNABLE to set the
     * lock mode because of validation. Callers to this method should check the
     * return value and throw the necessary exception.
     */
    @Override
    public boolean setLockModeType(String lockModeType, AbstractSession session) {
        if (lockModeType != null) {
            if (super.setLockModeType(lockModeType, session)) {
                return true;
            } else {
                // When a lock mode is used, we must validate that our report items
                // all have a version locking policy if the lock is set to anything
                // but PESSIMISTIC and NONE. Validate only those report items that
                // are expression builders (ignoring the others)
                if (! lockModeType.equals(PESSIMISTIC_READ) && ! lockModeType.equals(PESSIMISTIC_WRITE) && ! lockModeType.equals(NONE)) {
                    for (ReportItem reportItem : getItems()) {
                        if (reportItem.getAttributeExpression() != null && reportItem.getAttributeExpression().isExpressionBuilder()) {
                            OptimisticLockingPolicy lockingPolicy = reportItem.getDescriptor().getOptimisticLockingPolicy();

                            if (lockingPolicy == null || !(lockingPolicy instanceof VersionLockingPolicy)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * INTERNAL:
     * Clear the ReportQueryItems
     */
    public void clearItems() {
        this.items = new ArrayList<>();
        setIsPrepared(false);
    }

    /**
     * INTERNAL:
     * Lazily initialize and return the names of the items requested for use in each result object.
     */
    public List<String> getNames() {
        if (this.names == null) {
            this.names = new ArrayList<>();
            for (ReportItem item : getItems()) {
                this.names.add(item.getName());
            }
        }
        return this.names;
    }

    /**
     * INTERNAL:
     * Set the item names.
     */
    protected void setNames(List<String> names) {
        this.names = names;
    }

    /**
     * PUBLIC:
     * Return if this is a report query.
     */
    @Override
    public boolean isReportQuery() {
        return true;
    }

    /**
     * INTERNAL:
     * Prepare the receiver for execution in a session.
     * Initialize each item with its DTF mapping
     */
    @Override
    protected void prepare() throws QueryException {
        if (prepareFromCachedQuery()) {
            return;
        }
        // Oct 19, 2000 JED
        // Added exception to be thrown if no attributes have been added to the query
        if (!getItems().isEmpty()) {
            try {
                for (ReportItem item : getItems()) {
                    item.initialize(this);
                }
            } catch (QueryException exception) {
                exception.setQuery(this);
                throw exception;
            }
        } else {
            if ((!shouldRetrievePrimaryKeys()) && (!shouldRetrieveFirstPrimaryKey()) && !(shouldSelectValue1())) {
                throw QueryException.noAttributesForReportQuery(this);
            }
        }

        super.prepare();

    }

    /**
     * INTERNAL:
     * ReportQuery doesn't support fetch groups.
     */
    @Override
    public void prepareFetchGroup() throws QueryException {
        if(this.fetchGroup != null || this.fetchGroupName != null) {
            throw QueryException.fetchGroupNotSupportOnReportQuery();
        }
    }

    /**
     * INTERNAL:
     * Prepare the query from the prepared query.
     * This allows a dynamic query to prepare itself directly from a prepared query instance.
     * This is used in the EJBQL parse cache to allow preparsed queries to be used to prepare
     * dynamic queries.
     * This only copies over properties that are configured through EJBQL.
     */
    @Override
    public void prepareFromQuery(DatabaseQuery query) {
        super.prepareFromQuery(query);
        if (query.isReportQuery()) {
            ReportQuery reportQuery = (ReportQuery)query;
            this.names = reportQuery.names;
            this.items = reportQuery.items;
            this.groupByExpressions = reportQuery.groupByExpressions;
            this.havingExpression = reportQuery.havingExpression;
            this.returnChoice = reportQuery.returnChoice;
            this.returnedKeys = reportQuery.returnedKeys;
            this.shouldRetrievePrimaryKeys = reportQuery.shouldRetrievePrimaryKeys;
        }
    }

    /**
     * INTERNAL:
     * Return if the query is equal to the other.
     * This is used to allow dynamic expression query SQL to be cached.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!super.equals(object)) {
            return false;
        }
        ReportQuery query = (ReportQuery) object;
        List<ReportItem> items = getItems();
        List<ReportItem> otherItems = query.getItems();
        int size = items.size();
        if (size != otherItems.size()) {
            return false;
        }
        for (int index = 0; index < size; index++) {
            if (!items.get(index).equals(otherItems.get(index))) {
                return false;
            }
        }
        if (hasGroupByExpressions() && query.hasGroupByExpressions()) {
            List<Expression> groupBys = getGroupByExpressions();
            List<Expression> otherGroupBys = query.getGroupByExpressions();
            size = groupBys.size();
            if (size != otherGroupBys.size()) {
                return false;
            }
            for (int index = 0; index < size; index++) {
                if (!groupBys.get(index).equals(otherGroupBys.get(index))) {
                    return false;
                }
            }
        } else if (hasGroupByExpressions() || query.hasGroupByExpressions()) {
            return false;
        }
        if ((getHavingExpression() != query.getHavingExpression())
                && (getHavingExpression() != null)
                && (!getHavingExpression().equals(query.getHavingExpression()))) {
            return false;
        }
        if (this.returnChoice != query.returnChoice) {
            return false;
        }
        if (this.shouldRetrievePrimaryKeys != query.shouldRetrievePrimaryKeys) {
            return false;
        }
        return true;
    }

    /**
     * INTERNAL:
     * Prepare a report query with a count defined on an object attribute.
     * Added to fix bug 3268040, addCount(objectAttribute) not supported.
     */
    protected void prepareObjectAttributeCount(Map<Expression, Expression> clonedExpressions) {
        prepareObjectAttributeCount(getItems(), clonedExpressions);
    }

    /**
     * JPQL allows count([distinct] e), where e can be an object, not just a single field,
     * however the database only allows a single field, so object needs to be translated to a single field.
     * If the descriptor has a single pk, it is used, otherwise any pk is used if distinct, otherwise a subselect is used.
     * If the object was obtained through an outer join, then the subselect also will not work, so an error is thrown.
     */
    private void prepareObjectAttributeCount(List<ReportItem> items, Map<Expression, Expression> clonedExpressions) {
        int numOfReportItems = items.size();
        //gf675: need to loop through all items to fix all count(..) instances
        for (int i =0;i<numOfReportItems; i++){
            ReportItem item = items.get(i);
            if (item == null) {
                continue;
            } else if (item instanceof ConstructorReportItem) {
                // recursive call to process child ReportItems
                prepareObjectAttributeCount(((ConstructorReportItem)item).getReportItems(), clonedExpressions);
            } else if (item.getAttributeExpression() instanceof FunctionExpression count) {
                count.prepareObjectAttributeCount(null, item, this, clonedExpressions);
            }
        }
    }

    /**
     * INTERNAL:
     * Prepare the mechanism.
     */
    @Override
    protected void prepareSelectAllRows() {
        prepareObjectAttributeCount(null);

        getQueryMechanism().prepareReportQuerySelectAllRows();
    }

    /**
     * INTERNAL:
     * Prepare the receiver for being printed inside a subselect.
     * This prepares the statement but not the call.
     */
    public synchronized void prepareSubSelect(AbstractSession session, AbstractRecord translationRow, Map<Expression, Expression> clonedExpressions) throws QueryException {
        if (isPrepared()) {
            return;
        }

        setIsPrepared(true);
        setSession(session);
        setTranslationRow(translationRow);

        checkDescriptor(getSession());

        try {
            for (ReportItem item : getItems()) {
                item.initialize(this);
            }
        } catch (QueryException exception) {
            exception.setQuery(this);
            throw exception;
        }

        prepareObjectAttributeCount(clonedExpressions);

        getQueryMechanism().prepareReportQuerySubSelect();

        setSession(null);
        setTranslationRow(null);
    }

    /**
     * INTERNAL:
     * replace the value holders in the specified result object(s)
     */
    @Override
    public Map replaceValueHoldersIn(Object object, RemoteSessionController controller) {
        // do nothing, since report queries do not return domain objects
        return null;
    }

    /**
     * PUBLIC:
     * Set if the query results should contain the primary keys or each associated object.
     * This make retrieving the real object easier.
     * By default they are not retrieved.
     */
    public void retrievePrimaryKeys() {
        setShouldRetrievePrimaryKeys(true);
        //Bug2804042 Must un-prepare if prepared as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Return the return type.
     */
    public int getReturnType() {
        return returnChoice;
    }

    /**
     * PUBLIC:
     * Set the return type.
     * This can be one of several constants,
     * <ul>
     * <li>ShouldReturnReportResult - return {@literal List<ReportQueryResult>} : ReportQueryResult (Map) of each row is returned.
     * <li>ShouldReturnSingleResult - return ReportQueryResult : Only first row is returned.
     * <li>ShouldReturnSingleAttribute - return {@literal List<Object>} : Only first column of (all) rows are returned.
     * <li>ShouldReturnSingleValue - return Object : Only first value of first row is returned.
     * <li>ShouldReturnWithoutReportQueryResult - return {@literal List<Object[]>} : Array of each row is returned.
     * </ul>
     */
    public void setReturnType(int returnChoice) {
        this.returnChoice = returnChoice;
    }

    /**
     * PUBLIC:
     * Simplify the result by returning a single attribute. Don't wrap in a ReportQueryResult.
     */
    public void returnSingleAttribute() {
        returnChoice = ShouldReturnSingleAttribute;
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning the first result.
     * This can be used if it known that only one row is returned by the report query.
     */
    public void returnSingleResult() {
        returnChoice = ShouldReturnSingleResult;
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public void returnSingleValue() {
        returnChoice = ShouldReturnSingleValue;
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public void returnWithoutReportQueryResult() {
        this.returnChoice = ShouldReturnWithoutReportQueryResult;
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public void selectValue1() {
        returnChoice = ShouldSelectValue1;
    }

    /**
     * PUBLIC:
     * Simplify the result by returning an instance of IdClass. It's valid for queries like SELECT ID(e) FROM Entity e... and {@link jakarta.persistence.TypedQuery}
     */
    public void setReturnPKClassInstance() {
        this.returnChoice = ShouldReturnPKClassInstance;
    }

    /**
     * PUBLIC:
     * Set if the query results should contain the primary keys or each associated object.
     * This make retrieving the real object easier.
     * By default they are not retrieved.
     */
    public void setShouldRetrievePrimaryKeys(boolean shouldRetrievePrimaryKeys) {
        this.shouldRetrievePrimaryKeys = (shouldRetrievePrimaryKeys ? FULL_PRIMARY_KEY : NO_PRIMARY_KEY);
    }

    /**
     * ADVANCED:
     * Sets if the query results should contain the first primary key of each associated object.
     * Usefull if this is an EXISTS subquery and you don't care what fields are returned
     * so long as it is a single field.
     * The default value is false.
     * This should only be used with a subquery.
     */
    public void setShouldRetrieveFirstPrimaryKey(boolean shouldRetrieveFirstPrimaryKey) {
        this.shouldRetrievePrimaryKeys = (shouldRetrieveFirstPrimaryKey ? FIRST_PRIMARY_KEY : NO_PRIMARY_KEY);
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning the attribute (as opposed to wrapping in a ReportQueryResult).
     * This can be used if it is known that only one attribute is returned by the report query.
     */
    public void setShouldReturnSingleAttribute(boolean newChoice) {
        if (newChoice) {
            returnSingleAttribute();
        } else {
            dontReturnSingleAttribute();
        }
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning the first result.
     * This can be used if it known that only one row is returned by the report query.
     */
    public void setShouldReturnSingleResult(boolean newChoice) {
        if (newChoice) {
            returnSingleResult();
        } else {
            dontReturnSingleResult();
        }
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public void setShouldReturnSingleValue(boolean newChoice) {
        if (newChoice) {
            returnSingleValue();
        } else {
            dontReturnSingleValue();
        }
    }

    /**
     * PUBLIC:
     * Simplifies the result by returning a nested list instead of the ReportQueryResult.
     * This is used by EJB 3.
     */
    public void setShouldReturnWithoutReportQueryResult(boolean newChoice) {
        if (newChoice) {
            returnWithoutReportQueryResult();
        } else {
            dontReturnWithoutReportQueryResult();
        }
    }

    /**
     * PUBLIC:
     * Return if the query results should contain the primary keys or each associated object.
     * This make retrieving the real object easier.
     */
    public boolean shouldRetrievePrimaryKeys() {
        return this.shouldRetrievePrimaryKeys == FULL_PRIMARY_KEY;
    }

    /**
     * PUBLIC:
     * Return if the query results should contain the first primary key of each associated object.
     * Usefull if this is an EXISTS subquery and you don't care what fields are returned
     * so long as it is a single field.
     */
    public boolean shouldRetrieveFirstPrimaryKey() {
        return this.shouldRetrievePrimaryKeys == FIRST_PRIMARY_KEY;
    }

    /**
     * PUBLIC:
     * Answer if we are only returning the attribute (as opposed to wrapping in a ReportQueryResult).
     * This can be used if it is known that only one attribute is returned by the report query.
     */
    public boolean shouldReturnSingleAttribute() {
        return this.returnChoice == ShouldReturnSingleAttribute;
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning the first result.
     * This can be used if it known that only one row is returned by the report query.
     */
    public boolean shouldReturnSingleResult() {
        return this.returnChoice == ShouldReturnSingleResult;
    }

    /**
     * PUBLIC:
     * Simplifies the result by only returning a single value.
     * This can be used if it known that only one row is returned by the report query and only a single item is added
     * to the report.
     */
    public boolean shouldReturnSingleValue() {
        return this.returnChoice == ShouldReturnSingleValue;
    }

    /**
     * PUBLIC:
     * Simplifies the result by returning a nested list instead of the ReportQueryResult.
     * This is used by EJB 3.
     */
    public boolean shouldReturnWithoutReportQueryResult() {
        return this.returnChoice == ShouldReturnWithoutReportQueryResult;
    }

    /**
     * PUBLIC:
     * Returns true if results should be returned as an Object array.
     */
    public boolean shouldReturnArray() {
        return this.returnChoice == ShouldReturnArray;
    }

    /**
     * PUBLIC:
     * Returns true if results should be returned as an Object array.
     */
    public boolean shouldSelectValue1() {
        return this.returnChoice == ShouldSelectValue1;
    }

    /**
     * PUBLIC:
     * Returns true if results should be returned as instance of IdClass. It's valid for queries like SELECT ID(e) FROM Entity e...
     */
    public boolean shouldReturnPKClassInstance() {
        return this.returnChoice == ShouldReturnPKClassInstance;
    }

    public void setHasIDFunctionSelectItemOnly(boolean hasIDFunctionSelectItemOnly) {
        this.hasIDFunctionSelectItemOnly = hasIDFunctionSelectItemOnly;
    }

    public boolean hasIDFunctionSelectItemOnly() {
        return hasIDFunctionSelectItemOnly;
    }

    public Class<?> getResultClass() {
        return resultClass;
    }

    public void setResultClass(Class<?> resultClass) {
        this.resultClass = resultClass;
    }
}
