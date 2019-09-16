/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.defaultvalue;

public class GraphQLOperation {

    private String query;
    private String variables;
    private String operationName;

    public String getQuery() {
        return query;
    }
    public void setQuery(String operation) {
        this.query = operation;
    }
    public String getVariables() {
        return variables;
    }
    public void setVariables(String variables) {
        this.variables = variables;
    }
    public String getOperationName() {
        return operationName;
    }
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + System.lineSeparator() + 
                        "  query=\"" + query + "\", " + System.lineSeparator() +
                        "  operationName=\"" + operationName + "\", " + System.lineSeparator() +
                        "  variables=\"" + variables + "\"]";
    }
}
