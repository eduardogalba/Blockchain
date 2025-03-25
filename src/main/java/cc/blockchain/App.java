package cc.blockchain;


public class App {

    public static class Creador extends Thread {
        Blockchain blockchain;

        public Creador (Blockchain blockchain) {
            this.blockchain = blockchain;
        }

        public void run () {
            blockchain.crear("priv000", "pub000", 10);
            blockchain.crear("priv001", "pub001", 0);
            blockchain.crear("priv002", "pub002", 0);   
        }
    }

    public static class Transferidor extends Thread {
        Blockchain blockchain;
        int id;

        public Transferidor (Blockchain blockchain, int id) {
            this.blockchain = blockchain;
            this.id = id;
        }
        
        public void run () {

            try {
                switch (id) {
                    case 0:
                        blockchain.transferir("priv002", "pub001", 11);
                        System.out.println("Transfiere 11 de priv002 a priv001");
                        break;
                    case 1:
                        blockchain.transferir("priv002", "pub001", 1);
                        System.out.println("Transfiere 1 de priv002 a priv001");
                        break;
                    case 2:
                        blockchain.transferir("priv001", "pub002", 10);
                        System.out.println("Transfiere 10 de priv001 a priv002");
                        break;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error en la transferencia " + id);
            }
            
        }
    }

    public static class Alertador extends Thread {
        Blockchain blockchain;

        public Alertador (Blockchain blockchain) {
            this.blockchain = blockchain;
        }

        public void run () {
            for (int i = 0; i < 5; i++) {
                blockchain.alertarMax(("priv00" + i), 10);
                System.out.println("Alertando a priv00" + i + " si supera 10");
            }
        }
    }

    public static void main(String[] args) {
        Blockchain blockchain = new BlockchainMonitor();
        Creador creador = new Creador(blockchain);
        Transferidor [] transferidors = new Transferidor[3] ;
        Alertador alertador = new Alertador(blockchain);
        try {
            blockchain.crear("priv001", "pub001", 10);
            blockchain.crear("priv002", "pub002", 0);
            blockchain.crear("priv003", "pub003", 0); 
            for (int i = 0; i < transferidors.length; i++) {
                transferidors[i] = new Transferidor(blockchain, i);
                transferidors[i].start();
                transferidors[i].sleep(1000);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error en la transferencia");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }   
    }
}
