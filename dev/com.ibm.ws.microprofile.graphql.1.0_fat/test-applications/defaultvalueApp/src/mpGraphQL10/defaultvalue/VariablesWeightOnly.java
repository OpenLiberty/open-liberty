/**
 * 
 */
package mpGraphQL10.defaultvalue;

public class VariablesWeightOnly implements Variables {

    public static VariablesWeightOnly newVars(double weight) {
        return new VariablesWeightOnly(new WidgetWeightOnly(weight));
    }

    private WidgetWeightOnly widget;

    private VariablesWeightOnly(WidgetWeightOnly widget) {
        this.widget = widget;
    }

    public WidgetWeightOnly getWidget() {
        return widget;
    }

    public void setWidget(WidgetWeightOnly widget) {
        this.widget = widget;
    }
    
    public static class WidgetWeightOnly {
        double weight;
        
        public WidgetWeightOnly(double weight) {
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
        
    }
}
