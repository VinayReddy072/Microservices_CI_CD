package com.emergencylending.loan.service;

import com.emergencylending.loan.client.InventoryAvailabilityAdapter;
import com.emergencylending.loan.dto.EquipmentAvailabilityDto;
import com.emergencylending.loan.dto.LoanRequestCreateDto;
import com.emergencylending.loan.entity.LoanRequest;
import com.emergencylending.loan.entity.LoanStatus;
import com.emergencylending.loan.messaging.LoanEventPublisher;
import com.emergencylending.loan.repository.LoanRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoanRequestService}.
 *
 * <p>
 * Pure Mockito — no Spring context. Exercises the loan lifecycle state machine:
 * PENDING → APPROVED, PENDING → REJECTED, APPROVED → RETURNED, and guard
 * clauses.
 */
@ExtendWith(MockitoExtension.class)
class LoanRequestServiceTest {

        @Mock
        private LoanRequestRepository repository;

        @Mock
        private InventoryAvailabilityAdapter inventoryAdapter;

        @Mock
        private LoanEventPublisher eventPublisher;

        @InjectMocks
        private LoanRequestService service;

        private LoanRequestCreateDto createDto;

        @BeforeEach
        void setUp() {
                createDto = new LoanRequestCreateDto();
                createDto.setEquipmentItemId(42L);
                createDto.setBorrowerName("Jane Smith");
                createDto.setBorrowerContact("jane@example.com");
        }

        @Test
        @DisplayName("create: persists entity with PENDING status and correct fields")
        void create_shouldPersistWithPendingStatus() {
                LoanRequest saved = LoanRequest.builder()
                                .id(1L)
                                .equipmentItemId(42L)
                                .borrowerName("Jane Smith")
                                .borrowerContact("jane@example.com")
                                .status(LoanStatus.PENDING)
                                .build();

                when(repository.save(any(LoanRequest.class))).thenReturn(saved);

                LoanRequest result = service.create(createDto);

                assertThat(result.getStatus()).isEqualTo(LoanStatus.PENDING);
                assertThat(result.getEquipmentItemId()).isEqualTo(42L);
                assertThat(result.getBorrowerName()).isEqualTo("Jane Smith");
                verify(repository).save(any(LoanRequest.class));
        }

        @Test
        @DisplayName("approve: transitions to APPROVED when inventory reports equipment available")
        void approve_whenEquipmentAvailable_shouldSetApproved() {
                LoanRequest pending = LoanRequest.builder()
                                .id(1L)
                                .equipmentItemId(42L)
                                .status(LoanStatus.PENDING)
                                .build();
                LoanRequest approved = LoanRequest.builder()
                                .id(1L)
                                .equipmentItemId(42L)
                                .status(LoanStatus.APPROVED)
                                .build();

                when(repository.findById(1L)).thenReturn(Optional.of(pending));
                when(inventoryAdapter.checkAvailability(42L))
                                .thenReturn(new EquipmentAvailabilityDto(42L, true, "AVAILABLE"));
                when(repository.save(any(LoanRequest.class))).thenReturn(approved);

                LoanRequest result = service.approve(1L);

                assertThat(result.getStatus()).isEqualTo(LoanStatus.REJECTED);
                verify(eventPublisher).publishApproved(any());
        }

        @Test
        @DisplayName("approve: transitions to REJECTED when inventory reports equipment unavailable")
        void approve_whenEquipmentUnavailable_shouldSetRejected() {
                LoanRequest pending = LoanRequest.builder()
                                .id(2L)
                                .equipmentItemId(99L)
                                .status(LoanStatus.PENDING)
                                .build();
                LoanRequest rejected = LoanRequest.builder()
                                .id(2L)
                                .equipmentItemId(99L)
                                .status(LoanStatus.REJECTED)
                                .build();

                when(repository.findById(2L)).thenReturn(Optional.of(pending));
                when(inventoryAdapter.checkAvailability(99L))
                                .thenReturn(new EquipmentAvailabilityDto(99L, false, "ON_LOAN"));
                when(repository.save(any(LoanRequest.class))).thenReturn(rejected);

                LoanRequest result = service.approve(2L);

                assertThat(result.getStatus()).isEqualTo(LoanStatus.REJECTED);
                verify(eventPublisher, never()).publishApproved(any());
        }

        @Test
        @DisplayName("approve: throws IllegalStateException when loan is not in PENDING status")
        void approve_whenNotPending_shouldThrowIllegalState() {
                LoanRequest alreadyApproved = LoanRequest.builder()
                                .id(3L)
                                .equipmentItemId(5L)
                                .status(LoanStatus.APPROVED)
                                .build();

                when(repository.findById(3L)).thenReturn(Optional.of(alreadyApproved));

                assertThatThrownBy(() -> service.approve(3L))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Cannot approve loan");
        }

        @Test
        @DisplayName("returnLoan: transitions from APPROVED to RETURNED and publishes event")
        void returnLoan_whenApproved_shouldSetReturned() {
                LoanRequest approved = LoanRequest.builder()
                                .id(4L)
                                .equipmentItemId(10L)
                                .status(LoanStatus.APPROVED)
                                .build();
                LoanRequest returned = LoanRequest.builder()
                                .id(4L)
                                .equipmentItemId(10L)
                                .status(LoanStatus.RETURNED)
                                .build();

                when(repository.findById(4L)).thenReturn(Optional.of(approved));
                when(repository.save(any(LoanRequest.class))).thenReturn(returned);

                LoanRequest result = service.returnLoan(4L);

                assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
                verify(eventPublisher).publishReturned(any());
        }

        @Test
        @DisplayName("returnLoan: throws IllegalStateException when loan is not in APPROVED status")
        void returnLoan_whenNotApproved_shouldThrowIllegalState() {
                LoanRequest pending = LoanRequest.builder()
                                .id(5L)
                                .equipmentItemId(7L)
                                .status(LoanStatus.PENDING)
                                .build();

                when(repository.findById(5L)).thenReturn(Optional.of(pending));

                assertThatThrownBy(() -> service.returnLoan(5L))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Cannot return loan");
        }

        @Test
        @DisplayName("findById: throws EntityNotFoundException for unknown loan ID")
        void findById_whenNotFound_shouldThrowEntityNotFound() {
                when(repository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.findById(999L))
                                .isInstanceOf(EntityNotFoundException.class)
                                .hasMessageContaining("LoanRequest not found: 999");
        }
}
