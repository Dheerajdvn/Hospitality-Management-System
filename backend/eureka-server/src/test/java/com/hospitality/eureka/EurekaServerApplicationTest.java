package com.hospitality.eureka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EurekaServerApplicationTest {

    @Test
    @DisplayName("EurekaServerApplication class loads successfully")
    void contextLoads() {
        EurekaServerApplication app = new EurekaServerApplication();
        assertNotNull(app);
    }
}
