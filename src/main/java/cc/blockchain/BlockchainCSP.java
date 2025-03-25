package cc.blockchain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.ProcessManager;

public class BlockchainCSP implements Blockchain, CSProcess {

    /* Canales de los clientes */
    private final Any2OneChannel chCrear;
    private final Any2OneChannel chTransferir;
    private final Any2OneChannel chDisponible;
    private final Any2OneChannel chAlertar;

    /* Identificadores para el servidor */
    private static final int CREAR = 0;
    private static final int TRANSFERIR = 1;
    private static final int DISPONIBLE = 2;
    private static final int ALERTAR = 3;

    /* Comunicacion con los clientes */
    private static final int PRE_KO = -1;
    private static final int PRE_OK = 0;

    /* Estado del recurso */
    private Map<String, Integer> cuentas;
    private Map<String, String> identidades;

    /* Almacenes para las peticiones aplazadas */
    private Collection<Peticion> petTransferirs;
    private Collection<Peticion> petAlertars;

    private List<String> cuentasPendientes;

    public BlockchainCSP() {
        chCrear = Channel.any2one();
        chTransferir = Channel.any2one();
        chDisponible = Channel.any2one();
        chAlertar = Channel.any2one();
        new ProcessManager(this).start();
    }

    @Override
    public void crear(String idPrivado, String idPublico, int v) {
        Peticion nueva = new Peticion(idPrivado, idPublico, v);
        chCrear.out().write(nueva);
        if ((int) nueva.chResp.in().read() == PRE_KO)
            throw new IllegalArgumentException();

    }

    @Override
    public void transferir(String idPrivado, String idPublico, int v) {
        Peticion nueva = new Peticion(idPrivado, idPublico, v);
        chTransferir.out().write(nueva);
        if ((int) nueva.chResp.in().read() == PRE_KO)
            throw new IllegalArgumentException();
    }

    @Override
    public int disponible(String idPrivado) {
        Peticion nueva = new Peticion(idPrivado);
        chDisponible.out().write(nueva);
        int rcod = (int) nueva.chResp.in().read();
        if (rcod == PRE_KO)
            throw new IllegalArgumentException();
        return rcod;
    }

    @Override
    public void alertarMax(String idPrivado, int m) {
        Peticion nueva = new Peticion(idPrivado, m);
        chAlertar.out().write(nueva);
        if ((int) nueva.chResp.in().read() == PRE_KO)
            throw new IllegalArgumentException();
    }

    /*
     * Esta clase se encarga de encapsular las peticiones de los clientes
     */

    @SuppressWarnings("squid:S1104")
    public class Peticion {
        public String priv;
        public String pub;
        public int value;
        public One2OneChannel chResp;

        public Peticion(String idPriv, String idPub, int v) {
            this.priv = idPriv;
            this.pub = idPub;
            this.value = v;
            this.chResp = Channel.one2one();
        }

        public Peticion(String idPriv, int m) {
            this.priv = idPriv;
            this.value = m;
            this.chResp = Channel.one2one();
        }

        public Peticion(String idPriv) {
            this.priv = idPriv;
            this.chResp = Channel.one2one();
        }
    }

    private void ejecutarCrear(Peticion p) {
        if (p.value < 0 || cuentas.containsKey(p.priv) || identidades.containsKey(p.pub))
            p.chResp.out().write(PRE_KO);
        else {
            cuentas.put(p.priv, p.value);
            identidades.put(p.pub, p.priv);
            p.chResp.out().write(PRE_OK);
        }
    }

    private boolean ejecutarTransferir(Peticion p) {
        if (p.value <= 0 || !cuentas.containsKey(p.priv) || !identidades.containsKey(p.pub)
                || identidades.get(p.pub).equals(p.priv)) {
            p.chResp.out().write(PRE_KO);
            return true;
        } else if (cuentas.get(p.priv) >= p.value) {
            cuentas.put(p.priv, cuentas.get(p.priv) - p.value);
            cuentas.put(identidades.get(p.pub), cuentas.get(identidades.get(p.pub)) + p.value);
            p.chResp.out().write(PRE_OK);
            return true;
        } else {
            if (!cuentasPendientes.contains(p.priv))
                cuentasPendientes.add(p.priv);

            return false;
        }
            
    }

    private void ejecutarDisponible(Peticion p) {
        if (!cuentas.containsKey(p.priv))
            p.chResp.out().write(PRE_KO);
        else
            p.chResp.out().write(cuentas.get(p.priv));
    }

    private boolean ejecutarAlertar(Peticion p) {
        if (p.value < 0 || !cuentas.containsKey(p.priv)) {
            p.chResp.out().write(PRE_KO);
            return true;
        } else if (cuentas.get(p.priv) > p.value) {
            p.chResp.out().write(PRE_OK);
            return true;
        } else
            return false;
    }

    private boolean revisarPeticionesTransferir() {
        Iterator<Peticion> it = petTransferirs.iterator();
        String noRepeat = " ";
        while (it.hasNext()) {
            Peticion actual = it.next();

            if (!noRepeat.equals(actual.priv)) {
                noRepeat = actual.priv;

                if (ejecutarTransferir(actual)) {
                    it.remove();
                    
                    if (numTransferencias(actual.priv) < 1) 
                        cuentasPendientes.remove(actual.priv);
                    
                    return true;
                }
            }
        }

        return false;
    }

    private boolean revisarPeticionesAlertar () {
        Iterator<Peticion> it = petAlertars.iterator();

            while (it.hasNext()) {
                Peticion actual = it.next();
                if (ejecutarAlertar(actual)) {
                    it.remove();
                    return true;
                }
            }
        return false;
    }

    private void revisarPeticiones() {
        boolean cambio;
        do {
            cambio = revisarPeticionesTransferir() || revisarPeticionesAlertar();
        } while (cambio);
    }

    private int numTransferencias (String idPrivado) {
        int result = 0;
        for (Peticion p : petTransferirs) {
            if (p.priv.equals(idPrivado)) 
                result++;
        }
        return result;
    }

    @Override
    @SuppressWarnings({ "squid:S2189" })
    public void run() {
        /* Estado incial del recurso */
        cuentas = new HashMap<>();
        identidades = new HashMap<>();

        cuentasPendientes = new LinkedList<>();

        Guard[] entradas = {
                chCrear.in(),
                chTransferir.in(),
                chDisponible.in(),
                chAlertar.in()
        };

        Alternative servicios = new Alternative(entradas);

        petTransferirs = new LinkedList<>();
        petAlertars = new LinkedList<>();
        while (true) {
            Peticion p;
            switch (servicios.fairSelect()) {
                case CREAR:
                    p = (Peticion) chCrear.in().read();
                    ejecutarCrear(p);
                    revisarPeticiones();
                    break;
                case TRANSFERIR:
                    p = (Peticion) chTransferir.in().read();
                    if (cuentasPendientes.contains(p.priv))
                        petTransferirs.add(p);
                    else if (!ejecutarTransferir(p))
                        petTransferirs.add(p);
                    
                    revisarPeticiones();
                    break;
                case DISPONIBLE:
                    p = (Peticion) chDisponible.in().read();
                    ejecutarDisponible(p);
                    break;
                case ALERTAR:
                    p = (Peticion) chAlertar.in().read();
                    if (!ejecutarAlertar(p))
                        petAlertars.add(p);
                    revisarPeticiones();
                    break;
                default:
                    break;
            }
        }

    }

}
