package wemove.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.type.LogicalType;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class StrictJsonConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJson() {
        return builder ->
                builder.postConfigurer(
                        mapper -> {
                            mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                            mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
                            mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
                            mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
                            mapper.coercionConfigFor(LogicalType.Integer)
                                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
                            mapper.coercionConfigFor(LogicalType.Boolean)
                                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                            mapper.coercionConfigFor(LogicalType.Textual)
                                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                        });
    }
}
