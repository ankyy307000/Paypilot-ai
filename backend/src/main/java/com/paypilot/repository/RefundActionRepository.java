
package com.paypilot.repository;

import com.paypilot.entity.RefundActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundActionRepository
        extends JpaRepository<RefundActionEntity, String> {

    List<RefundActionEntity> findByCaptureId(String captureId);

    List<RefundActionEntity> findAllByOrderByCreatedAtDesc();
}
