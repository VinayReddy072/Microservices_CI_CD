package com.emergencylending.loan.controller;

import com.emergencylending.loan.entity.LoanRequest;
import com.emergencylending.loan.entity.LoanStatus;
import com.emergencylending.loan.service.LoanRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice test for {@link LoanRequestController}.
 *
 * <p>Uses {@code @WebMvcTest} which loads only the MVC layer (controller + advice)
 * and mocks the service tier. Exercises HTTP contract: correct status codes,
 * response body structure, and validation error handling.
 */
@WebMvcTest(LoanRequestController.class)
@TestPropertySource(properties = {
    "spring.config.import=",
    "spring.application.name=loan-service-test",
    "eureka.client.enabled=false",
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false",
    "spring.cloud.config.enabled=false",
    "management.tracing.enabled=false"
})
class LoanRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanRequestService service;

    @Test
    @DisplayName("POST /loans: valid body returns 201 Created with persisted entity")
    void createLoan_validRequest_returns201WithPendingStatus() throws Exception {
        LoanRequest saved = LoanRequest.builder()
                .id(1L)
                .equipmentItemId(5L)
                .borrowerName("Jane Borrower")
                .borrowerContact("jane@example.com")
                .status(LoanStatus.PENDING)
                .build();

        when(service.create(any())).thenReturn(saved);

        String body = objectMapper.writeValueAsString(Map.of(
                "equipmentItemId", 5,
                "borrowerName", "Jane Borrower",
                "borrowerContact", "jane@example.com"
        ));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.borrowerName").value("Jane Borrower"));
    }

    @Test
    @DisplayName("POST /loans: missing required fields return 400 with per-field validation errors")
    void createLoan_missingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.equipmentItemId").exists())
                .andExpect(jsonPath("$.borrowerName").exists())
                .andExpect(jsonPath("$.borrowerContact").exists());
    }
}
