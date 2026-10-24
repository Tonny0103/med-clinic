package br.com.medclinic.infrastructure;

import static org.assertj.core.api.Assertions.*;

import br.com.medclinic.application.ClinicStore;
import br.com.medclinic.domain.Patient;
import br.com.medclinic.domain.RuleViolation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class FoundationTest {
    @Autowired
    br.com.medclinic.application.AuditService audit;

    @Test
    void auditQueryIsManagerOnlyFilteredAndAuditsItselfOnce() throws Exception {
        String uniqueActor = "usuario-teste-" + UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        store.audit(new ClinicStore.Event(
                eventId, Instant.now(), uniqueActor, "RECEPCAO", "EVENTO_TESTE", null, "SUCESSO", "phone", "", ""));
        long before = store.events().stream()
                .filter(e -> e.operation().equals("CONSULTAR_AUDITORIA")
                        && e.outcome().equals("SUCESSO"))
                .count();
        var report = audit.query(
                new br.com.medclinic.application.Actor("gerente", "GERENTE"),
                java.util.Map.of("actorFilter", uniqueActor),
                0);
        assertThat(report.page().items())
                .hasSize(1)
                .extracting(ClinicStore.Event::id)
                .containsExactly(eventId);
        assertThat(store.events().stream()
                        .filter(e -> e.operation().equals("CONSULTAR_AUDITORIA")
                                && e.outcome().equals("SUCESSO"))
                        .count())
                .isEqualTo(before + 1);
        assertThatThrownBy(() -> audit.query(
                        new br.com.medclinic.application.Actor("recepcao", "RECEPCAO"), java.util.Map.of(), 0))
                .isInstanceOf(br.com.medclinic.application.AccessDenied.class);
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/auditoria")
                                .param("actorFilter", uniqueActor)
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("gerente")
                                        .roles("GERENTE")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(uniqueActor)));
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/auditoria")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("medico")
                                        .roles("MEDICO")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isForbidden());
        assertThat(store.events().toString()).doesNotContain("00000000353", "Pessoa fictícia de cadastro");
    }

    @Autowired
    br.com.medclinic.application.MergeService merges;

    @Test
    void mergeKeepsAliasesEvidenceAndRevocationAndIsIdempotent() throws Exception {
        Patient source = mergingPatient("SOURCE"), destination = mergingPatient("DEST");
        store.transaction(() -> {
            store.save(source, -1);
            store.save(destination, -1);
            return null;
        });
        var manager = new br.com.medclinic.application.Actor("gerente", "GERENTE");
        byte[] png = PrivateFileTest.png();
        attachments.upload(
                manager, source.id, 1, UUID.randomUUID(), java.util.Map.of("category", "IDENTIDADE"), png, "image/png");
        attachments.upload(
                manager,
                destination.id,
                1,
                UUID.randomUUID(),
                java.util.Map.of("category", "IDENTIDADE"),
                png,
                "image/png");
        UUID sourceProof =
                store.find(source.id).orElseThrow().attachments.getFirst().id();
        UUID destinationProof =
                store.find(destination.id).orElseThrow().attachments.getFirst().id();
        var form = new java.util.HashMap<>(java.util.Map.of(
                "purpose",
                "IMAGEM",
                "termVersion",
                "DEMO-v1",
                "manifestedOn",
                patients.today().toString(),
                "declarant",
                "Paciente fictício",
                "evidenceId",
                sourceProof.toString()));
        consents.record(manager, source.id, 2, UUID.randomUUID(), form, true, null, null);
        form.put("evidenceId", destinationProof.toString());
        form.put("decision", "CONCEDIDO");
        consents.record(manager, destination.id, 2, UUID.randomUUID(), form, false, null, null);
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/mesclagem")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("gerente")
                                        .roles("GERENTE")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk());
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mesclagem/comparar")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("gerente")
                                        .roles("GERENTE"))
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .param("sourceId", source.id.toString())
                                .param("destinationId", destination.id.toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk());
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mesclagem/previa")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("gerente")
                                        .roles("GERENTE"))
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .param("sourceId", source.id.toString())
                                .param("destinationId", destination.id.toString())
                                .param("sourceVersion", "3")
                                .param("destinationVersion", "3")
                                .param("sourceEvidenceId", sourceProof.toString())
                                .param("destinationEvidenceId", destinationProof.toString())
                                .param("reason", "Identidades fictícias conferidas"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("REVOGADO")));
        var preview = merges.preview(
                manager,
                source.id,
                destination.id,
                3,
                3,
                sourceProof,
                destinationProof,
                java.util.Map.of(),
                "Mesma pessoa, provas fictícias conferidas");
        UUID token = UUID.randomUUID();
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mesclagem/confirmar")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("gerente")
                                        .roles("GERENTE"))
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .param("previewId", preview.id().toString())
                                .param("token", token.toString())
                                .param("confirmed", "sim"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/pacientes/" + destination.id + "?sucesso"));
        assertThat(merges.confirm(manager, preview.id(), token, true)).isEqualTo(destination.id);
        Patient merged = store.find(destination.id).orElseThrow();
        assertThat(merged.attachments).hasSize(2);
        assertThat(merged.consents).hasSize(2);
        assertThat(merged.chart).isEqualTo(destination.chart);
        assertThat(br.com.medclinic.application.ConsentService.status(merged, "IMAGEM"))
                .isEqualTo("REVOGADO");
        assertThat(store.find(source.id).orElseThrow().status).isEqualTo("MESCLADO");
        assertThat(patients.read(manager, source.id).originalChart()).isEqualTo(source.chart);
        assertThat(patients.read(manager, source.id).patient().id).isEqualTo(destination.id);
        var controlledHistory = new br.com.medclinic.application.PatientService(
                store,
                java.time.Clock.system(java.time.ZoneId.of("America/Cuiaba")),
                id -> new br.com.medclinic.application.VisitHistory.Result(
                        br.com.medclinic.application.VisitHistory.State.DISPONIVEL,
                        java.util.List.of(new br.com.medclinic.application.VisitHistory.Entry(
                                patients.today(), "Profissional fictício", "Consulta", id))));
        assertThat(controlledHistory.read(manager, source.id).history().entries())
                .hasSize(2)
                .extracting(br.com.medclinic.application.VisitHistory.Entry::origin)
                .containsExactlyInAnyOrder(source.id, destination.id);
        assertThatThrownBy(() -> patients.changeStatus(
                        manager,
                        source.id,
                        4,
                        UUID.randomUUID(),
                        java.util.Map.of("target", "ATIVO", "reason", "Tentativa")))
                .isInstanceOf(RuleViolation.class);
    }

    @Test
    void invalidatingProofMakesMergePreviewStaleWithoutPartialConsolidation() throws Exception {
        Patient source = mergingPatient("STALE-SOURCE"), destination = mergingPatient("STALE-DEST");
        store.transaction(() -> {
            store.save(source, -1);
            store.save(destination, -1);
            return null;
        });
        var manager = new br.com.medclinic.application.Actor("gerente", "GERENTE");
        byte[] png = PrivateFileTest.png();
        attachments.upload(
                manager, source.id, 1, UUID.randomUUID(), java.util.Map.of("category", "IDENTIDADE"), png, "image/png");
        attachments.upload(
                manager,
                destination.id,
                1,
                UUID.randomUUID(),
                java.util.Map.of("category", "IDENTIDADE"),
                png,
                "image/png");
        UUID sourceProof =
                store.find(source.id).orElseThrow().attachments.getFirst().id();
        UUID destinationProof =
                store.find(destination.id).orElseThrow().attachments.getFirst().id();
        var preview = merges.preview(
                manager,
                source.id,
                destination.id,
                2,
                2,
                sourceProof,
                destinationProof,
                java.util.Map.of(),
                "Provas conferidas na demonstração");
        attachments.invalidate(
                manager,
                source.id,
                2,
                UUID.randomUUID(),
                java.util.Map.of("attachmentId", sourceProof.toString(), "reason", "Vínculo corrigido"));
        assertThatThrownBy(() -> merges.confirm(manager, preview.id(), UUID.randomUUID(), true))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(source.id).orElseThrow().status).isEqualTo("ATIVO");
        assertThat(store.find(destination.id).orElseThrow().attachments).hasSize(1);
        assertThat(store.find(destination.id).orElseThrow().merges).isEmpty();
    }

    private Patient mergingPatient(String document) {
        Patient p = fixture();
        p.name = "Paciente fictício para mesclagem";
        p.cpf = "";
        p.nationality = "AR";
        p.alternativeType = "PASSAPORTE";
        p.alternativeNumber = document;
        p.alternativeCountry = "AR";
        p.alternativeIssuer = "DEMO";
        p.alternativeReason = "Sem CPF";
        p.phone = "+55 65 90000-0000";
        p.address = "Rua fictícia, 10";
        return p;
    }

    @Autowired
    br.com.medclinic.application.ConsentService consents;

    @Test
    void revocationOfInactivePatientCanAtomicallyAddEvidenceOnlyInThatFlow() throws Exception {
        Patient p = fixture();
        p.cpf = "00000000949";
        store.transaction(() -> {
            store.save(p, -1);
            return null;
        });
        var actor = new br.com.medclinic.application.Actor("recepcao", "RECEPCAO");
        byte[] bytes = PrivateFileTest.png();
        attachments.upload(
                actor, p.id, 1, UUID.randomUUID(), java.util.Map.of("category", "IDENTIDADE"), bytes, "image/png");
        UUID proof = store.find(p.id).orElseThrow().attachments.getFirst().id();
        var form = new java.util.HashMap<>(java.util.Map.of(
                "purpose",
                "PROTECAO_DADOS",
                "termVersion",
                "DEMO-v1",
                "decision",
                "CONCEDIDO",
                "manifestedOn",
                patients.today().toString(),
                "declarant",
                "Paciente fictício",
                "evidenceId",
                proof.toString()));
        consents.record(actor, p.id, 2, UUID.randomUUID(), form, false, null, null);
        patients.changeStatus(
                actor, p.id, 3, UUID.randomUUID(), java.util.Map.of("target", "INATIVO", "reason", "Pausa"));
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                                        "/pacientes/" + p.id + "/revogacoes")
                                .file(new org.springframework.mock.web.MockMultipartFile(
                                        "file", "evidencia-ficticia.png", "image/png", bytes))
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("recepcao")
                                        .roles("RECEPCAO"))
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .param("version", "4")
                                .param("token", UUID.randomUUID().toString())
                                .param("purpose", "PROTECAO_DADOS")
                                .param("termVersion", "DEMO-v1")
                                .param("manifestedOn", patients.today().toString())
                                .param("declarant", "Paciente fictício")
                                .param("evidenceId", ""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .is3xxRedirection());
        Patient revoked = store.find(p.id).orElseThrow();
        assertThat(revoked.status).isEqualTo("INATIVO");
        assertThat(revoked.consents).hasSize(2);
        assertThat(revoked.attachments).hasSize(2);
        assertThat(revoked.attachments.getLast().category()).isEqualTo("EVIDENCIA_REVOGACAO");
        assertThat(br.com.medclinic.application.ConsentService.status(revoked, "PROTECAO_DADOS"))
                .isEqualTo("REVOGADO");
        attachments.invalidate(
                new br.com.medclinic.application.Actor("gerente", "GERENTE"),
                p.id,
                5,
                UUID.randomUUID(),
                java.util.Map.of(
                        "attachmentId",
                        revoked.attachments.getLast().id().toString(),
                        "reason",
                        "Teste de invalidação"));
        assertThat(br.com.medclinic.application.ConsentService.status(
                        store.find(p.id).orElseThrow(), "PROTECAO_DADOS"))
                .isEqualTo("REVOGADO");
    }

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    br.com.medclinic.application.FileScanner scanner;

    @Autowired
    br.com.medclinic.application.AttachmentService attachments;

    @Test
    void clinicalFilesAreDeniedAndAuditedForEveryRoleAndInactiveCannotUpload() throws Exception {
        Patient p = fixture();
        p.cpf = "00000000868";
        store.transaction(() -> {
            store.save(p, -1);
            return null;
        });
        var actor = new br.com.medclinic.application.Actor("recepcao", "RECEPCAO");
        byte[] content = PrivateFileTest.png();
        attachments.upload(
                actor, p.id, 1, UUID.randomUUID(), java.util.Map.of("category", "EXAME"), content, "image/png");
        UUID clinicalId = store.find(p.id).orElseThrow().attachments.getFirst().id();
        for (String role : java.util.List.of("RECEPCAO", "GERENTE", "MEDICO")) {
            assertThatThrownBy(
                            () -> attachments.download(new br.com.medclinic.application.Actor(role, role), clinicalId))
                    .isInstanceOf(br.com.medclinic.application.AccessDenied.class);
        }
        assertThat(store.events().stream()
                        .filter(e -> e.operation().equals("LER_ANEXO")
                                && e.outcome().equals("NEGADO")
                                && clinicalId.toString().equals(e.references())))
                .hasSize(3);
        attachments.upload(
                actor, p.id, 2, UUID.randomUUID(), java.util.Map.of("category", "IDENTIDADE"), content, "image/png");
        UUID administrativeId =
                store.find(p.id).orElseThrow().attachments.getLast().id();
        assertThat(attachments
                        .download(new br.com.medclinic.application.Actor("medico", "MEDICO"), administrativeId)
                        .content())
                .isEqualTo(content);
        attachments.invalidate(
                new br.com.medclinic.application.Actor("gerente", "GERENTE"),
                p.id,
                3,
                UUID.randomUUID(),
                java.util.Map.of("attachmentId", administrativeId.toString(), "reason", "Vínculo de teste"));
        assertThatThrownBy(() -> attachments.download(actor, administrativeId))
                .isInstanceOf(br.com.medclinic.application.AccessDenied.class);
        patients.changeStatus(
                actor, p.id, 4, UUID.randomUUID(), java.util.Map.of("target", "INATIVO", "reason", "Pausa"));
        assertThatThrownBy(() -> attachments.upload(
                        actor,
                        p.id,
                        5,
                        UUID.randomUUID(),
                        java.util.Map.of("category", "IDENTIDADE", "revocation", "true"),
                        content,
                        "image/png"))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(p.id).orElseThrow().attachments).hasSize(2);
    }

    @Autowired
    org.springframework.test.web.servlet.MockMvc web;

    @Test
    void registrationPageRendersAndInvalidPostKeepsValues() throws Exception {
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/pacientes/novo")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("recepcao")
                                        .roles("RECEPCAO")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/pacientes")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("recepcao")
                                        .roles("RECEPCAO"))
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .param("token", UUID.randomUUID().toString())
                                .param("name", "Nome fictício preservado"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .is(422))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Nome fictício preservado")));
    }

    @Autowired
    br.com.medclinic.application.PatientService patients;

    @Autowired
    ClinicStore store;

    @Test
    void createsOnceAndRejectsDuplicateDocumentAndDoctorWrite() throws Exception {
        var reception = new br.com.medclinic.application.Actor("recepcao", "RECEPCAO");
        var form = new java.util.HashMap<>(java.util.Map.of(
                "name",
                "Pessoa fictícia de cadastro",
                "birthdate",
                "1990-03-07",
                "sex",
                "NAO_INFORMADO",
                "nationality",
                "BR",
                "cpf",
                "00000000353",
                "phone",
                "+55 65 90000-0000",
                "address",
                "Rua fictícia, 1",
                "country",
                "BR"));
        UUID token = UUID.randomUUID();
        UUID id = patients.create(reception, token, form);
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/pacientes")
                                .param("q", "PESSOA FICTICIA")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("medico")
                                        .roles("MEDICO")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Pessoa fictícia de cadastro")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("00000000353"))));
        web.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/pacientes/" + id)
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.user("medico")
                                        .roles("MEDICO")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Histórico ainda não disponível")));
        assertThat(patients.create(reception, token, form)).isEqualTo(id);
        assertThat(store.find(id).orElseThrow().chart).hasSize(12);
        assertThat(store.events().stream()
                        .filter(e -> e.operation().equals("CRIAR")
                                && id.equals(e.patientId())
                                && e.outcome().equals("SUCESSO")))
                .hasSize(1);
        assertThatThrownBy(() -> patients.create(reception, UUID.randomUUID(), form))
                .isInstanceOf(RuleViolation.class);
        form.put("cpf", "00000000434");
        assertThatThrownBy(() -> patients.create(
                        new br.com.medclinic.application.Actor("medico", "MEDICO"), UUID.randomUUID(), form))
                .isInstanceOf(br.com.medclinic.application.AccessDenied.class);
    }

    @DynamicPropertySource
    static void passwords(DynamicPropertyRegistry registry) {
        registry.add("clinic.reception-password", () -> UUID.randomUUID().toString());
        registry.add("clinic.manager-password", () -> UUID.randomUUID().toString());
        registry.add("clinic.doctor-password", () -> UUID.randomUUID().toString());
    }

    @Test
    void failedTransactionLeavesNeitherPatientNorSuccessAudit() {
        Patient patient = fixture();
        patient.cpf = "00000000515";
        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> store.transaction(() -> {
                    store.save(patient, -1);
                    store.audit(new ClinicStore.Event(
                            eventId,
                            Instant.now(),
                            "teste",
                            "GERENTE",
                            "CRIAR",
                            patient.id,
                            "SUCESSO",
                            "name",
                            "",
                            ""));
                    throw new RuleViolation("test", "Falha controlada");
                }))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(patient.id)).isEmpty();
        assertThat(store.events()).noneMatch(event -> event.id().equals(eventId));
    }

    @Test
    void reservesIdentityAndRejectsStaleVersion() {
        Patient patient = fixture();
        store.transaction(() -> {
            store.save(patient, -1);
            return null;
        });
        Patient competing = fixture();
        competing.cpf = patient.cpf;
        assertThatThrownBy(() -> store.transaction(() -> {
                    store.save(competing, -1);
                    return null;
                }))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(competing.id)).isEmpty();
        Patient stale = store.find(patient.id).orElseThrow();
        store.transaction(() -> {
            store.save(patient, 1);
            return null;
        });
        assertThatThrownBy(() -> store.transaction(() -> {
                    store.save(stale, 1);
                    return null;
                }))
                .isInstanceOf(RuleViolation.class);
        assertThat(store.find(patient.id).orElseThrow().version).isEqualTo(2);
    }

    private Patient fixture() {
        Patient p = new Patient();
        p.chart = store.nextChart();
        p.name = "Paciente fictício " + p.chart;
        p.birthdate = LocalDate.of(1990, 1, 1);
        p.cpf = "00000000272";
        return p;
    }

    @Autowired
    br.com.medclinic.application.RelationshipService relationships;

    @Test
    void renewalPreservesPreviousExpiryAndHistoricalPeriod() {
        Patient p = fixture();
        p.cpf = "00000000787";
        p.phone = "+55 65 90000-0000";
        p.address = "Rua fictícia, 7";
        store.transaction(() -> {
            store.save(p, -1);
            return null;
        });
        var actor = new br.com.medclinic.application.Actor("recepcao", "RECEPCAO");
        LocalDate today = patients.today();
        var form = new java.util.HashMap<>(java.util.Map.of(
                "insurer",
                "Operadora fictícia",
                "plan",
                "Demo",
                "card",
                "CARD-DEMO",
                "startsOn",
                today.minusMonths(2).toString(),
                "expiresOn",
                today.minusDays(10).toString()));
        relationships.update(actor, p.id, 1, UUID.randomUUID(), "convenio", form);
        Patient afterAddition = store.find(p.id).orElseThrow();
        UUID oldId = afterAddition.coverages.getFirst().id();
        relationships.update(
                actor, p.id, 2, UUID.randomUUID(), "preferencia", java.util.Map.of("preference", oldId.toString()));
        form.put("startsOn", today.minusDays(5).toString());
        form.put("expiresOn", today.plusYears(1).toString());
        form.put("supersedesCoverageId", oldId.toString());
        form.put("preferenceAfter", "SUCCESSOR");
        relationships.update(actor, p.id, 3, UUID.randomUUID(), "convenio", form);
        Patient renewed = store.find(p.id).orElseThrow();
        assertThat(renewed.coverages).hasSize(2);
        var old = renewed.coverages.getFirst();
        assertThat(old.expiresOn()).isEqualTo(today.minusDays(10));
        assertThat(old.inForceOn(today.minusDays(20))).isTrue();
        assertThat(old.inForceOn(today)).isFalse();
        assertThat(renewed.coverages.getLast().supersedesCoverageId()).isEqualTo(old.id());
        assertThat(renewed.preference)
                .isEqualTo(renewed.coverages.getLast().id().toString());
    }

    @Test
    void inactivationAndReturnPreserveChartAndRejectStaleChanges() {
        Patient patient = fixture();
        patient.cpf = "00000000604";
        store.transaction(() -> {
            store.save(patient, -1);
            return null;
        });
        String chart = patient.chart;
        var reception = new br.com.medclinic.application.Actor("recepcao", "RECEPCAO");
        var inactive = java.util.Map.of("target", "INATIVO", "reason", "Pausa de demonstração");
        patients.changeStatus(reception, patient.id, 1, UUID.randomUUID(), inactive);
        assertThat(store.find(patient.id).orElseThrow().status).isEqualTo("INATIVO");
        assertThat(patients.search(reception, chart, "ATIVO", 0).total()).isEqualTo(1);
        assertThat(patients.search(reception, patient.name, "ATIVO", 0).total()).isZero();
        assertThatThrownBy(() -> patients.changeStatus(
                        reception,
                        patient.id,
                        1,
                        UUID.randomUUID(),
                        java.util.Map.of("target", "ATIVO", "reason", "Retorno")))
                .isInstanceOf(RuleViolation.class);
        assertThatThrownBy(() -> patients.changeStatus(
                        reception, patient.id, 2, UUID.randomUUID(), java.util.Map.of("target", "ATIVO", "reason", "")))
                .isInstanceOf(RuleViolation.class);
        patients.changeStatus(
                reception,
                patient.id,
                2,
                UUID.randomUUID(),
                java.util.Map.of("target", "ATIVO", "reason", "Retorno de demonstração"));
        Patient restored = store.find(patient.id).orElseThrow();
        assertThat(restored.chart).isEqualTo(chart);
        assertThat(restored.status).isEqualTo("ATIVO");
        assertThat(restored.version).isEqualTo(3);
    }
}
