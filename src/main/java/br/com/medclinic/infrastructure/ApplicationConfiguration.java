package br.com.medclinic.infrastructure;

import br.com.medclinic.application.ClinicStore;
import br.com.medclinic.application.PatientService;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock clinicClock(@Value("${clinic.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    ClinicStore clinicStore(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        return new JdbcClinicStore(jdbc, new TransactionTemplate(manager));
    }

    @Bean
    PatientService patientService(ClinicStore store, Clock clock) {
        return new PatientService(store, clock);
    }
}
