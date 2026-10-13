package com.advocacia_microservice;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureTests {
    private static final Path ROOT = Path.of("src/main/java/com/advocacia_microservice");
    private List<Path> fontes(Path dir) throws IOException {
        try (var files = Files.walk(dir)) { return files.filter(p -> p.toString().endsWith(".java")).toList(); }
    }
    @Test void controllersNaoDeclaramRecordsNemAcessamPersistencia() throws IOException {
        for (var file : fontes(ROOT)) {
            if (!file.getFileName().toString().endsWith("Controller.java")) continue;
            String source = Files.readString(file);
            assertFalse(Pattern.compile("\\brecord\\s").matcher(source).find(), file.toString());
            assertFalse(Pattern.compile("import .*?(persistence|repository|JdbcTemplate|JpaRepository).*;").matcher(source).find(), file.toString());
        }
    }
    @Test void aplicacaoDeEmpresasEPlataformaDependeDeContratos() throws IOException {
        for (String module : List.of("escritorio", "plataforma", "agenda")) {
            for (var file : fontes(ROOT.resolve(module).resolve("application"))) {
                assertFalse(Pattern.compile("import .*?(infrastructure|\\.api\\.|JdbcTemplate|jakarta.persistence|org.springframework.data).*;")
                        .matcher(Files.readString(file)).find(), file.toString());
            }
        }
    }
    @Test void dominioDeEmpresasEPlataformaNaoDependeDeFrameworks() throws IOException {
        for (String module : List.of("escritorio", "plataforma", "agenda")) {
            for (var file : fontes(ROOT.resolve(module).resolve("domain"))) {
                assertFalse(Pattern.compile("import (org.springframework|jakarta|.*\\.api\\.|.*\\.infrastructure\\.)")
                        .matcher(Files.readString(file)).find(), file.toString());
            }
        }
    }
    @Test void entidadesERepositoriesJpaTemPacotesSeparados() throws IOException {
        for (var file : fontes(ROOT)) {
            String source = Files.readString(file);
            if (source.contains("@Entity")) assertTrue(source.contains(".infrastructure.persistence.entity;"), file.toString());
            if (source.contains("extends JpaRepository")) assertTrue(source.contains(".infrastructure.persistence.repository;"), file.toString());
        }
    }
}
