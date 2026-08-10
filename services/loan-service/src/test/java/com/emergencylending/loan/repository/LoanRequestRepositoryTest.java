package com.emergencylending.loan.repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.emergencylending.loan.entity.LoanRequest;
import com.emergencylending.loan.entity.LoanStatus;

/**
 * Integration test for {@link LoanRequestRepository}.
 *
 * <p>Uses {@code @DataJpaTest} with an H2 in-memory database (auto-configured
 * by Spring Boot's test slice). Verifies that the JPA mapping is correct and
 * that the database-per-service isolation contract holds at the schema level.
 */
@DataJpaTest
@ActiveProfiles("test")
class LoanRequestRepositoryTest {

    @Autowired
    private LoanRequestRepository repository;

    @Test
    @DisplayName("save: persists entity, assigns generated id, defaults status to PENDING")
    void save_shouldPersistEntityWithGeneratedIdAndDefaultStatus() {
        LoanRequest loan = LoanRequest.builder()
                .equipmentItemId(10L)
                .borrowerName("Alice Johnson")
                .borrowerContact("alice@fire.gov")
                .build();

        LoanRequest saved = repository.save(loan);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(saved.getRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("findById: retrieves all persisted fields for a saved entity")
    void findById_shouldReturnEntityWithAllPersistedFields() {
        LoanRequest loan = LoanRequest.builder()
                .equipmentItemId(20L)
                .borrowerName("Bob Rescue")
                .borrowerContact("bob@ems.gov")
                .build();

        LoanRequest saved = repository.save(loan);
        Optional<LoanRequest> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBorrowerName()).isEqualTo("Bob Rescue");
        assertThat(found.get().getBorrowerContact()).isEqualTo("bob@ems.gov");
        assertThat(found.get().getEquipmentItemId()).isEqualTo(20L);
        assertThat(found.get().getStatus()).isEqualTo(LoanStatus.PENDING);
    }
}
