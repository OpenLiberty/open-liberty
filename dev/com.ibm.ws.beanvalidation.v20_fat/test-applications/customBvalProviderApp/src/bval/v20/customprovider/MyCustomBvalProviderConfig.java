package bval.v20.customprovider;

import java.io.InputStream;

import javax.validation.BootstrapConfiguration;
import javax.validation.ClockProvider;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidatorFactory;
import javax.validation.valueextraction.ValueExtractor;

public class MyCustomBvalProviderConfig implements Configuration<MyCustomBvalProviderConfig> {

    @Override
    public MyCustomBvalProviderConfig addMapping(InputStream arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig addProperty(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig addValueExtractor(ValueExtractor<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValidatorFactory buildValidatorFactory() {
        return new MyCustomValidatorFactory();
    }

    @Override
    public MyCustomBvalProviderConfig clockProvider(ClockProvider arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig constraintValidatorFactory(ConstraintValidatorFactory arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BootstrapConfiguration getBootstrapConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClockProvider getDefaultClockProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageInterpolator getDefaultMessageInterpolator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParameterNameProvider getDefaultParameterNameProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TraversableResolver getDefaultTraversableResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig ignoreXmlConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig messageInterpolator(MessageInterpolator arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig parameterNameProvider(ParameterNameProvider arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyCustomBvalProviderConfig traversableResolver(TraversableResolver arg0) {
        throw new UnsupportedOperationException();
    }

}
