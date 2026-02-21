package com.isaac.approvalworkflowengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
class ApprovalWorkflowEngineApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("application context starts with default profile configuration")
    void contextLoads() {
        assertThat(environment.getActiveProfiles()).isEmpty();
    }
}
