package com.company.common.signature.repository;

import com.company.common.signature.entity.SignatureDiagram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignatureDiagramRepository extends JpaRepository<SignatureDiagram, Long> {

    Optional<SignatureDiagram> findByOwnerTypeAndOwnerId(String ownerType, Long ownerId);

    boolean existsByOwnerTypeAndOwnerId(String ownerType, Long ownerId);
}
