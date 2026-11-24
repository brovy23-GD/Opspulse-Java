package com.rovy.opspulse.repository;

import com.rovy.opspulse.domain.MonitorCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, Long> {
    List<MonitorCheck> findTop30ByOrderByCheckedAtDescIdDesc();
    List<MonitorCheck> findTop20ByService_IdOrderByCheckedAtDescIdDesc(Long serviceId);
}
