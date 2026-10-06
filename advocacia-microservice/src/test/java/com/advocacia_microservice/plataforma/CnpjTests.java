package com.advocacia_microservice.plataforma;

import com.advocacia_microservice.plataforma.domain.Cnpj;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CnpjTests {
    @Test void normalizaNumericoEAlfanumericoComDigitosVerificadores() {
        assertEquals("11222333000181", Cnpj.normalizar("11.222.333/0001-81"));
        assertEquals("00000000E08G12", Cnpj.normalizar("00.000.000/e08g-12"));
    }
    @Test void rejeitaDocumentoInvalidoSemDescartarCaracteresIndevidos() {
        for (String valor : new String[]{"00000000000000", "11222333000182", "11_222333000181", "", "00000000E08G13"})
            assertThrows(IllegalArgumentException.class, () -> Cnpj.normalizar(valor));
    }
}
