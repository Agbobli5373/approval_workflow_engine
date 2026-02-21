package com.isaac.approvalworkflowengine;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void verifiesModuleStructureWithoutCycles() {
        ApplicationModules.of(ApprovalWorkflowEngineApplication.class).verify();
    }
}
