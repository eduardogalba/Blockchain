package cc.blockchain;

public interface Blockchain {
    /**
    * Ordena la creacion de una cuenta cuyo saldo inicial es v.
    * @param idPrivado identidad privada de la cuenta
    * @param idPublico identidad publica de la cuenta
    * @param v saldo inicial
    * @throws IllegalArgumentException si v < 0, o si idPrivado o idPublico identifican
    * cuentas ya creadas.
    */
    void crear(String idPrivado, String idPublico, int v);
    /**
    * Un orden de transferir un determinado valor v desde
    * una cuenta origen a otra cuenta destino.
    * @param idPrivado la identidad privada de la cuenta origen
    * @param idPublico la identidad publica de la cuenta destino
    * @param v valor a transferir
    * @throws IllegalArgumentException si v <= 0 o si idPrivado y idPublico identifica
    * la misma cuenta
    *
    */
    void transferir(String idPrivado, String idPublico, int v);
    /**
    * Una pregunta sobre el saldo de una cuenta
    * @param idPrivado la identidad privada de la cuenta
    * @return saldo disponible en la cuenta
    * @throws IllegalArgumentException si idPrivado no identifica una cuenta
    */
    int disponible(String idPrivado);
    /**
    * Un avisador establece una alerta para una cuenta. La operación
    * termina cuando el saldo de la cuenta c sube por enicima de m.
    * @param idPrivado la identidad privada de la cuenta
    * @param m saldo maximo
    * @throws IllegalArgumentException si m <= 0 o si idPrivado no identifica una cuenta
    */
    void alertarMax(String idPrivado, int m);
    }