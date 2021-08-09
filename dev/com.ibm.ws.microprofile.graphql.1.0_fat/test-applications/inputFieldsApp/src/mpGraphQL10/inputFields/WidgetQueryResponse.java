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
package mpGraphQL10.inputFields;

import java.util.List;

public class WidgetQueryResponse {

    private AllWidgetData data;

    public AllWidgetData getData() {
        return data;
    }

    public void setData(AllWidgetData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WidgetQueryResponse[");
        if (data == null) {
            sb.append("null");
        } else {
            List<WidgetClientObject> widgets = data.getAllWidgets();
            if (widgets == null) {
                if (data.getCreateWidget() != null) {
                    sb.append("created widget:" + data.getCreateWidget());
                } else {
                    sb.append("nullList");
                }
            } else {
                for (WidgetClientObject w : widgets) {
                    sb.append(" ").append(w);
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
