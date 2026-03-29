// BaseWebMvcTest.java
package com.worldcup.hotelbooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity, we use @WithMockUser
public abstract class BaseWebMvcTest {
    @Autowired
    protected ObjectMapper objectMapper;
}