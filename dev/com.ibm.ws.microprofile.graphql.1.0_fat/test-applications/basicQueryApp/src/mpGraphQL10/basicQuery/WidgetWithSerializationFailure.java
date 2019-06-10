/**
 * 
 */
package mpGraphQL10.basicQuery;

public class WidgetWithSerializationFailure extends Widget {

    public WidgetWithSerializationFailure(String name, int quantity, double weight) {
        super(name, quantity, weight);
    }
    
    @Override
    public int getQuantity() {
        throw new RuntimeException("Intentional failure");
    }

    @Override
    public void setQuantity(int quantity) {
        super.setQuantity(quantity);
    }
}
