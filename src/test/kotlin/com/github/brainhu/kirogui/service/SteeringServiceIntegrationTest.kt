package com.github.brainhu.kirogui.service

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Integration test verifying that [SteeringService] can be retrieved
 * from the IntelliJ service manager as a project-level service.
 *
 * Requirements: 6.1, 6.2, 6.4
 */
class SteeringServiceIntegrationTest : BasePlatformTestCase() {

    fun `test SteeringService can be retrieved from project`() {
        val service = project.getService(SteeringServiceImpl::class.java)
        assertNotNull("SteeringService should be available from project", service)
        assertTrue("Service should be SteeringServiceImpl", service is SteeringServiceImpl)
    }

    fun `test SteeringService is singleton per project`() {
        val service1 = project.getService(SteeringServiceImpl::class.java)
        val service2 = project.getService(SteeringServiceImpl::class.java)
        assertSame("SteeringService should be singleton per project", service1, service2)
    }
}
