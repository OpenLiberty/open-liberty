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
package mpGraphQL10.basicMutation;

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
            List<Widget> widgets = data.getAllWidgets();
            if (widgets != null) {
                for (Widget w : data.getAllWidgets()) {
                    sb.append(" ").append(w);
                }
            }
            Widget createdWidget = data.getCreateWidget();
            if (createdWidget != null) {
                sb.append(" createdWidget=" + createdWidget);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
