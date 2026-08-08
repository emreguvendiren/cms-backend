package com.cmsBackend.ws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WsApplicationProfileTests {
    @Test
    void ideDevtoolsLaunchWithoutAProfileUsesLocalProfile() {
        assertThat(WsApplication.shouldActivateLocalProfile(new String[0], true, null)).isTrue();
    }

    @Test
    void packagedApplicationNeverGetsImplicitLocalProfile() {
        assertThat(WsApplication.shouldActivateLocalProfile(new String[0], false, null)).isFalse();
    }

    @Test
    void explicitArgumentProfileIsNeverOverridden() {
        assertThat(WsApplication.shouldActivateLocalProfile(
                        new String[] {"--spring.profiles.active=production"}, true, null))
                .isFalse();
    }

    @Test
    void configuredEnvironmentProfileIsNeverOverridden() {
        assertThat(WsApplication.shouldActivateLocalProfile(new String[0], true, "production")).isFalse();
    }
}
