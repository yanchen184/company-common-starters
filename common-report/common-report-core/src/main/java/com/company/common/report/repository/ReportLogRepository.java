package com.company.common.report.repository;

import com.company.common.report.entity.ReportLog;
import com.company.common.report.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 報表記錄 Repository
 */
public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {

    Optional<ReportLog> findByUuid(String uuid);

    @Modifying
    @Query("DELETE FROM ReportLogBlob b WHERE b.reportLog.id IN " +
           "(SELECT r.id FROM ReportLog r WHERE r.createdDate < :date AND r.status = :status)")
    void deleteBlobsByCreatedDateBeforeAndStatus(@Param("date") LocalDateTime date,
                                                  @Param("status") ReportStatus status);

    @Modifying
    @Query("DELETE FROM ReportLog r WHERE r.createdDate < :date AND r.status = :status")
    void deleteLogsByCreatedDateBeforeAndStatus(@Param("date") LocalDateTime date,
                                                 @Param("status") ReportStatus status);
}
