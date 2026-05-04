package com.circleguard.promotion.service;

import com.circleguard.promotion.model.graph.CircleNode;
import com.circleguard.promotion.repository.graph.CircleNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircleInviteCodeFormatTest {

    @Mock
    private CircleNodeRepository circleRepository;

    @Mock
    private HealthStatusService healthStatusService;

    @InjectMocks
    private CircleService circleService;

    // The invite code must always match "MESH-" followed by 4 chars from the allowed alphabet
    @Test
    void createCircle_GeneratedInviteCodeMatchesMeshFormat() {
        when(circleRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(circleRepository.save(any(CircleNode.class))).thenAnswer(inv -> inv.getArgument(0));

        CircleNode result = circleService.createCircle("Aula 3B", "room-101");

        assertThat(result.getInviteCode()).matches("MESH-[A-Z2-9]{4}");
    }

    // On a first-call collision, the service must keep generating until it finds a unique code
    @Test
    void createCircle_WhenFirstCodeCollides_RetriesToGenerateUniqueCode() {
        // First existsByInviteCode call → collision; second → free
        when(circleRepository.existsByInviteCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(circleRepository.save(any(CircleNode.class))).thenAnswer(inv -> inv.getArgument(0));

        circleService.createCircle("Lab 2", "room-202");

        // Must have checked at least twice due to the collision on the first attempt
        verify(circleRepository, atLeast(2)).existsByInviteCode(anyString());
    }

    // The code saved to the repository must be the same code that ends up in the entity
    @Test
    void createCircle_SavedEntityContainsSameCodeThatWasCheckedForUniqueness() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CircleNode> nodeCaptor = ArgumentCaptor.forClass(CircleNode.class);

        when(circleRepository.existsByInviteCode(codeCaptor.capture())).thenReturn(false);
        when(circleRepository.save(nodeCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        circleService.createCircle("Class Room", "room-303");

        assertThat(nodeCaptor.getValue().getInviteCode())
                .isEqualTo(codeCaptor.getValue());
    }
}
