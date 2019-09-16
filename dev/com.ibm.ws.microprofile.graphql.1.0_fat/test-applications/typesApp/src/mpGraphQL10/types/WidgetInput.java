/**
 * 
 */
package mpGraphQL10.types;

import org.eclipse.microprofile.graphql.InputType;

@InputType(value="WidgetInput", description="A for-sale item object used for input.")
public class WidgetInput{

    private String name;
    private int quantity;
    private double weight = -1.0;

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
