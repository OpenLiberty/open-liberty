package com.ibm.ws.kernel.boot;

import java.util.Collection;
import java.util.ResourceBundle;

public interface HelpActions {
    public Object toAction(String val);
    public boolean isHelpAction(Object action);
    public String allActions();
    public Collection<String> options(Object action);
    public Collection<?> getCategories();
    public Collection<?> geActionsForCategories(Object c);
    public ResourceBundle getResourceBundle();
}