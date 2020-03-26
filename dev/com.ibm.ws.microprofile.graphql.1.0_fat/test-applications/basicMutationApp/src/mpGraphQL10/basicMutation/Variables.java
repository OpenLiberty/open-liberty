/**
 * 
 */
package mpGraphQL10.basicMutation;

/**
 *"{" + System.lineSeparator() +
                                      "  \"widget\": {" + System.lineSeparator() +
                                      "    \"name\": \"Earbuds\"," + System.lineSeparator() +
                                      "    \"quantity\": 20," + System.lineSeparator() +
                                      "    \"weight\": 1.2" + System.lineSeparator() +
                                      "  }" + System.lineSeparator() +
                                      "}"
 */
public class Variables {

    public static Variables newVars(String name, int quantity, double weight) {
        WidgetInput w = new WidgetInput(name, quantity, weight);
        return new Variables(w);
    }

    private WidgetInput widget;

    private Variables(WidgetInput widget) {
        this.widget = widget;
    }

    public WidgetInput getWidget() {
        return widget;
    }

    public void setWidget(WidgetInput widget) {
        this.widget = widget;
    }

    public static class WidgetInput {
        String name;
        int quantity;
        double weight;
        
        WidgetInput(String name, int quantity, double weight) {
            this.name = name;
            this.quantity = quantity;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public int getQuantity() {
            return quantity;
        }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        public double getWeight() {
            return weight;
        }
        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
}
