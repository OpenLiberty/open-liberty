/*
 * Copyright (c) 1998, 2025 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022 IBM Corporation. All rights reserved.
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
//     05/24/2011-2.3 Guy Pelletier
//       - 345962: Join fetch query when using tenant discriminator column fails.
package org.eclipse.persistence.internal.expressions;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.queries.DatabaseQuery;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Used for parameterized expressions, such as expression defined in mapping queries.
 */
public class ParameterExpression extends BaseExpression {

    /** The parameter field or name. */
    protected DatabaseField field;

    /** The opposite side of the relation, this is used for conversion of the parameter using the others mapping. */
    protected Expression localBase;

    protected boolean isProperty = false;

    /**
     *  'True' indicates this expression can bind parameters
     *  'False' indicates this expression cannot bind parameters
     *  Defaults to 'null' to indicate specific no preference
     */
    protected Boolean canBind = null;

    /** The inferred type of the parameter.
     * Please note that the type might not be always initialized to correct value.
     * It might be null if not initialized correctly.
     */
    Object type;

    public ParameterExpression() {
        super();
    }

    public ParameterExpression(String fieldName) {
        this(new DatabaseField(fieldName));
    }

    public ParameterExpression(DatabaseField field) {
        super();
        this.field = field;
    }

    // For bug 3107049 ParameterExpression will now be built with a
    // default localBase, same as with ConstantExpression.
    public ParameterExpression(String fieldName, Expression localbaseExpression, Object type) {
        this(new DatabaseField(fieldName), localbaseExpression);
        this.type = type;
    }

    public ParameterExpression(DatabaseField field, Expression localbaseExpression) {
        super();
        this.field = field;
        localBase = localbaseExpression;
    }

    /**
     * INTERNAL:
     * Return if the expression is equal to the other.
     * This is used to allow dynamic expression's SQL to be cached.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!super.equals(object)) {
            return false;
        }
        ParameterExpression expression = (ParameterExpression) object;
        return ((getField() == expression.getField()) || ((getField() != null) && getField().equals(expression.getField())));
    }

    /**
     * INTERNAL:
     * Compute a consistent hash-code for the expression.
     * This is used to allow dynamic expression's SQL to be cached.
     */
    @Override
    public int computeHashCode() {
        int hashCode = super.computeHashCode();
        if (getField() != null) {
            hashCode = hashCode + getField().hashCode();
        }
        return hashCode;
    }

    /**
     * Return description.
     * Used for toString.
     */
    public String basicDescription() {
        return String.valueOf(getField());
    }

    /**
     * INTERNAL:
     * Used for debug printing.
     */
    @Override
    public String descriptionOfNodeType() {
        return "Parameter";
    }

    /**
     * This allows for nesting of parameterized expression.
     * This is used for parameterizing object comparisons.
     */
    @Override
    public Expression get(String attributeOrQueryKey) {
        ParameterExpression expression = new ParameterExpression(attributeOrQueryKey);
        expression.setBaseExpression(this);

        return expression;
    }

    /**
     * Return the expression builder which is the ultimate base of this expression, or
     * null if there isn't one (shouldn't happen if we start from a root)
     */
    @Override
    public ExpressionBuilder getBuilder() {
        if (localBase == null) {
            //Bug#5097278 Need to return the builder from the base expression if nested.
            if (getBaseExpression() != null) {
                return getBaseExpression().getBuilder();
            } else {
                return null;
            }
        }
        return localBase.getBuilder();
    }

    public DatabaseField getField() {
        return field;
    }

    /**
     * INTERNAL:
     * Used to set the internal field value.
     */
    public void setField(DatabaseField field) {
        this.field = field;
    }

    /**
     * This allows for nesting of parametrized expression.
     * This is used for parameterizing object comparisons.
     */
    @Override
    public Expression getField(DatabaseField field) {
        ParameterExpression expression = new ParameterExpression(field);
        expression.setBaseExpression(this);

        return expression;
    }

    /**
     * The opposite side of the relation, this is used for conversion of the parameter using the others mapping.
     */
    public Expression getLocalBase() {
        return localBase;
    }

    /**
     * The inferred type of this parameter.
     * Please note that the type might not be always initialized to correct value.
     * It might be null if not initialized correctly
     */
    public Object getType() { return type; }

    /**
     * The inferred type of this parameter.
     * Please note that the type might not be always initialized to correct value.
     * It might be null if not initialized correctly
     */
    public void setType(Object type) {
        this.type = type;
    }

    /**
     * Extract the value from the row.
     * This may require recursion if it is a nested parameter.
     */
    public Object getValue(AbstractRecord translationRow, AbstractSession session) {
        return getValue(translationRow, null, session);
    }

    /**
     * Extract the value from the row.
     * This may require recursion if it is a nested parameter.
     */
    public Object getValue(AbstractRecord translationRow, DatabaseQuery query, AbstractSession session) {
        if (this.field == null) {
            return null;
        }

        Object value = null;

        // Check for nested parameters.
        if (this.baseExpression != null) {
            value = ((ParameterExpression)this.baseExpression).getValue(translationRow, query, session);
            if (value == null) {
                return null;
            }

            ClassDescriptor descriptor = session.getDescriptor(value);
            //Bug4924639  Aggregate descriptors have to be acquired from their mapping as they are cloned and initialized by each mapping
            if (descriptor != null && descriptor.isAggregateDescriptor() && ((ParameterExpression)getBaseExpression()).getLocalBase().isObjectExpression()) {
                if (getLocalBase() != null && getLocalBase().isQueryKeyExpression()) {
                    descriptor = ((QueryKeyExpression)getLocalBase()).getMapping().getDescriptor();
                } else {
                    descriptor = ((ObjectExpression) ((ParameterExpression) getBaseExpression()).getLocalBase()).getDescriptor();
                }
            }

            if (descriptor == null) {
                // Bug 245268 validate parameter type against mapping
                validateParameterValueAgainstMapping(value, true);
            } else {
                // For bug 2990493 must unwrap for EJBQL "Select Person(p) where p = ?1"
                //if we had to unwrap it make sure we replace the argument with this value
                //incase it is needed again, say in conforming.
                //bug 3750793
                value = descriptor.getObjectBuilder().unwrapObject(value, session);

                // Bug 245268 must unwrap before validating parameter type
                validateParameterValueAgainstMapping(value, true);

                translationRow.put(((ParameterExpression)this.baseExpression).getField(), value);

                // The local parameter is either a field or attribute of the object.
                DatabaseMapping mapping = descriptor.getObjectBuilder().getMappingForField(this.field);
                if (mapping != null) {
                    value = mapping.valueFromObject(value, this.field, session);
                } else {
                    mapping = descriptor.getObjectBuilder().getMappingForAttributeName(this.field.getName());
                    if (mapping != null) {
                        //Nested attribute
                        if (value.getClass() != descriptor.getJavaClass() && this.getLocalBase().isQueryKeyExpression() && ((QueryKeyExpression)this.getLocalBase()).getBaseExpression().isQueryKeyExpression()) {
                            value = getParentNodeValue(descriptor.getJavaClass(), (QueryKeyExpression)this.getLocalBase(), value);
                        }
                        if (value != null) {
                            value = mapping.getRealAttributeValueFromObject(value, session);
                        }
                    } else {
                        DatabaseField queryKeyField = descriptor.getObjectBuilder().getFieldForQueryKeyName(this.field.getName());
                        if (queryKeyField != null) {
                            mapping = descriptor.getObjectBuilder().getMappingForField(this.field);
                            if (mapping != null) {
                                value = mapping.valueFromObject(value, this.field, session);
                            }
                        }
                    }
                }
            }
        } else {
            // Check for null translation row.
            if (translationRow == null) {
                value = AbstractRecord.noEntry;
            } else {
                value = translationRow.getIndicatingNoEntry(this.field);
            }

            // Throw an exception if the field is not mapped. Null may be
            // returned if it is a property so check for null and isProperty
            if ((value == AbstractRecord.noEntry) || ((value == null) && this.isProperty)) {
                if (this.isProperty) {
                    if (query != null) {
                        value = query.getSession().getProperty(this.field.getName());
                    } else {
                        value = session.getProperty(this.field.getName());
                    }

                    if (value == null) {
                        throw QueryException.missingContextPropertyForPropertyParameterExpression(query, this.field.getName());
                    }

                    return value;
                }
                // Also check the same field, but a different table for table per class inheritance.
                // TODO: JPA also allows for field to be renamed in subclasses, this needs to account for that (never has...).
                if (translationRow != null) {
                    value = translationRow.getIndicatingNoEntry(new DatabaseField(this.field.getName()));
                }
                if ((value == AbstractRecord.noEntry) || (value == null)) {
                    throw QueryException.parameterNameMismatch(this.field.getName());
                }
            }

            // validate parameter type against mapping
            // validate against the localbase (false), since there are no nested params
            validateParameterValueAgainstMapping(value, false);
        }

        // Convert the value to the correct type, i.e. object type mappings.
        if (this.localBase != null) {
            value = this.localBase.getFieldValue(value, session);
        }

        return value;
    }

    private boolean directionUp = true;

    private Object getParentNodeValue(Class<?> parentNodeClass, QueryKeyExpression queryKeyExpression, Object inputValue) {
        Object value = null;
        QueryKeyExpression baseExpression = queryKeyExpression;
        DatabaseMapping mapping = baseExpression.getMapping();
        if (directionUp) {
            baseExpression = (QueryKeyExpression) queryKeyExpression.getBaseExpression();
            mapping = baseExpression.getMapping();
        }
        if (mapping.getReferenceDescriptor().getJavaClass() == parentNodeClass &&
                mapping.getDescriptor().getJavaClass() == inputValue.getClass()) {
            value = mapping.getAttributeValueFromObject(inputValue);
        } else {
            Object tmpValue = inputValue;
            if (mapping.getDescriptor().getJavaClass() == inputValue.getClass()) {
                tmpValue = mapping.getAttributeValueFromObject(inputValue);
            }
            if (tmpValue != inputValue) {
                directionUp = false;
                return tmpValue;
            }
            tmpValue = getParentNodeValue(parentNodeClass, baseExpression, tmpValue);
            value = mapping.getAttributeValueFromObject(tmpValue);
        }
        return value;
    }

    @Override
    public boolean isParameterExpression() {
        return true;
    }

    /**
     * INTERNAL:
     */
    @Override
    public boolean isValueExpression() {
        return true;
    }

    /**
     * INTERNAL:
     * Return true if this parameter expression maps to a property.
     */
    public boolean isProperty() {
        return isProperty;
    }

    /**
     * INTERNAL:
     *  true indicates this expression can bind parameters
     *  false indicates this expression cannot bind parameters
     *  Defaults to null to indicate no specific preference
     */
    public Boolean canBind() {
        return canBind;
    }

    /**
     * INTERNAL:
     * Used for cloning.
     */
    @Override
    protected void postCopyIn(Map<Expression, Expression> alreadyDone) {
        super.postCopyIn(alreadyDone);
        if (getLocalBase() != null) {
            setLocalBase(getLocalBase().copiedVersionFrom(alreadyDone));
        }
    }

    /**
     * INTERNAL:
     * Print SQL onto the stream, using the ExpressionPrinter for context
     */
    @Override
    public void printSQL(ExpressionSQLPrinter printer) {
        if (printer.shouldPrintParameterValues()) {
            Object value = getValue(printer.getTranslationRow(), printer.getSession());
            if (value instanceof Collection<?> collection) {
                printer.printValuelist(collection, this.canBind);
            } else {
                if(getField() == null) {
                    printer.printPrimitive(value, this.canBind);
                } else {
                    printer.printParameter(this);
                }
            }
        } else {
            if (getField() != null) {
                printer.printParameter(this);
            }
        }
    }

    /**
     * INTERNAL:
     * Print java for project class generation
     */
    @Override
    public void printJava(ExpressionJavaPrinter printer) {
        ((DataExpression)getLocalBase()).getBaseExpression().printJava(printer);
        printer.printString(".getParameter(\"" + getField().getQualifiedName() + "\")");
    }

    /**
     * INTERNAL:
     * This expression is built on a different base than the one we want. Rebuild it and
     * return the root of the new tree
     */
    @Override
    public Expression rebuildOn(Expression newBase) {
        ParameterExpression result = (ParameterExpression)clone();
        result.setLocalBase(localBase.rebuildOn(newBase));
        return result;
    }

    /**
     * INTERNAL:
     * Search the tree for any expressions (like SubSelectExpressions) that have been
     * built using a builder that is not attached to the query.  This happens in case of an Exists
     * call using a new ExpressionBuilder().  This builder needs to be replaced with one from the query.
     */
    @Override
    public void resetPlaceHolderBuilder(ExpressionBuilder queryBuilder){
        return;
    }

    /**
     * INTERNAL:
     * Set to true if this parameter expression maps to a property value.
     */
    public void setIsProperty(boolean isProperty) {
        this.isProperty = isProperty;
    }

    /**
     * INTERNAL:
     * Set to true if this expression can bind parameters
     * Set to false if this expression cannot bind parameters
     * Set to null to indicate no specific preference
     */
    public void setCanBind(Boolean canBind) {
        this.canBind = canBind;
    }

    /**
     * The opposite side of the relation, this is used for conversion of the parameter using the others mapping.
     */
    @Override
    public void setLocalBase(Expression localBase) {
        this.localBase = localBase;
    }

    /**
     * INTERNAL:
     * Rebuild against the base, with the values of parameters supplied by the context
     * expression. This is used for transforming a standalone expression (e.g. the join criteria of a mapping)
     * into part of some larger expression. You normally would not call this directly, instead calling twist,
     * (see the comment there for more details).
     */
    @Override
    public Expression twistedForBaseAndContext(Expression newBase, Expression context, Expression oldBase) {
        if (isProperty()) {
            return context.getProperty(getField());
        } else if (newBase == oldBase) {
            return this;
        } else {
            return context.getField(getField());
        }
    }

    /**
     * INTERNAL
     * Validate the passed parameter against the local base mapping.
     * Throw a QueryException if the parameter is of an incorrect class for object comparison.
     * Added for Bug 245268
     */
    protected void validateParameterValueAgainstMapping(Object value, boolean useBaseExpression) {
        Expression queryKey = null;
        if (useBaseExpression) {
            // used to support validating against the base expression in the case of nesting
            ParameterExpression baseExpression = (ParameterExpression)getBaseExpression();
            queryKey = baseExpression.getLocalBase();
        } else {
            // used where we need to simply validate against the local base expression
            queryKey = this.getLocalBase();
        }

        if ((value != null) && !(value instanceof Collection) && (queryKey != null) && queryKey.isObjectExpression()) {
            DatabaseMapping mapping = ((ObjectExpression) queryKey).getMapping();
            if (mapping != null) {
                if (mapping.isCollectionMapping() && queryKey.isMapEntryExpression() && !((MapEntryExpression)queryKey).shouldReturnMapEntry()){
                    // this is a map key expression, operate on the key
                    ContainerPolicy cp = mapping.getContainerPolicy();
                    Object keyType = cp.getKeyType();
                    Class<?> keyTypeClass = keyType instanceof Class<?> c ? c: ((ClassDescriptor)keyType).getJavaClass();
                    if (!keyTypeClass.isInstance(value)){
                        throw QueryException.incorrectClassForObjectComparison(baseExpression, value, mapping);
                    }
                } else if (mapping.isDirectCollectionMapping()) {
                    // Do not validate direct collection, as type may be convertable.
                } else if (mapping.isForeignReferenceMapping() && !mapping.getReferenceDescriptor().getJavaClass().isInstance(value)) {
                    throw QueryException.incorrectClassForObjectComparison(baseExpression, value, mapping);
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Return the value for in memory comparison.
     * This is only valid for valueable expressions.
     */
    @Override
    public Object valueFromObject(Object object, AbstractSession session, AbstractRecord translationRow, int valueHolderPolicy, boolean isObjectUnregistered) {
        // Run ourselves through the translation row to find the desired value
        if (getField() != null) {
            return getValue(translationRow, session);
        }

        throw QueryException.cannotConformExpression();
    }

    /**
     * INTERNAL:
     * Used to print a debug form of the expression tree.
     */
    @Override
    public void writeDescriptionOn(BufferedWriter writer) throws IOException {
        writer.write(basicDescription());
    }

    /**
     * INTERNAL:
     * Append the parameter into the printer.
     * "Normal" ReadQuery never has ParameterExpression in it's select clause hence for a "normal" ReadQuery this method is never called.
     * The reason this method was added is that UpdateAllQuery (in case temporary storage is required)
     * creates a "helper" ReportQuery with ReportItem corresponding to each update expression - and update expression
     * may be a ParameterExpression. The call created by "helper" ReportQuery is never executed -
     * it's used during construction of insert call into temporary storage.
     */
    @Override
    public void writeFields(ExpressionSQLPrinter printer, List<DatabaseField> newFields, SQLSelectStatement statement) {
        /*
         * If the platform doesn't support binding for functions, then disable binding for the whole query
         *
         * DatabasePlatform classes should instead override DatasourcePlatform.initializePlatformOperators()
         *      @see ExpressionOperator.setIsBindingSupported(boolean isBindingSupported)
         * In this way, platforms can define their own supported binding behaviors for individual functions
         */
        if (printer.getPlatform().isDynamicSQLRequiredForFunctions()) {
            printer.getCall().setUsesBinding(false);
        }

        /*
         *  Allow the platform to indicate if they support parameter expressions in the SELECT clause
         *  as a whole, regardless if individual functions allow binding. We make that decision here
         *  before we continue parsing into generic API calls
         */
        if (!printer.getPlatform().allowBindingForSelectClause()) {
            setCanBind(false);
        }

        //print ", " before each selected field except the first one
        if (printer.isFirstElementPrinted()) {
            printer.printString(", ");
        } else {
            printer.setIsFirstElementPrinted(true);
        }

        // This field is a parameter value, so any name can be used.
        newFields.add(new DatabaseField("*"));
        printSQL(printer);
    }

    /**
     * Print the base for debuggin purposes.
     */
    @Override
    public void writeSubexpressionsTo(BufferedWriter writer, int indent) throws IOException {
        if (getBaseExpression() != null) {
            getBaseExpression().toString(writer, indent);
        }
    }
}