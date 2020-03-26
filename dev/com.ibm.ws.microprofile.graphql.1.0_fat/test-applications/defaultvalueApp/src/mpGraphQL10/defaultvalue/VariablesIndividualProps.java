/**
 * 
 */
package mpGraphQL10.defaultvalue;

public class VariablesIndividualProps implements Variables {

    public static VariablesIndividualProps newVars(String name, int quantity, double weight, double length, double height, double depth) {
        WidgetInput w = new WidgetInput(name, quantity, weight, length, height, depth);
        return new VariablesIndividualProps(w);
    }

    private WidgetInput widget;

    private VariablesIndividualProps(WidgetInput widget) {
        this.widget = widget;
    }

    public WidgetInput getWidget() {
        return widget;
    }

    public void setWidget(WidgetInput widget) {
        this.widget = widget;
    }
}
