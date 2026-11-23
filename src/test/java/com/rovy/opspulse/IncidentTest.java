package com.rovy.opspulse;
import com.rovy.opspulse.domain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IncidentTest {
    @Test void cannotReopenResolvedIncident(){
        Incident i=new Incident(new ManagedService("payments","Payments","test"),"Outage",Severity.HIGH);
        i.transitionTo(IncidentStatus.INVESTIGATING);
        i.transitionTo(IncidentStatus.RESOLVED);
        assertNotNull(i.getResolvedAt());
        assertThrows(IllegalStateException.class,()->i.transitionTo(IncidentStatus.OPEN));
    }
}