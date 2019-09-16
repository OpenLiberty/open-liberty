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

import java.util.List;

public class AllWidgetData {

    private List<Widget> allWidgets;
    private Widget widgetByName;

    public List<Widget> getAllWidgets() {
        return allWidgets;
    }

    public void setAllWidgets(List<Widget> allWidgets) {
        this.allWidgets = allWidgets;
    }
    
    public Widget getWidgetByName() {
        return widgetByName;
    }
    
    public void setWidgetByName(Widget widget) {
        widgetByName = widget;
    }
    
    public Widget getCreateWidget() {
        return widgetByName;
    }
    
    public void setCreateWidget(Widget widgetByName) {
        this.widgetByName = widgetByName;
    }

    public Widget getCreateWidgetByString() {
        return widgetByName;
    }
    
    public void setCreateWidgetByString(Widget widgetByName) {
        this.widgetByName = widgetByName;
    }
}
