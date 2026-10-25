package br.com.medclinic.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Administrative aggregate. Contains no framework or persistence dependencies. */
public class Patient {
    public UUID id = UUID.randomUUID();
    public String chart;
    public long version;
    public String status = "ATIVO";
    public UUID mergedInto;
    public String name = "";
    public LocalDate birthdate;
    public String sex = "NAO_INFORMADO";
    public String nationality = "BR";
    public String cpf = "";
    public String alternativeType = "";
    public String alternativeNumber = "";
    public String alternativeCountry = "";
    public String alternativeIssuer = "";
    public String alternativeReason = "";
    public String phone = "";
    public String email = "";
    public String address = "";
    public String country = "BR";
    public String preference = "PARTICULAR";
    public List<Guardian> guardians = new ArrayList<>();
    public Emergency emergency;
    public List<Coverage> coverages = new ArrayList<>();
    public List<Consent> consents = new ArrayList<>();
    public List<Attachment> attachments = new ArrayList<>();
    public List<Merge> merges = new ArrayList<>();

    public record Guardian(
            UUID id,
            UUID origin,
            String name,
            String relationship,
            String document,
            String phone,
            LocalDate startsOn,
            LocalDate endsOn) {
        public boolean currentOn(LocalDate date) {
            return !startsOn.isAfter(date) && (endsOn == null || date.isBefore(endsOn));
        }
    }

    public record Emergency(String name, String relationship, String phone) {}

    public record Coverage(
            UUID id,
            UUID origin,
            String insurer,
            String plan,
            String card,
            LocalDate expiresOn,
            LocalDate startsOn,
            LocalDate endsOn,
            UUID supersedesCoverageId) {
        public boolean inForceOn(LocalDate date) {
            return !date.isBefore(startsOn) && (endsOn == null || date.isBefore(endsOn));
        }

        public boolean expiredOn(LocalDate date) {
            return expiresOn.isBefore(date);
        }
    }

    public record Consent(
            UUID id,
            UUID origin,
            String purpose,
            String termVersion,
            String decision,
            LocalDate manifestedOn,
            Instant recordedAt,
            String declarant,
            UUID guardianId,
            String actor,
            UUID evidenceId,
            long generation) {}

    public record Attachment(
            UUID id,
            UUID origin,
            String category,
            String classification,
            String mediaType,
            long size,
            String storageKey,
            String author,
            Instant createdAt,
            boolean invalid,
            String invalidationReason) {
        public boolean administrative() {
            return "ADMINISTRATIVO".equals(classification) && !invalid;
        }
    }

    public record Merge(
            UUID sourceId,
            String sourceChart,
            UUID sourceEvidenceId,
            UUID destinationEvidenceId,
            String reason,
            String choices,
            String actor,
            Instant mergedAt,
            long destinationVersion,
            UUID destinationId) {}
}
