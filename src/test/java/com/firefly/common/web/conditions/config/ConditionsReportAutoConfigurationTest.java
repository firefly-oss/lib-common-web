/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.web.conditions.config;

import com.firefly.common.web.conditions.listener.ConditionsReportListener;
import com.firefly.common.web.conditions.service.ConditionsReportService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for ConditionsReportAutoConfiguration.
 */
class ConditionsReportAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConditionsReportPropertiesConfiguration.class,
                    ConditionsReportAutoConfiguration.class
            ));

    @Test
    void shouldNotLoadBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("firefly.conditions-report.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ConditionsReportService.class);
                    assertThat(context).doesNotHaveBean(ConditionsReportListener.class);
                });
    }

    @Test
    void shouldNotLoadBeansByDefault() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ConditionsReportService.class);
                    assertThat(context).doesNotHaveBean(ConditionsReportListener.class);
                });
    }

    @Test
    void shouldLoadBeansWhenEnabled() {
        contextRunner
                .withPropertyValues("firefly.conditions-report.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ConditionsReportService.class);
                    assertThat(context).hasSingleBean(ConditionsReportListener.class);
                    assertThat(context).hasSingleBean(ConditionsReportProperties.class);
                });
    }

    @Test
    void shouldRespectConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "firefly.conditions-report.enabled=true",
                        "firefly.conditions-report.show-negative=false",
                        "firefly.conditions-report.show-details=false",
                        "firefly.conditions-report.summary-only=true",
                        "firefly.conditions-report.use-colors=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ConditionsReportProperties.class);
                    
                    ConditionsReportProperties properties = context.getBean(ConditionsReportProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.isShowNegative()).isFalse();
                    assertThat(properties.isShowDetails()).isFalse();
                    assertThat(properties.isSummaryOnly()).isTrue();
                    assertThat(properties.isUseColors()).isFalse();
                });
    }

    @Test
    void shouldUseDefaultValues() {
        contextRunner
                .withPropertyValues("firefly.conditions-report.enabled=true")
                .run(context -> {
                    ConditionsReportProperties properties = context.getBean(ConditionsReportProperties.class);
                    
                    // Verify default values
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.isShowNegative()).isTrue();
                    assertThat(properties.isShowDetails()).isTrue();
                    assertThat(properties.isSummaryOnly()).isFalse();
                    assertThat(properties.isUseColors()).isTrue();
                });
    }
}

