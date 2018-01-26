package bval.v20.customprovider;

import javax.validation.Configuration;
import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

public class MyCustomBvalProvider implements ValidationProvider<MyCustomBvalProviderConfig> {

    @Override
    public MyCustomBvalProviderConfig createSpecializedConfiguration(BootstrapState b) {
        return new MyCustomBvalProviderConfig();
    }

    @Override
    public Configuration<?> createGenericConfiguration(BootstrapState b) {
        return new MyCustomBvalProviderConfig();
    }

    @Override
    public ValidatorFactory buildValidatorFactory(ConfigurationState paramConfigurationState) {
        return new MyCustomValidatorFactory();
    }

}
