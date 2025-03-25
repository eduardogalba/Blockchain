package cc.blockchain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import es.upm.babel.cclib.Monitor;

public class BlockchainMonitor implements Blockchain {

    /* Declaracion del estado del recurso */
    private Map<String, Integer> cuentas;
    private Map<String, String> identidades;

    /*
     * Declaraciones para prevenir situaciones de carrera y sincronizacion de
     * procesos
     */
    private Monitor mutex;
    private Collection<Peticion> peticiones;

    /* 
    * Asegurar prioridad => Lista con las claves privadas, en el origen de las 
    * transferencias pendientes, ya procesadas
    */
    List<String> cuentasPendientes = new LinkedList<>();

    public BlockchainMonitor() {

        mutex = new Monitor();
        cuentas = new HashMap<>();
        identidades = new HashMap<>();
        peticiones = new LinkedList<>();
    }

    /* 
     * Indexacion por clientes => Almacenar la informacion de las llamadas de los procesos a los 
     * metodos del recurso compartido
     */

    public class Peticion {
        String priv;
        String pub;
        int cant;
        Monitor.Cond cond;
        boolean esTransferir;

        public Peticion(String idPriv, String idPub, int v) {
            this.esTransferir = true;
            this.priv = idPriv;
            this.pub = idPub;
            this.cant = v;
            cond = mutex.newCond();
        }

        public Peticion(String idPriv, int m) {
            this.esTransferir = false;
            this.priv = idPriv;
            this.cant = m;
            cond = mutex.newCond();
        }
    }

    /**
   * Ordena la creacion de una cuenta cuyo saldo inicial es v.
   * @param idPrivado identidad privada de la cuenta
   * @param idPublico identidad publica de la cuenta
   * @param v saldo inicial
   * @throws IllegalArgumentException si v < 0, o si idPrivado o idPublico identifican
   * cuentas ya creadas.
   */
    @Override
    public void crear(String idPrivado, String idPublico, int v) {

        /* Protocolo de entrada a SC */
        mutex.enter();

        /* Se cumple la PRE? */
        if (v < 0 || cuentas.containsKey(idPrivado) || identidades.containsKey(idPublico)) {
            mutex.leave();
            throw new IllegalArgumentException();
        }

        /* Seccion critica */
        cuentas.put(idPrivado, v);
        identidades.put(idPublico, idPrivado);

        /* Desbloqueo? */
        desbloqueo(); 

        /* Protocolo de salida de SC */
        mutex.leave();
    }


    /**
   * Un orden de transferir un determinado valor v desde
   * una cuenta origen a otra cuenta destino.
   * @param idPrivado la identidad privada de la cuenta origen
   * @param idPublico la identidad publica de la cuenta destino
   * @param v valor a transferir
   * @throws IllegalArgumentException si v <= 0 o si idPrivado y idPublico identifica 
   * la misma cuenta o si idPrivado y idPublico no identifican cuentas creadas
   *
   */
    @Override
    public void transferir(String idPrivado, String idPublico, int v) {

        /* Protocolo de entrada a SC */
        mutex.enter();
        
        /* Se cumple la PRE? */
        if (v <= 0 || !cuentas.containsKey(idPrivado) || !identidades.containsKey(idPublico)
                || identidades.get(idPublico).equals(idPrivado)) {
            mutex.leave();        
            throw new IllegalArgumentException();
        }
        
        /* Se cumple la CPRE? */
        if (cuentas.get(idPrivado) < v || cuentasPendientes.contains(idPrivado)){
            if (!cuentasPendientes.contains(idPrivado)) 
                cuentasPendientes.add(idPrivado);
            
            Peticion nueva = new Peticion (idPrivado, idPublico, v);
            peticiones.add(nueva);
            nueva.cond.await();
        }
        
        /* Seccion critica */
        cuentas.put(idPrivado, cuentas.get(idPrivado) - v);
        cuentas.put(identidades.get(idPublico), cuentas.get(identidades.get(idPublico)) + v);
        
        /* Desbloqueo? */
        desbloqueo();

        /* Protocolo de salida de SC */
        mutex.leave();
    }

    /**
   * Una pregunta sobre el saldo de una cuenta
   * @param idPrivado la identidad privada de la cuenta
   * @return saldo disponible en la cuenta
   * @throws IllegalArgumentException si idPrivado no identifica una cuenta
   */
    @Override
    public int disponible(String idPrivado) {

        /* Protocolo de entrada a SC */
        mutex.enter();

        /* Se cumple la PRE? */
        if (!cuentas.containsKey(idPrivado)) {
            mutex.leave();
            throw new IllegalArgumentException();
        }

        /* Seccion critica */
        int result = cuentas.get(idPrivado);

        /* Protocolo de salida de SC */
        mutex.leave();

        return result;
    }

    /**
   * Un avisador establece una alerta para una cuenta. La operación
   * termina cuando el saldo de la cuenta c sube por enicima de m.
   * @param idPrivado la identidad privada de la cuenta
   * @param m saldo maximo
   * @throws IllegalArgumentException si m < 0 o si idPrivado no identifica una cuenta
   */
    @Override
    public void alertarMax(String idPrivado, int m) {

        /* Protocolo de entrada a SC */
        mutex.enter();

        /* Se cumple la PRE? */
        if (m < 0 || !cuentas.containsKey(idPrivado)) {
            mutex.leave();
            throw new IllegalArgumentException();
        }

        /* Se cumple la CPRE? */
        if (cuentas.get(idPrivado) <= m) {
            Peticion nueva = new Peticion(idPrivado, m);
            peticiones.add(nueva);
            nueva.cond.await();
        }

        /* Desbloqueo? */
        desbloqueo(); 

        /* Protocolo de salida de SC */
        mutex.leave();
    }

    /*
     * Metodo que se encarga de desbloquear las peticiones aplazadas que se 
     * han realizado a los metodos del recurso compartido
     */

    private void desbloqueo() {

        /* Variables para recorrer el bucle de peticiones aplazadas */
        Iterator<Peticion> it = peticiones.iterator();
        boolean encontrado = false;
        String noRepeat = " ";
        while (it.hasNext() && !encontrado) {
            Peticion actual = it.next();

            /* Es una peticion de transferencia? Ya ha sido procesada? */
            if (actual.esTransferir && !noRepeat.equals(actual.priv)) {
                noRepeat = actual.priv;
                /* Guardamos la CPRE */
                encontrado = cuentas.get(actual.priv) >= actual.cant;

            } else if (!actual.esTransferir)
                /* Guardamos la CPRE */
                encontrado = cuentas.get(actual.priv) > actual.cant;

            /* Se cumple la CPRE? */
            if (encontrado) {
                /* Peticion aplazada procesada */
                it.remove();

                if (numTransferencias(actual.priv) < 1) 
                    cuentasPendientes.remove(actual.priv);

                /* Desbloqueamos la peticion */
                actual.cond.signal();
            }
        }
    }

    private int numTransferencias (String idPrivado) {
        int result = 0;
        for (Peticion p : peticiones) {
            if (p.esTransferir && p.priv.equals(idPrivado)) 
                result++;
        }
        return result;
    }
}
