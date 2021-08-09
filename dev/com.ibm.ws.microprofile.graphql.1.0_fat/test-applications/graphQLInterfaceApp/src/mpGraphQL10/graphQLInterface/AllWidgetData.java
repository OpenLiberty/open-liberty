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
package mpGraphQL10.graphQLInterface;

import java.util.List;

public class AllWidgetData {

    private List<Widget> allWidgets;
    private Widget createdWidget;

    public List<Widget> getAllWidgets() {
        return allWidgets;
    }

    public void setAllWidgets(List<Widget> allWidgets) {
        this.allWidgets = allWidgets;
    }
    
    public Widget getCreateWidget() {
        return createdWidget;
    }
    
    public void setCreateWidget(Widget widget) {
        createdWidget = widget;
    }

    public Widget getCreateWidgetByHand() {
        return createdWidget;
    }
    
    public void setCreateWidgetByHand(Widget widget) {
        createdWidget = widget;
    }
}
