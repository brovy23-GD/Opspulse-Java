package com.rovy.opspulse.repository;
import com.rovy.opspulse.domain.HealthObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HealthObservationRepository extends JpaRepository<HealthObservation,Long> {
    List<HealthObservation> findTop20ByService_IdOrderByObservedAtDesc(Long serviceId);
}