package br.com.medclinic.domain;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public final class Rules {
    private Rules() {}

    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    public static boolean validCpf(String value) {
        if (value == null || !value.matches("[0-9.\\-\\s]+")) return false;
        String cpf = digits(value);
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;
        for (int size = 9; size <= 10; size++) {
            int sum = 0;
            for (int i = 0; i < size; i++) sum += (cpf.charAt(i) - '0') * (size + 1 - i);
            int digit = (sum * 10) % 11;
            if (digit == 10) digit = 0;
            if (digit != cpf.charAt(size) - '0') return false;
        }
        return true;
    }

    public static String maskedCpf(String value) {
        String cpf = digits(value);
        return cpf.length() == 11 ? "***.***." + cpf.substring(6, 9) + "-**" : "Sem CPF";
    }

    public static boolean suspicious(String a, LocalDate ad, String b, LocalDate bd) {
        if (ad == null || bd == null) return false;
        String x = normalize(a);
        String y = normalize(b);
        if (ad.equals(bd)) return oneEditApart(x, y);
        return x.equals(y)
                && ad.getYear() == bd.getYear()
                && ad.getMonthValue() == bd.getDayOfMonth()
                && ad.getDayOfMonth() == bd.getMonthValue();
    }

    private static boolean oneEditApart(String x, String y) {
        if (x.equals(y)) return true;
        if (Math.abs(x.length() - y.length()) > 1) return false;
        if (x.length() > y.length()) return oneEditApart(y, x);
        int first = 0;
        while (first < x.length() && x.charAt(first) == y.charAt(first)) first++;
        if (x.length() < y.length()) return x.substring(first).equals(y.substring(first + 1));
        if (x.substring(first + 1).equals(y.substring(first + 1))) return true;
        return first + 1 < x.length()
                && x.charAt(first) == y.charAt(first + 1)
                && x.charAt(first + 1) == y.charAt(first)
                && x.substring(first + 2).equals(y.substring(first + 2));
    }

    public static void require(String field, boolean condition, String message) {
        if (!condition) throw new RuleViolation(field, message);
    }

    public static void text(String field, String value, int limit) {
        require(
                field,
                value != null && !value.isBlank() && value.length() <= limit,
                "Preencha " + field + " (até " + limit + " caracteres).");
    }

    public static void phone(String field, String value) {
        require(
                field,
                value != null
                        && value.matches("[+0-9() .-]{7,30}")
                        && digits(value).length() >= 7
                        && digits(value).length() <= 15,
                "Informe telefone válido, com código de área e país quando necessário.");
    }

    public static void validate(Patient p, LocalDate today) {
        text("name", p.name, 160);
        require(
                "birthdate",
                p.birthdate != null && !p.birthdate.isAfter(today),
                "Informe nascimento válido, sem data futura.");
        require(
                "sex",
                Set.of("FEMININO", "MASCULINO", "OUTRO", "NAO_INFORMADO").contains(p.sex),
                "Selecione o sexo ou não informado.");
        text("nationality", p.nationality, 80);
        require(
                "nationality",
                java.util.Arrays.asList(Locale.getISOCountries()).contains(p.nationality),
                "Use o código de duas letras do país, por exemplo BR ou AR.");
        phone("phone", p.phone);
        text("address", p.address, 500);
        text("country", p.country, 80);
        require(
                "email",
                p.email != null
                        && p.email.length() <= 254
                        && (p.email.isBlank() || p.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")),
                "Informe e-mail válido ou deixe vazio.");
        boolean minor = p.birthdate.plusYears(18).isAfter(today);
        if (p.cpf.isBlank()) {
            require("cpf", minor || !"BR".equalsIgnoreCase(p.nationality), "Adulto brasileiro precisa de CPF.");
            validateAlternative(p);
        } else {
            require("cpf", validCpf(p.cpf), "CPF inválido.");
            p.cpf = digits(p.cpf);
            if (!p.alternativeNumber.isBlank()) validateAlternative(p);
        }
        for (Patient.Guardian guardian : p.guardians) {
            text("guardianName", guardian.name(), 160);
            text("guardianRelationship", guardian.relationship(), 80);
            text("guardianDocument", guardian.document(), 100);
            phone("guardianPhone", guardian.phone());
            require(
                    "guardianStartsOn",
                    guardian.startsOn() != null
                            && (guardian.endsOn() == null || !guardian.endsOn().isBefore(guardian.startsOn())),
                    "Confira o período do responsável.");
        }
        require(
                "guardianName",
                !minor || p.guardians.stream().anyMatch(g -> g.currentOn(today)),
                "Menor precisa de responsável legal atual.");
    }

    private static void validateAlternative(Patient p) {
        text("alternativeType", p.alternativeType, 80);
        text("alternativeNumber", p.alternativeNumber, 100);
        text("alternativeCountry", p.alternativeCountry, 80);
        text("alternativeIssuer", p.alternativeIssuer, 100);
        text("alternativeReason", p.alternativeReason, 500);
    }
}
