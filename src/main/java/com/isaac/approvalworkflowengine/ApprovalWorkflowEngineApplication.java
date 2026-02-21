package com.isaac.approvalworkflowengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApprovalWorkflowEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalWorkflowEngineApplication.class, args);
    }

}
