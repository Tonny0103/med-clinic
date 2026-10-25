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
    br.com.medclinic.application.AuditService audit(PatientService patients) {
        return new br.com.medclinic.application.AuditService(patients);
    }

    @Bean
    br.com.medclinic.application.MergeService merges(PatientService patients) {
        return new br.com.medclinic.application.MergeService(patients);
    }

    @Bean
    br.com.medclinic.application.ConsentService consents(
            PatientService patients, br.com.medclinic.application.AttachmentService attachments) {
        return new br.com.medclinic.application.ConsentService(patients, attachments);
    }

    @Bean
    br.com.medclinic.application.FileScanner scanner(
            @Value("${clinic.clam-host}") String host, @Value("${clinic.clam-port}") int port) {
        return new ClamAvScanner(host, port);
    }

    @Bean
    br.com.medclinic.application.PrivateFiles privateFiles(
            @Value("${clinic.files}") String root, br.com.medclinic.application.FileScanner scanner) {
        return new PrivateFileAdapter(java.nio.file.Path.of(root), scanner);
    }

    @Bean
    br.com.medclinic.application.AttachmentService attachments(
            PatientService patients, br.com.medclinic.application.PrivateFiles files) {
        return new br.com.medclinic.application.AttachmentService(patients, files);
    }

    @Bean
    br.com.medclinic.application.RelationshipService relationships(PatientService patients) {
        return new br.com.medclinic.application.RelationshipService(patients);
    }

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
