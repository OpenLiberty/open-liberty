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
package mpGraphQL10.basicQuery;

import java.util.List;

public class WidgetQueryResponse {

    private AllWidgetData data;
    private List<Error> errors;

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public AllWidgetData getData() {
        return data;
    }

    public void setData(AllWidgetData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WidgetQueryResponse[").append(System.lineSeparator()).append("  data");
        if (data == null || data.getAllWidgets() == null) {
            sb.append("null");
        } else {
            for (Widget w : data.getAllWidgets()) {
                sb.append(" ").append(w);
            }
        }
        sb.append("]");
        if (errors != null) {
            sb.append(System.lineSeparator()).append("  errors [");
            for (Error e : errors) {
                sb.append("    {").append(System.lineSeparator());
                sb.append("      message : ").append(e.getMessage()).append(System.lineSeparator());
                sb.append("      path : ").append(e.getPath()).append(System.lineSeparator());
                sb.append("      extensions : ").append(e.getExtensions()).append(System.lineSeparator());
                sb.append("    }").append(System.lineSeparator());
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
