package cc.blockchain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BlockchainTest {
    BlockchainMonitor blockchain;

    static Stream<Arguments> provideWrightParameters () {
        return Stream.of(Arguments.of("priv000", "pub000", 2),
        Arguments.of("priv001", "pub001", 1),
        Arguments.of("priv002", "pub002", 3),
        Arguments.of("priv003", "pub003", 5));
    }

    @BeforeEach
    void setUp () {
        blockchain = new BlockchainMonitor();
    }


    @ParameterizedTest
    @MethodSource ("provideWrightParameters")
    void crearCuentaCorrectamente (String idPriv, String idPub, int v) {
        assertDoesNotThrow(() -> blockchain.crear(idPriv, idPub, v));
    }

    @ParameterizedTest
    @MethodSource ("provideWrightParameters")
    void crearCuentaDoble (String idPriv, String idPub, int v) {
        assertDoesNotThrow(() -> blockchain.crear(idPriv, idPub, v));
        assertThrows(IllegalArgumentException.class, () -> blockchain.crear(idPriv, idPub, v));
    }

    @Test
    void crearCuentaSaldoIncorrecto () {
        assertThrows(IllegalArgumentException.class, () -> blockchain.crear("idPriv", "idPub", -1));
    }

    @Test
    void transferirAlaMismaPersona1 () {
        assertDoesNotThrow(() -> blockchain.crear("priv001", "pub001", 0));
        assertThrows(IllegalArgumentException.class, () -> blockchain.transferir("priv001", "pub001", 1));
    }

}
