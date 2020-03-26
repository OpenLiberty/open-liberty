/**
 * 
 */
package mpGraphQL10.defaultvalue;

public class VariablesAsString implements Variables {

    public static VariablesAsString newVars(String widgetString) {
        return new VariablesAsString(widgetString);
    }

    private String widgetString;

    private VariablesAsString(String widgetString) {
        this.widgetString = widgetString;
    }

    public String getWidgetString() {
        return widgetString;
    }

    public void setWidgetString(String widgetString) {
        this.widgetString = widgetString;
    }
}
