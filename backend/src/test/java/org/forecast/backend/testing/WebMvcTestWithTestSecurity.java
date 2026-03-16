package org.forecast.backend.testing;

import org.forecast.backend.config.GlobalExceptionHandler;
import org.forecast.backend.config.TestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation to apply common test config for WebMvc tests that need the test security config.
 * Usage: keep using @WebMvcTest(controllers = ...) and add @WebMvcTestWithTestSecurity
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({GlobalExceptionHandler.class, TestConfig.class})
@TestPropertySource(properties = "test.security.use-test-config=true")
public @interface WebMvcTestWithTestSecurity {

}

