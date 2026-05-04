package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceIdempotencyTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hashSalt", "test-salt");
    }

    // Core idempotency: same identity must always yield the same anonymousId
    @Test
    void getOrCreateAnonymousId_SameIdentityCalledTwice_ReturnsSameIdWithoutCreatingNewMapping() {
        UUID existingId = UUID.randomUUID();
        IdentityMapping existing = IdentityMapping.builder()
                .anonymousId(existingId)
                .realIdentity("student@university.edu")
                .identityHash("irrelevant-stored-hash")
                .salt("somesalt")
                .build();

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(existing));

        UUID firstCall  = service.getOrCreateAnonymousId("student@university.edu");
        UUID secondCall = service.getOrCreateAnonymousId("student@university.edu");

        assertThat(firstCall).isEqualTo(existingId);
        assertThat(secondCall).isEqualTo(existingId);
        // Idempotent: no new row should ever be persisted for an existing identity
        verify(repository, never()).save(any());
    }

    // New identity: must persist and return the generated UUID
    @Test
    void getOrCreateAnonymousId_NewIdentity_PersistsAndReturnsGeneratedId() {
        UUID newId = UUID.randomUUID();
        IdentityMapping saved = IdentityMapping.builder()
                .anonymousId(newId)
                .realIdentity("newuser@university.edu")
                .identityHash("hash")
                .salt("salt")
                .build();

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenReturn(saved);

        UUID result = service.getOrCreateAnonymousId("newuser@university.edu");

        assertThat(result).isEqualTo(newId);
        verify(repository, times(1)).save(any(IdentityMapping.class));
    }

    // Two different real identities must produce two distinct anonymousIds
    @Test
    void getOrCreateAnonymousId_TwoDifferentIdentities_ProduceDifferentAnonymousIds() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();

        IdentityMapping mappingA = IdentityMapping.builder().anonymousId(idA)
                .realIdentity("a@uni.edu").identityHash("hA").salt("s").build();
        IdentityMapping mappingB = IdentityMapping.builder().anonymousId(idB)
                .realIdentity("b@uni.edu").identityHash("hB").salt("s").build();

        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenReturn(mappingA, mappingB);

        UUID resultA = service.getOrCreateAnonymousId("a@uni.edu");
        UUID resultB = service.getOrCreateAnonymousId("b@uni.edu");

        assertThat(resultA).isNotEqualTo(resultB);
    }
}
