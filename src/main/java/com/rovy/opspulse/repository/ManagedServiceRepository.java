package com.rovy.opspulse.repository;
import com.rovy.opspulse.domain.ManagedService;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ManagedServiceRepository extends JpaRepository<ManagedService,Long> {
    boolean existsByServiceKeyIgnoreCase(String serviceKey);
}