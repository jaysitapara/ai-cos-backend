package com.app.service;

import com.app.entity.BrandEntity;
import com.app.entity.UserEntity;
import com.app.repository.BrandKnowledgeRepository;
import com.app.repository.BrandMemoryRepository;
import com.app.repository.BrandProductRepository;
import com.app.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Brand Management Service Tests")
class BrandManagementServiceTest {

    @Mock
    private BrandRepository brandRepository;
    @Mock
    private BrandProductRepository brandProductRepository;
    @Mock
    private BrandKnowledgeRepository brandKnowledgeRepository;
    @Mock
    private BrandMemoryRepository brandMemoryRepository;

    @InjectMocks
    private BrandManagementService brandService;

    private UserEntity user;
    private BrandEntity brand;
    private UUID brandPublicId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = UserEntity.builder().id(1L).email("user@example.com").build();
        brandPublicId = UUID.randomUUID();
        brand = BrandEntity.builder()
            .id(10L)
            .publicId(brandPublicId)
            .user(user)
            .name("Acme Tech")
            .industry("Technology")
            .build();
    }

    @Test
    @DisplayName("Should retrieve brands owned by user")
    void testGetBrandsForUser() {
        when(brandRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(brand));

        List<BrandEntity> result = brandService.getBrandsForUser(user);

        assertEquals(1, result.size());
        assertEquals("Acme Tech", result.get(0).getName());
        verify(brandRepository, times(1)).findByUserIdOrderByUpdatedAtDesc(1L);
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when user accesses unowned brand")
    void testUnownedBrandThrowsException() {
        when(brandRepository.findByPublicIdAndUserId(brandPublicId, 1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> brandService.getBrandByPublicId(user, brandPublicId));
    }

    @Test
    @DisplayName("Should create brand profile assigned to user")
    void testCreateBrand() {
        BrandEntity draft = BrandEntity.builder().name("New Brand").industry("AI").build();
        when(brandRepository.save(any(BrandEntity.class))).thenAnswer(i -> i.getArgument(0));

        BrandEntity created = brandService.createBrand(user, draft);

        assertNotNull(created.getPublicId());
        assertEquals(user, created.getUser());
        assertEquals("New Brand", created.getName());
    }
}
