package com.rovy.opspulse.repository;
import com.rovy.opspulse.domain.Incident;
import com.rovy.opspulse.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface IncidentRepository extends JpaRepository<Incident,Long> {
    List<Incident> findByService_IdOrderByOpenedAtDesc(Long serviceId);
    long countByStatusNot(IncidentStatus status);
}