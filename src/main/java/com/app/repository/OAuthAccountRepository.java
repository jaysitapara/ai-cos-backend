package com.app.repository;

import com.app.entity.OAuthAccountEntity;
import com.app.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, Long> {

    Optional<OAuthAccountEntity> findByProviderAndProviderUserIdAndDeletedAtIsNull(String provider, String providerUserId);

    List<OAuthAccountEntity> findAllByUserAndDeletedAtIsNull(UserEntity user);
}
