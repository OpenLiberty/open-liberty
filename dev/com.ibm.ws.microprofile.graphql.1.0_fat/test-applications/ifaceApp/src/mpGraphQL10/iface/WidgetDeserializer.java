/**
 * 
 */
package mpGraphQL10.iface;

import java.lang.reflect.Type;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;


public class WidgetDeserializer implements JsonbDeserializer<Widget> {

    @Override
    public Widget deserialize(JsonParser parser, DeserializationContext ctx, Type type) {
        WidgetImpl impl = new WidgetImpl();
        while (parser.hasNext()) {
            Event event = parser.next();
            if (event == JsonParser.Event.KEY_NAME) {
                String key = parser.getString();
                parser.next();
                System.out.println("WidgetDeserializer " + key + "=" + parser.getString());
                if (key.equals("name")) {
                    impl.setName(parser.getString());
                } else if (key.equals("quantity")) {
                    impl.setQuantity(parser.getInt());
                } else if (key.equals("weight")) {
                    impl.setWeight(parser.getBigDecimal().doubleValue());
                }
            }
            
        }
        System.out.println("WidgetDeserializer returning " + impl);
        return impl;
    }

}
