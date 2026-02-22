package com.isaac.approvalworkflowengine.platform;

import com.isaac.approvalworkflowengine.ApprovalWorkflowEngineApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void verifiesModuleStructureWithoutCycles() {
        ApplicationModules.of(ApprovalWorkflowEngineApplication.class).verify();
    }
}
