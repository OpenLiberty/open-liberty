/*
 * Copyright (c) 2006, 2024 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024 Contributors to the Eclipse Foundation. All rights reserved.
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
//     Oracle - initial API and implementation
//
package org.eclipse.persistence.jpa.jpql.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.persistence.jpa.jpql.WordParser;
import org.eclipse.persistence.jpa.jpql.utility.iterable.ListIterable;
import org.eclipse.persistence.jpa.jpql.utility.iterable.SnapshotCloneListIterable;

/**
 * An identification variable followed by the navigation operator (.) and a state field or
 * association field is a path expression. The type of the path expression is the type computed as
 * the result of navigation; that is, the type of the state field or association field to which the
 * expression navigates.
 *
 * @see CollectionValuedPathExpression
 * @see IdentificationVariable
 *
 * @version 2.5
 * @since 2.3
 * @author Pascal Filion
 */
public abstract class AbstractPathExpression extends AbstractExpression {

    /**
     * Determines whether the path ends with a dot or not.
     */
    private boolean endsWithDot;

    /**
     * The identification variable that starts the path expression, which can be a sample {@link
     * IdentificationVariable identification variable}, an {@link EntryExpression entry expression},
     * a {@link ValueExpression value expression} or a {@link KeyExpression key expression}.
     */
    private AbstractExpression identificationVariable;

    /**
     * The state field path in a ordered list of string segments.
     */
    private List<String> paths;

    /**
     * The cached number of segments representing the path expression.
     */
    private int pathSize;

    /**
     * Determines whether the path starts with a dot or not.
     */
    private boolean startsWithDot;

    /**
     * Creates a new <code>AbstractPathExpression</code>.
     *
     * @param parent The parent of this expression
     * @param identificationVariable The identification variable that was already parsed, which means
     * the beginning of the parsing should start with a dot
     */
    @SuppressWarnings("this-escape")
    protected AbstractPathExpression(AbstractExpression parent, AbstractExpression identificationVariable) {
        super(parent);
        this.pathSize = -1;
        this.identificationVariable = identificationVariable;
        this.identificationVariable.setParent(this);
    }

    /**
     * Creates a new <code>AbstractPathExpression</code>.
     *
     * @param parent The parent of this expression
     * @param identificationVariable The identification variable that was already parsed, which means
     * the beginning of the parsing should start with a dot
     * @param paths The path expression that is following the identification variable
     */
    @SuppressWarnings("this-escape")
    public AbstractPathExpression(AbstractExpression parent,
                                  AbstractExpression identificationVariable,
                                  String paths) {

        super(parent, paths);
        this.pathSize = -1;
        this.identificationVariable = identificationVariable;
        this.identificationVariable.setParent(this);
    }

    /**
     * Creates a new <code>AbstractPathExpression</code>.
     *
     * @param parent The parent of this expression
     * @param paths The path expression
     */
    protected AbstractPathExpression(AbstractExpression parent, String paths) {
        super(parent, paths);
        this.pathSize = -1;
    }

    @Override
    public void acceptChildren(ExpressionVisitor visitor) {
        getIdentificationVariable().accept(visitor);
    }

    @Override
    protected void addChildrenTo(Collection<Expression> children) {
        checkPaths();
        children.add(identificationVariable);
    }

    @Override
    protected final void addOrderedChildrenTo(List<Expression> children) {

        checkPaths();

        if (!hasVirtualIdentificationVariable()) {
            children.add(identificationVariable);
        }

        children.add(buildStringExpression(getText()));
    }

    private void checkPaths() {

        // Nothing to do
        if (paths != null) {
            return;
        }

        paths = new ArrayList<>();
        String text = getText();
        char character = '\0';
        StringBuilder singlePath = new StringBuilder();

        // Extract each path from the text
        for (int index = 0, count = text.length(); index < count; index++) {

            character = text.charAt(index);

            // Make sure the identification variable is handled
            // correctly if it was passed during instantiation
            if (index == 0) {

                // No identification variable was passed during instantiation
                if (identificationVariable == null) {

                    // The path starts with '.'
                    startsWithDot = (character == DOT);

                    // Start appending to the current single path
                    if (!startsWithDot) {
                        singlePath.append(character);
                    }
                }
                // The identification variable was passed during instantiation,
                // add its parsed text as a path, it's assume the character is a dot
                else if (!identificationVariable.isNull() &&
                         !identificationVariable.isVirtual()) {

                    paths.add(identificationVariable.toParsedText());
                }
                // Start appending to the current single path
                else {
                    singlePath.append(character);
                }
            }
            else {

                // Append the character and continue
                if (character != DOT) {
                    singlePath.append(character);
                }
                // Scanning a '.'
                else {

                    // Store the current single path
                    paths.add(singlePath.toString());

                    // Clean the buffer
                    singlePath.setLength(0);
                }
            }
        }

        // Check if the last character is a '.'
        endsWithDot = (character == DOT);

        // Make sure the last path is added to the list
        if (!singlePath.isEmpty()) {
            paths.add(singlePath.toString());
        }

        // Cache the size
        pathSize = paths.size();

        // The identification variable can never be null
        if (identificationVariable == null) {
            if (startsWithDot || !endsWithDot && (pathSize == 1)) {
                identificationVariable = buildNullExpression();
            }
            else {
                identificationVariable = new IdentificationVariable(this, paths.get(0), false);
            }
        }
    }

    /**
     * Determines whether the path ends with a dot or not.
     *
     * @return <code>true</code> if the path ends with a dot; <code>false</code> otherwise
     */
    public final boolean endsWithDot() {
        checkPaths();
        return endsWithDot;
    }

    @Override
    public final JPQLQueryBNF findQueryBNF(Expression expression) {

        if ((identificationVariable != null) && identificationVariable.isAncestor(expression)) {
            return getQueryBNF(GeneralIdentificationVariableBNF.ID);
        }

        return super.findQueryBNF(expression);
    }

    /**
     * Returns the identification variable that starts the path expression, which can be a sample
     * identification variable, a map value, map key or map entry expression.
     *
     * @return The root of the path expression
     */
    public final Expression getIdentificationVariable() {
        checkPaths();
        return identificationVariable;
    }

    /**
     * Returns the specified segment of the state field path.
     *
     * @param index The 0-based segment index
     * @return The specified segment
     */
    public final String getPath(int index) {
        checkPaths();
        return paths.get(index);
    }

    /**
     * Determines whether the identification variable was parsed.
     *
     * @return <code>true</code> the identification variable was parsed; <code>false</code> otherwise
     */
    public final boolean hasIdentificationVariable() {
        checkPaths();
        return !identificationVariable.isNull() &&
               !identificationVariable.isVirtual();
    }

    /**
     * Determines whether the path's identification variable is virtual or not, meaning it's not part
     * of the query but is required for proper navigability.
     *
     * @return <code>true</code> if this identification variable was virtually created to fully
     * qualify path expression; <code>false</code> if it was parsed
     */
    public final boolean hasVirtualIdentificationVariable() {
        checkPaths();
        return identificationVariable.isVirtual();
    }

    /**
     * Determines whether the path's identification variable is virtual and not used in the query with the {@code this} keyword.
     *
     * @return <code>true</code> if this identification variable was virtually created and is not explicitly used in this path expression; <code>false</code> otherwise (is not virtual or is virtual and referenced with the {@code this} keyword)
     */
    public final boolean hasImplicitIdentificationVariable() {
        checkPaths();
        return identificationVariable.isVirtual() && !paths.get(0).equals(Expression.THIS);
    }

    @Override
    protected final void parse(WordParser wordParser, boolean tolerant) {
        wordParser.moveForward(getText());
    }

    /**
     * Returns the segments in the state field path in order.
     *
     * @return An <code>Iterator</code> over the segments of the state field path
     */
    public final ListIterable<String> paths() {
        checkPaths();
        return new SnapshotCloneListIterable<>(paths);
    }

    /**
     * Returns the number of segments in the state field path.
     *
     * @return The number of segments
     */
    public final int pathSize() {
        checkPaths();
        return pathSize;
    }

    /**
     * Sets a virtual identification variable because the abstract schema name was parsed without
     * one. This is valid in an <b>UPDATE</b> and <b>DELETE</b> queries.
     *
     * @param variableName The identification variable that was generated to identify the "root" object
     */
    protected final void setVirtualIdentificationVariable(String variableName) {

        identificationVariable = new IdentificationVariable(this, variableName, true);

        rebuildActualText();
        rebuildParsedText();
    }

    /**
     * Determines whether the path starts with a dot or not.
     *
     * @return <code>true</code> if the path starts with a dot; <code>false</code> otherwise
     */
    public final boolean startsWithDot() {
        return startsWithDot;
    }

    /**
     * Returns a string representation from the given range.
     *
     * @param startIndex The beginning of the range to create the string representation
     * @param stopIndex When to stop creating the string representation, which is exclusive
     * @return The string representation of this path expression contained in the given range
     * @since 2.4
     */
    public String toParsedText(int startIndex, int stopIndex) {

        checkPaths();
        StringBuilder writer = new StringBuilder();

        for (int index = startIndex; index < stopIndex; index++) {
            writer.append(paths.get(index));

            if (index < stopIndex - 1) {
                writer.append(DOT);
            }
        }

        return writer.toString();
    }

    @Override
    protected final void toParsedText(StringBuilder writer, boolean actual) {

        checkPaths();

        if (startsWithDot) {
            writer.append(DOT);
        }

        for (int index = 0, count = pathSize(); index < count; index++) {

            if (index > 0) {
                writer.append(DOT);
            }

            // Make sure to use the identification variable for proper formatting
            if ((index == 0) && hasIdentificationVariable()) {
                identificationVariable.toParsedText(writer, actual);
            }
            // Append a single path
            else {
                writer.append(paths.get(index));
            }
        }

        if (endsWithDot) {
            writer.append(DOT);
        }
    }
}
