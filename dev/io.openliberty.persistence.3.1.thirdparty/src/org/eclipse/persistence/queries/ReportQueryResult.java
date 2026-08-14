/*
 * Copyright (c) 1998, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2026 IBM Corporation. All rights reserved.
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
//     11/10/2011-2.4 Guy Pelletier
//       - 357474: Address primaryKey option from tenant discriminator column
//     10/01/2018: Will Dazey
//       - #253: Add support for embedded constructor results with CriteriaBuilder
package org.eclipse.persistence.queries;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.expressions.FunctionExpression;
import org.eclipse.persistence.internal.expressions.MapEntryExpression;
import org.eclipse.persistence.internal.core.helper.CoreClassConstants;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.NonSynchronizedSubVector;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.queries.ReportItem;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedInvokeConstructor;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.mappings.AggregateObjectMapping;
import org.eclipse.persistence.mappings.Association;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.foundation.AbstractColumnMapping;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Session;

/**
 * <b>Purpose</b>: A single row (type) result for a ReportQuery<p>
 *
 * <b>Description</b>: Represents a single row of attribute values (converted using mapping) for
 * a ReportQuery. The attributes can be from various objects.
 *
 * <b>Responsibilities</b>:<ul>
 * <li> Converted field values into object attribute values.
 * <li> Provide access to values by index or item name
 * </ul>
 *
 * @author Doug Clarke
 * @since TOPLink/Java 2.0
 */
public class ReportQueryResult implements Serializable, Map {

    /** Item names to lookup result values */
    protected List<String> names;

    /** Actual converted attribute values */
    protected List<Object> results;

    /** Id value if the retrievPKs flag was set on the ReportQuery. These can be used to get the actual object */
    protected Object primaryKey;

    /** If an objectLevel distinct is used then generate unique key for this result */
    // GF_ISSUE_395
    protected StringBuffer key;

    /**
     * INTERNAL:
     * Used to create test results
     */
    public ReportQueryResult(List<Object> results, Object primaryKeyValues) {
        this.results = results;
        this.primaryKey = primaryKeyValues;
    }

    public ReportQueryResult(ReportQuery query, AbstractRecord row, Vector toManyResults) {
        super();
        this.names = query.getNames();
        buildResult(query, row, toManyResults);
    }

    /**
     * INTERNAL:
     * Create an array of attribute values (converted from raw field values using the mapping).
     */
    protected void buildResult(ReportQuery query, AbstractRecord row, Vector toManyData) {
        // GF_ISSUE_395
        if (query.shouldDistinctBeUsed() && (query.shouldFilterDuplicates())) {
            this.key = new StringBuffer();
        }

        if (query.shouldRetrievePrimaryKeys()) {
            setId(query.getDescriptor().getObjectBuilder().extractPrimaryKeyFromRow(row, query.getSession()));
            // For bug 3115576 this is only used for EXISTS sub-selects so no result is needed.
        }

        List<Object> results = new ArrayList<>();
        for(ReportItem item: query.getItems()) {
            if (item.isConstructorItem()) {
                Object result = processConstructorItem(query, row, toManyData, (ConstructorReportItem) item);
                results.add(result);
            } else if (item.getAttributeExpression() != null && item.getAttributeExpression().isClassTypeExpression()) {
                Object value = processItem(query, row, toManyData, item);
                ClassDescriptor descriptor = ((org.eclipse.persistence.internal.expressions.ClassTypeExpression)item.getAttributeExpression()).getContainingDescriptor(query);
                if (descriptor != null && descriptor.hasInheritance()) {
                    value = descriptor.getInheritancePolicy().classFromValue(value, query.getSession());
                } else {
                    value = query.getSession().getDatasourcePlatform().convertObject(value, Class.class);
                }
                results.add(value);
            } else {
                // Normal items
                Object value = processItem(query, row, toManyData, item);
                results.add(value);
            }
        }

        setResults(results);
    }

    private Object processConstructorItem(ReportQuery query, AbstractRecord row, Vector toManyData, ConstructorReportItem constructorItem) {
        // For constructor items need to process each constructor argument.
        Class<?>[] constructorArgTypes = constructorItem.getConstructorArgTypes();
        int numberOfArguments = constructorItem.getReportItems().size();
        Object[] constructorArgs = new Object[numberOfArguments];

        Constructor itemConstructor = constructorItem.getConstructor();
        Class<?>[] actualParamTypes = itemConstructor != null ? itemConstructor.getParameterTypes() : null;

        for (int argumentIndex = 0; argumentIndex < numberOfArguments; argumentIndex++) {
            ReportItem argumentItem = constructorItem.getReportItems().get(argumentIndex);
            Object result = null;
            if(argumentItem.isConstructorItem()) {
                result = processConstructorItem(query, row, toManyData, (ConstructorReportItem) argumentItem);
            } else {
                result = processItem(query, row, toManyData, argumentItem);
            }

            // Use actual constructor parameter type if constructorArgTypes is Object.class
            Class<?> targetType = constructorArgTypes[argumentIndex];
            if (targetType == CoreClassConstants.OBJECT && actualParamTypes != null && argumentIndex < actualParamTypes.length) {
                targetType = actualParamTypes[argumentIndex];
            }

            constructorArgs[argumentIndex] = ConversionManager.getDefaultManager().convertObject(result, targetType);
        }
        try {
            Constructor constructor = constructorItem.getConstructor();
            Object returnValue = null;
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    returnValue = AccessController.doPrivileged(new PrivilegedInvokeConstructor(constructor, constructorArgs));
                } catch (PrivilegedActionException exception) {
                    throw QueryException.exceptionWhileUsingConstructorExpression(exception.getException(), query);
                }
            } else {
                returnValue = PrivilegedAccessHelper.invokeConstructor(constructor, constructorArgs);
            }
            return returnValue;
        } catch (ReflectiveOperationException exc) {
            throw QueryException.exceptionWhileUsingConstructorExpression(exc, query);
        }
    }

    private Object processItemFromMapping(ReportQuery query, AbstractRecord row, DatabaseMapping mapping, ReportItem item, int itemIndex) {
        Object value = null;

        // If mapping is not null then it must be a direct mapping - see Reportitem.init.
        // Check for non database (EIS) records to use normal get.
        if (row instanceof DatabaseRecord) {
            value = row.getValues().get(itemIndex);
        } else {
            value = row.get(mapping.getField());
        }

        //Bug 421056: JPA 2.1; section 4.8.5
        if(item.getAttributeExpression().isFunctionExpression()) {
            FunctionExpression exp = (FunctionExpression) item.getAttributeExpression();
            int selector = exp.getOperator().getSelector();
            //a value of null for max/min implies no rows could be applied
            //we want to return null, per the spec, here before the mapping gets to alter the value
            if (value == null && ((selector == ExpressionOperator.Maximum) || (selector == ExpressionOperator.Minimum))) {
                return value;
            }
        }

        //If the mapping was set on the ReportItem, then use the mapping to convert the value
        if (mapping.isAbstractColumnMapping()) {
            value = ((AbstractColumnMapping)mapping).getObjectValue(value, query.getSession());
        } else if (mapping.isDirectCollectionMapping()) {
            value = ((DirectCollectionMapping)mapping).getObjectValue(value, query.getSession());
        }
        return value;
    }

    /**
     * INTERNAL:
     * Return a value from an item and database row (converted from raw field values using the mapping).
     */
    protected Object processItem(ReportQuery query, AbstractRecord row, Vector toManyData, ReportItem item) {
        JoinedAttributeManager joinManager = null;
        if (item.hasJoining()) {
            joinManager = item.getJoinedAttributeManager();
            if (joinManager.isToManyJoin()) {
                // PERF: Only reset data-result if unset, must only occur once per item, not per row (n vs n^2).
                if (joinManager.getDataResults_() == null) {
                    joinManager.setDataResults(new ArrayList(toManyData), query.getSession());
                }
            }
        }

        Object value = null;
        int rowSize = row.size();
        int itemIndex = item.getResultIndex();

        DatabaseMapping mapping = item.getMapping();
        ClassDescriptor descriptor = item.getDescriptor();

        if (item.getAttributeExpression() != null) {
            if (descriptor == null && mapping != null) {
                descriptor = mapping.getReferenceDescriptor();
            }
            if (mapping != null && (mapping.isAbstractColumnMapping() || mapping.isDirectCollectionMapping())) {

                if (itemIndex >= rowSize) {
                    throw QueryException.reportQueryResultSizeMismatch(itemIndex + 1, rowSize);
                }

                value = processItemFromMapping(query, row, mapping, item, itemIndex);

                // GF_ISSUE_395+
                if (this.key != null) {
                    this.key.append(value);
                    this.key.append("_");
                }
            } else if (descriptor != null) {
                // Item is for an object result.
                int size = descriptor.getAllSelectionFields(query).size();
                if (itemIndex + size > rowSize) {
                    throw QueryException.reportQueryResultSizeMismatch(itemIndex + size, rowSize);
                }
                AbstractRecord subRow = row;
                // Check if at the start of the row, then avoid building a subRow.
                if (itemIndex > 0) {
                    BatchFetchPolicy batchFetchPolicy = query.getBatchFetchPolicy();
                    if (batchFetchPolicy != null && batchFetchPolicy.isIN()) {

                        List<AbstractRecord> subRows = new ArrayList<>(toManyData.size());
                        for (AbstractRecord parentRow : (Vector<AbstractRecord>) toManyData) {
                            Vector<DatabaseField> trimedParentFields = new NonSynchronizedSubVector<>(parentRow.getFields(), itemIndex, rowSize);
                            Vector trimedParentValues = new NonSynchronizedSubVector<>(parentRow.getValues(), itemIndex, rowSize);
                            subRows.add(new DatabaseRecord(trimedParentFields, trimedParentValues));
                        }

                        for (DatabaseMapping subMapping : descriptor.getMappings()) {
                            batchFetchPolicy.setDataResults(subMapping, subRows);
                        }

                        subRow = subRows.get(toManyData.indexOf(row));
                    } else {
                        Vector<DatabaseField> trimedFields = new NonSynchronizedSubVector<>(row.getFields(), itemIndex, rowSize);
                        Vector trimedValues = new NonSynchronizedSubVector(row.getValues(), itemIndex, rowSize);
                        subRow = new DatabaseRecord(trimedFields, trimedValues);
                    }
                }
                if (mapping != null && mapping.isAggregateObjectMapping()){
                    value = ((AggregateObjectMapping)mapping).buildAggregateFromRow(subRow, null, null, joinManager, query, false, query.getSession(), true);
                } else {
                    //TODO : Support prefrechedCacheKeys in report query
                    value = descriptor.getObjectBuilder().buildObject(query, subRow, joinManager);
                }

                // this covers two possibilities
                // 1. We want the actual Map.Entry from the table rather than the just the key
                // 2. The map key is extracted from the owning object rather than built with
                // a specific mapping.  This could happen in a MapContainerPolicy
                if (item.getAttributeExpression().isMapEntryExpression() && mapping.isCollectionMapping()){
                    Object rowKey = null;
                    if (mapping.getContainerPolicy().isMapPolicy() && !mapping.getContainerPolicy().isMappedKeyMapPolicy()){
                        rowKey = mapping.getContainerPolicy().keyFrom(value, query.getSession());
                    } else {
                        rowKey = mapping.getContainerPolicy().buildKey(subRow, query, null, query.getSession(), true);
                    }
                    if (((MapEntryExpression)item.getAttributeExpression()).shouldReturnMapEntry()){
                        value = new Association(rowKey, value);
                    } else {
                        value = rowKey;
                    }
                }
                // GF_ISSUE_395
                if (this.key != null) {
                    Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromRow(subRow, query.getSession());
                    if (primaryKey != null){//GF3233 NPE is caused by processing the null PK being extracted from referenced target with null values in database.
                        this.key.append(primaryKey);
                    }
                    this.key.append("_");
                }
            } else {
                value = row.getValues().get(itemIndex);
                // GF_ISSUE_395
                if (this.key != null) {
                    this.key.append(value);
                }
            }
        }
        return value;
    }

    /**
     * PUBLIC:
     * Clear the contents of the result.
     */
    @Override
    public void clear() {
        this.names = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    /**
     * PUBLIC:
     * Check if the value is contained in the result.
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * PUBLIC:
     * Check if the key is contained in the result.
     */
    @Override
    public boolean containsKey(Object key) {
        return getNames().contains(key);
    }

    /**
     * PUBLIC:
     * Check if the value is contained in the result.
     */
    @Override
    public boolean containsValue(Object value) {
        return getResults().contains(value);
    }

    /**
     * PUBLIC:
     * Returns a set of the keys.
     */
    @Override
    public Set entrySet() {
        return new EntrySet();
    }


    /**
     * Defines the virtual entrySet.
     */
    protected class EntrySet extends AbstractSet {
        @Override
        public Iterator iterator() {
            return new EntryIterator();
        }
        @Override
        public int size() {
            return ReportQueryResult.this.size();
        }
        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Entry)) {
                return false;
            }
            return ReportQueryResult.this.containsKey(((Entry)object).getKey());
        }
        @Override
        public boolean remove(Object object) {
            if (!(object instanceof Entry)) {
                return false;
            }
            ReportQueryResult.this.remove(((Entry)object).getKey());
            return true;
        }
        @Override
        public void clear() {
            ReportQueryResult.this.clear();
        }
    }

    /**
     * Entry class for implementing Map interface.
     */
    protected static class RecordEntry implements Entry {
        Object key;
        Object value;

        public RecordEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Object setValue(Object value) {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Map.Entry)) {
                return false;
            }
            Map.Entry entry = (Map.Entry)object;
            return compare(key, entry.getKey()) && compare(value, entry.getValue());
        }

        @Override
        public int hashCode() {
            return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

        private boolean compare(Object object1, Object object2) {
            return (object1 == null ? object2 == null : object1.equals(object2));
        }
    }

    /**
     * Defines the virtual entrySet iterator.
     */
    protected class EntryIterator implements Iterator {
        int index;

        EntryIterator() {
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return this.index < ReportQueryResult.this.size();
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.index++;
            return new RecordEntry(getNames().get(this.index - 1), getResults().get(this.index - 1));
        }

        @Override
        public void remove() {
            if (this.index >= ReportQueryResult.this.size()) {
                throw new IllegalStateException();
            }
            ReportQueryResult.this.remove(getNames().get(this.index));
        }
    }

    /**
     * Defines the virtual keySet iterator.
     */
    protected class KeyIterator extends EntryIterator {
        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.index++;
            return getNames().get(this.index - 1);
        }
    }

    /**
     * PUBLIC:
     * Compare if the two results are equal.
     */
    @Override
    public boolean equals(Object anObject) {
        if (anObject instanceof ReportQueryResult) {
            return equals((ReportQueryResult)anObject);
        }

        return false;
    }

    /**
     * INTERNAL:
     * Used in testing to compare if results are correct.
     */
    public boolean equals(ReportQueryResult result) {
        if (this == result) {
            return true;
        }
        if (!getResults().equals(result.getResults())) {
            return false;
        }

        // Compare PKs
        if (getId() != null) {
            if (result.getId() == null) {
                return false;
            }
            return getId().equals(getId());
        }

        return true;
    }

    @Override
    public int hashCode() {
        List<Object> results = getResults();
        Object id = getId();
        int result = results != null ? results.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    /**
     * PUBLIC:
     * Return the value for given item name.
     */
    @Override
    public Object get(Object name) {
        if (name instanceof String) {
            return get((String)name);
        }

        return null;
    }

    /**
     * PUBLIC:
     * Return the value for given item name.
     */
    public Object get(String name) {
        int index = getNames().indexOf(name);
        if (index == -1) {
            return null;
        }

        return getResults().get(index);
    }

    /**
     * PUBLIC:
     * Return the indexed value from result.
     */
    public Object getByIndex(int index) {
        return getResults().get(index);
    }

    /**
     * INTERNAL:
     * Return the unique key for this result
     */
    public String getResultKey(){
        if (this.key != null){
            return this.key.toString();
        }
        return null;
    }

    /**
     * PUBLIC:
     * Return the names of report items, provided to ReportQuery.
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * PUBLIC:
     * Return the Id for the result or null if not requested.
     */
    public Object getId() {
        return primaryKey;
    }

    /**
     * PUBLIC:
     * Return the results.
     */
    public List<Object> getResults() {
        return results;
    }

    /**
     * PUBLIC:
     * Return if the result is empty.
     */
    @Override
    public boolean isEmpty() {
        return getNames().isEmpty();
    }

    /**
     * PUBLIC:
     * Returns a set of the keys.
     */
    @Override
    public Set keySet() {
        return new KeySet();
    }

    /**
     * Defines the virtual keySet.
     */
    protected class KeySet extends EntrySet {
        @Override
        public Iterator iterator() {
            return new KeyIterator();
        }
        @Override
        public boolean contains(Object object) {
        return ReportQueryResult.this.containsKey(object);
        }
        @Override
        public boolean remove(Object object) {
            return ReportQueryResult.this.remove(object) != null;
        }
    }

    /**
     * ADVANCED:
     * Set the value for given item name.
     */
    @Override
    public Object put(Object name, Object value) {
        int index = getNames().indexOf(name);
        if (index == -1) {
            getNames().add((String)name);
            getResults().add(value);
            return null;
        }

        Object oldValue = getResults().get(index);
        getResults().set(index, value);
        return oldValue;
    }

    /**
     * PUBLIC:
     * Add all of the elements.
     */
    @Override
    public void putAll(Map map) {
        Iterator entriesIterator = map.entrySet().iterator();
        while (entriesIterator.hasNext()) {
            Map.Entry entry = (Map.Entry)entriesIterator.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * PUBLIC:
     * If the PKs were retrieved with the attributes then this method can be used to read the real object from the database.
     */
    public Object readObject(Class<?> javaClass, Session session) {
        if (getId() == null) {
            throw QueryException.reportQueryResultWithoutPKs(this);
        }

        ReadObjectQuery query = new ReadObjectQuery(javaClass);
        query.setSelectionId(getId());

        return session.executeQuery(query);
    }

    /**
     * INTERNAL:
     * Remove the name key and value from the result.
     */
    @Override
    public Object remove(Object name) {
        int index = getNames().indexOf(name);
        if (index >= 0) {
            getNames().remove(index);
            Object value = getResults().get(index);
            getResults().remove(index);
            return value;
        }
        return null;
    }

    protected void setNames(List<String> names) {
        this.names = names;
    }

    /**
     * INTERNAL:
     * Set the Id for the result row's object.
     */
    protected void setId(Object primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * INTERNAL:
     * Set the results.
     */
    public void setResults(List<Object> results) {
        this.results = results;
    }

    /**
     * PUBLIC:
     * Return the number of name/value pairs in the result.
     */
    @Override
    public int size() {
        return getNames().size();
    }

    /**
     * INTERNAL:
     * Converts the ReportQueryResult to a simple array of values.
     */
    public Object[] toArray(){
       List<Object> list = getResults();
       return (list == null) ? null : list.toArray();
    }

    /**
     * INTERNAL:
     * Converts the ReportQueryResult to a simple list of values.
     */
    public List toList(){
        return this.getResults();
    }

    @Override
    public String toString() {
        java.io.StringWriter writer = new java.io.StringWriter();
        writer.write("ReportQueryResult(");
        for (int index = 0; index < getResults().size(); index++) {
            Object resultObj = getResults().get(index);
            writer.write(String.valueOf(resultObj));
            writer.write(" <"
                         + (resultObj == null ? "null" : resultObj.getClass().getName())
                         + ">");
            if (index < (getResults().size() - 1)) {
                writer.write(", ");
            }
        }
        writer.write(")");
        return writer.toString();
    }

    /**
     * PUBLIC:
     * Returns an collection of the values.
     */
    @Override
    public Collection values() {
        return getResults();
    }
}
