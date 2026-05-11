package com.example.cybersource.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber test runner for executing Excel-driven BDD scenarios.
 *
 * <p>Plugins configured:
 * <ul>
 *   <li><b>pretty</b>     – human-readable console output</li>
 *   <li><b>html</b>       – HTML report at {@code target/cucumber-reports/cucumber.html}</li>
 *   <li><b>json</b>       – JSON report consumed by Masterthought at {@code target/cucumber-reports/cucumber.json}</li>
 *   <li><b>timeline</b>   – parallel execution timeline at {@code target/cucumber-reports/timeline}</li>
 * </ul>
 *
 * <p>The Masterthought rich HTML report is generated in the Maven {@code verify} phase via
 * the {@code cucumber-reporting} plugin configured in {@code pom.xml}.
 * Run {@code mvn verify} to produce the full coverage report at
 * {@code target/cucumber-html-reports/overview-features.html}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.cybersource.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, "
                + "html:target/cucumber-reports/cucumber.html, "
                + "json:target/cucumber-reports/cucumber.json, "
                + "timeline:target/cucumber-reports/timeline")
public class CucumberTestRunner {
}

