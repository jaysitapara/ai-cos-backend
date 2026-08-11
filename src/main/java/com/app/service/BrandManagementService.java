package com.app.service;

import com.app.entity.*;
import com.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandManagementService {

    private final BrandRepository brandRepository;
    private final BrandProductRepository brandProductRepository;
    private final BrandKnowledgeRepository brandKnowledgeRepository;
    private final BrandMemoryRepository brandMemoryRepository;

    @Transactional(readOnly = true)
    public List<BrandEntity> getBrandsForUser(UserEntity user) {
        return brandRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public BrandEntity getBrandByPublicId(UserEntity user, UUID publicId) {
        return brandRepository.findByPublicIdAndUserId(publicId, user.getId())
            .orElseThrow(() -> new NoSuchElementException("Brand not found or access denied for ID: " + publicId));
    }

    @Transactional
    public BrandEntity createBrand(UserEntity user, BrandEntity draft) {
        draft.setUser(user);
        draft.setPublicId(UUID.randomUUID());
        draft.setCreatedAt(OffsetDateTime.now());
        draft.setUpdatedAt(OffsetDateTime.now());
        BrandEntity saved = brandRepository.save(draft);
        log.info("User [{}] created new brand [{}] ({})", user.getEmail(), saved.getName(), saved.getPublicId());
        return saved;
    }

    @Transactional
    public BrandEntity updateBrand(UserEntity user, UUID publicId, BrandEntity updates) {
        BrandEntity existing = getBrandByPublicId(user, publicId);
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getIndustry() != null) existing.setIndustry(updates.getIndustry());
        if (updates.getPositioning() != null) existing.setPositioning(updates.getPositioning());
        if (updates.getTargetAudience() != null) existing.setTargetAudience(updates.getTargetAudience());
        if (updates.getProductsSummary() != null) existing.setProductsSummary(updates.getProductsSummary());
        if (updates.getServicesSummary() != null) existing.setServicesSummary(updates.getServicesSummary());
        if (updates.getUsp() != null) existing.setUsp(updates.getUsp());
        if (updates.getWebsite() != null) existing.setWebsite(updates.getWebsite());
        if (updates.getBrandVoice() != null) existing.setBrandVoice(updates.getBrandVoice());
        if (updates.getWritingStyle() != null) existing.setWritingStyle(updates.getWritingStyle());
        if (updates.getWordsToUse() != null) existing.setWordsToUse(updates.getWordsToUse());
        if (updates.getWordsToAvoid() != null) existing.setWordsToAvoid(updates.getWordsToAvoid());
        if (updates.getContentGoals() != null) existing.setContentGoals(updates.getContentGoals());
        if (updates.getBrandGuidelines() != null) existing.setBrandGuidelines(updates.getBrandGuidelines());

        existing.setUpdatedAt(OffsetDateTime.now());
        return brandRepository.save(existing);
    }

    @Transactional
    public void deleteBrand(UserEntity user, UUID publicId) {
        BrandEntity brand = getBrandByPublicId(user, publicId);
        brandRepository.delete(brand);
        log.info("User [{}] deleted brand [{}] ({})", user.getEmail(), brand.getName(), publicId);
    }

    // ─── Knowledge Items ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BrandKnowledgeEntity> getKnowledgeItems(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = getBrandByPublicId(user, brandPublicId);
        return brandKnowledgeRepository.findByBrandIdAndUserIdOrderByUpdatedAtDesc(brand.getId(), user.getId());
    }

    @Transactional
    public BrandKnowledgeEntity addKnowledgeItem(UserEntity user, UUID brandPublicId, BrandKnowledgeEntity knowledge) {
        BrandEntity brand = getBrandByPublicId(user, brandPublicId);
        knowledge.setUser(user);
        knowledge.setBrand(brand);
        knowledge.setPublicId(UUID.randomUUID());
        knowledge.setCreatedAt(OffsetDateTime.now());
        knowledge.setUpdatedAt(OffsetDateTime.now());
        return brandKnowledgeRepository.save(knowledge);
    }

    // ─── Products & Services ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BrandProductEntity> getProducts(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = getBrandByPublicId(user, brandPublicId);
        return brandProductRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
    }

    @Transactional
    public BrandProductEntity addProduct(UserEntity user, UUID brandPublicId, BrandProductEntity product) {
        BrandEntity brand = getBrandByPublicId(user, brandPublicId);
        product.setUser(user);
        product.setBrand(brand);
        product.setPublicId(UUID.randomUUID());
        product.setCreatedAt(OffsetDateTime.now());
        return brandProductRepository.save(product);
    }

    // ─── Brand Learned Memories ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BrandMemoryEntity> getBrandMemories(UserEntity user, UUID brandPublicId) {
        BrandEntity brand = getBrandByPublicId(user, brandPublicId);
        return brandMemoryRepository.findByBrandIdAndUserIdOrderByCreatedAtDesc(brand.getId(), user.getId());
    }
}
