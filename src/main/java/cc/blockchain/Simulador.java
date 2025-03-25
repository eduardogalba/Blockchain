package cc.blockchain;

import java.util.Random;


import java.util.List;
import java.util.ArrayList;

import es.upm.babel.cclib.ConcIO;


public class Simulador {

  static final int MAX_CUENTAS = 10;

  public static void main(String[] args) {
    Blockchain blockchain;
    String[] privCuentas = privCuentas();
    String[] pubCuentas = pubCuentas();
    Generador g = new Generador();

    // Por defecto el simulador usa la implementacion programado con monitores
    // Para comprobar la implementacion usando CSP cambia las dos lineas abajo.
    //blockchain = new BlockchainMonitor();
    blockchain = new BlockchainCSP();
    // Crea procesos
    new Cajero(blockchain,g).start();
    new Ordenante(blockchain,g).start();
    new Ordenante(blockchain,g).start();
    new Ordenante(blockchain,g).start();
    new Ordenante(blockchain,g).start();
    new Ordenante(blockchain, g).start();
    new Ordenante(blockchain, g).start();
    new Ordenante(blockchain, g).start();
    new Ordenante(blockchain, g).start();

    for (int i=0; i<MAX_CUENTAS; i++)
      new Consultor(blockchain,privCuentas[i],g).start();
    for (int i=0; i<MAX_CUENTAS; i++)
      new Avisador(blockchain,privCuentas[i],g).start();
    
  }

  /**
   * Devuele un array con todos los identidades privadas de cuenta posibles.
   */
  public static String[] privCuentas()
  {
    String cs[] = new String[Simulador.MAX_CUENTAS];
    for (int i = 0; i < Simulador.MAX_CUENTAS; i++)
      cs[i] = String.format("priv_%03d", i);
    return cs;
  }

  /**
   * Devuele un array con todos los identidades publicas de cuenta posibles.
   */
  public static String[] pubCuentas()
  {
    String cs[] = new String[Simulador.MAX_CUENTAS];
    for (int i = 0; i < Simulador.MAX_CUENTAS; i++)
      cs[i] = String.format("pub_%03d", i);
    return cs;
  }
}


/**
 * Las instancias de Generador son capaces de generar números de
 * cuenta y valores para ingresar o transferir así como enteros entre
 * 0 y un entero máximo (Nota: la implementación no es reentrante, lo
 * mejor es que los procesos no compartan el mismo generador).
 */
class Generador {
  private final int MAX_VALOR = 10;
  private Random r = new Random();
  private String privCuentas[];
  private String pubCuentas[];
  private List<Integer> creados;
  private List<Integer> noCreados;

  /**
   * Crea un nuevo generador de cuentas y valores.
   */
  public Generador()
  {
    privCuentas = Simulador.privCuentas();
    pubCuentas = Simulador.pubCuentas();
    creados = new ArrayList<>();
    noCreados = new ArrayList<>();
    for (int i=0; i<Simulador.MAX_CUENTAS; i++)
      noCreados.add(i);
  }

  
  /**
   * Con probabilidad alta devuelve un indice de una cuenta creada (si hay).
   */
  private synchronized int cuentaIndiceExists()
  {
    if (!creados.isEmpty()) {
      int choice = r.nextInt(4);
      if (choice > 0) return creados.get(r.nextInt(creados.size()));
      else return r.nextInt(Simulador.MAX_CUENTAS);
    } else return r.nextInt(Simulador.MAX_CUENTAS);
  }

  /**
   * Con probabilidad alta devuelve un indice de una cuenta no creada (si hay).
   */
  private synchronized int cuentaIndiceNoExists()
  {
    if (!noCreados.isEmpty()) {
      int choice = r.nextInt(4);
      if (choice > 0) return noCreados.get(r.nextInt(noCreados.size()));
      else return r.nextInt(Simulador.MAX_CUENTAS);
    } else return r.nextInt(Simulador.MAX_CUENTAS);
  }

  /**
   * Devuelve un array (de tamano 2, un par) de una identidad private y publica.
   */
  public synchronized String[] cuenta() {
    int i = cuentaIndiceNoExists();
    if (noCreados.contains(i)) {
      // Asumimos que la creacion va a funcionar
      creados.add(i);
      noCreados.remove(Integer.valueOf(i));
    }
    return new String[] { privCuentas[i], pubCuentas[i] };
  }

  /**
   * Devuele una identidad privada de cuenta aleatorio.
   */
  public synchronized String privCuenta()
  {
    return privCuentas[cuentaIndiceExists()];
  }

  /**
   * Devuele una identidad privada de cuenta aleatorio.
   */
  public synchronized String pubCuenta()
  {
    return pubCuentas[cuentaIndiceExists()];
  }
  
  /**
   * Devuele una valor aleatorio que se puede usar para ingresar o
   * hacer una transferencia.
   */
  public int valor()
  {
    return 1 + r.nextInt(MAX_VALOR);
  }

  /**
   * Devuele un entero positivo entre 1 y max.
   */
  public int positivo(int max)
  {
    return 1 + r.nextInt(max);
  }
}


class Cajero extends Thread {
  private Blockchain b;
  private Generador g;

  public Cajero(Blockchain b, Generador g) {
    this.b = b;
    this.g = g;
  }

  public void run() {
    while (true) {
      String[] cuenta = g.cuenta();
      int v = g.valor();
      try { b.crear(cuenta[0], cuenta[1], v);
        ConcIO.printfnl(
                        "Crear cuenta en <%s,%s>; disponible %d",
                        cuenta[0], cuenta[1], v);
      }
      catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Crear cuenta %s %s disponible %d lanzó la excepción IllegalArgumentException",
                        cuenta[0], cuenta[1], v);
      }
      try { sleep(g.positivo(10000)); } catch (InterruptedException exc) { }
    }
  }
}

class Ordenante extends Thread {
  private Blockchain b;
  private Generador g;

  public Ordenante(Blockchain b, Generador g) {
    this.b = b;
    this.g = g;
  }

  public void run() {
    String o, d;
    int v;
    while (true) {
      o = g.privCuenta();
      d = g.pubCuenta();
      v = g.valor();

      try { b.transferir(o, d, v);
        // Imprimir información sobre
        // la transferencia realizada
        ConcIO.printfnl("Transferencia de %s a %s: %d",
                        o, d, v);
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Transferencia de %s a %s: %d lanzó la excepción IllegalArgumentException",
                        o, d, v);
      }
      try { sleep(g.positivo(6000)); } catch (InterruptedException exc) { }
    }
  }
}

class Consultor extends Thread {
  private Blockchain b;
  private String c;
  private Generador g;

  public Consultor(Blockchain b, String c, Generador g) {
    this.b = b;
    this.c = c;
    this.g = g;
  }

  public void run() {
    int s;
    while (true) {

      try {
        s = b.disponible(c);
        // Imprime información sobre
        // el saldo disponible
        ConcIO.printfnl("Saldo en %s: %d",
                        c, s);
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Saldo en %s lanzó la excepción IllegalArgumentException",
                        c);
      }

      try { sleep(g.positivo(2000)); } catch (InterruptedException exc) { }
    }
  }
}

class Avisador extends Thread {
  private Blockchain b;
  private String c;
  private Generador g;

  public Avisador(Blockchain b, String c, Generador g) {
    this.b = b;
    this.c = c;
    this.g = g;
  }

  public void run() {
    int m;
    while (true) {
      m = g.valor();

      try {
        ConcIO.printfnl("Estableciendo alerta: %s por encima de %d",
                        c, m);
        b.alertarMax(c, m);
        // Comunicar la alerta sobre
        // saldo inferior a m
        ConcIO.printfnl("ALERTA: %s por encima de %d",
                        c, m);
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("ALERTA: %s por encima de %d lanzó la excepción IllegalArgumentException",
                        c, m);
      }

      try { sleep(g.positivo(2000)); } catch (InterruptedException exc) { }
    }
  }
}
