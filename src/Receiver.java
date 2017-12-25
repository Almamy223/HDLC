import java.io.*;
import java.net.*;

public class Receiver {

	private static ServerSocket RECEIVER;
	private static Socket SOCKET;
	private static int PORT;

	private static String MESSAGE = new String();
	private static int NEXT_NUM = 0;
	
	private static final int TIME_OUT = 3; // Temps en seconde du Time Out.
	private static PrintStream PS;
	
	private static Frame REJ_FRAME;
	private static boolean REJ_SENT = false;
	private static long TIME_OUT_REJ;
	
	private static double ERROR_PROB = 0.05;

	
	public static void main(String[] args) throws Exception {
		
		// Initialisation du récepteur.
		if(args.length < 1) {
			System.out.println("Erreur, arguments manquants : <Numero_Port> <PROBA_ERREUR>*");
		} else {
			try {
				PORT = Integer.parseInt(args[0]);	
				if(args.length > 1) ERROR_PROB = Double.parseDouble(args[1]);
			} catch (Exception e) {
				System.out.println("Erreur, le numéro de port doit être un nombre");
				System.exit(0);
			}
			run();
		}
	}
	
	
	private static void run() throws Exception {
		
		// On initialise le récepteur avec son numéro de port.
		RECEIVER = new ServerSocket(PORT);
		
		while(true) {
			for(int i = 0; i < 30; i++) {
				System.out.println();
			}
			System.out.println("\n********************************************************");
			System.out.println("*                EN ATTENTE DE CONNEXION               *");
			System.out.println("********************************************************\n");
			SOCKET = RECEIVER.accept();
			
			PS = new PrintStream(SOCKET.getOutputStream());
			getFrame();
		}	
		
	}
	
	
	// Fonction qui permet de traiter chaque Trame reçue.
	private static void getFrame() throws IOException {
		InputStreamReader ISR = new InputStreamReader(SOCKET.getInputStream());
		BufferedReader BR = new BufferedReader(ISR); 

		// On lit le message reçu.
		String message;
		boolean cond = true;
		while(cond) {
			// On attend qu'une trame soit reçue.
			while(!BR.ready());
			
			message = Controller.getFrame(BR.readLine());
			if(message.length() >= 32) {
				Frame frame = new Frame(message);
				
				if(frame.isValid()) {

					switch(frame.getType()) {
						case 'C': 
							System.out.println("\n********************************************************");
							System.out.println("Trame de demande de connexion => " + frame);
							sendRRC();
							break;
						case 'I':
							System.out.println("Trame reçue => " + frame);
							if(frame.getNumValue() == NEXT_NUM) {
								REJ_SENT = false;
								NEXT_NUM = (NEXT_NUM + 1) % 8;
								MESSAGE += frame.getData();
							} else {
								sendREJ();
							}
							break;
						case 'P':
							System.out.println("\nTrame de demande de confirmation reçue => " + frame);
							int nb = (((NEXT_NUM - 1) % 8) + 8) % 8;
							if(nb == frame.getNumValue())
								sendRR();
							else 
								sendREJ();
							break;
						case 'F':
							System.out.println("\n********************************************************");
							System.out.println("Trame de fin de connexion reçue => " + frame);
							endConnexion();
							cond = false;
							break;
					}
				} else {
					System.out.println("Trame reçue corrompue.");
					sendREJ();
				}
			}
			else {
				System.out.println("Trame reçue invalide => Destruction de celle-ci.");
				if(!REJ_SENT) {
					sendREJ();
				}
			}
		}
	}
	
	// Fin de connexion
	private static void endConnexion() {
		
		System.out.println("Fin de connexion.");
		System.out.println("********************************************************\n");
		
		System.out.println("Message reçu : \n");
		System.out.println(MESSAGE);
		System.out.println("************************************************************************************************\n\n");
		
		// Re initialisation des paramètres.
		MESSAGE = new String();
		NEXT_NUM = 0;
	}
	
	// Fonction qui permet d'envoyer les RR.
	private static void sendRR() {
		
		int nb = (((NEXT_NUM - 1) % 8) + 8) % 8;
		char num = Integer.toString(nb).charAt(0);
		Frame RR = new Frame('A', num, "");
		if(Math.random() < ERROR_PROB) {
			System.out.println("Erreur de Transmission de la Trame " + RR.getNum());
		} else {
			PS.println(RR.getBinaryFrame()); 
			System.out.println("Envoi de la Trame => " + RR);
			System.out.println("********************************************************\n");
		}
		
	}
	
	// Fonction qui permet d'accepter la connexion.
	private static void sendRRC() {

		Frame RR = new Frame('A', '0', "");
		if(Math.random() < ERROR_PROB) {
			System.out.println("Erreur lors de l'acceptation de connexion.");
			System.out.println("********************************************************\n");
		} else {
			PS.println(RR.getBinaryFrame());
			System.out.println("Envoi de la confirmation de connexion.");
			System.out.println("********************************************************\n");
		}
		
		
	}
	
	
	// Fonction qui permet d'envoyer un REJ, dans le cas ou un REJ à déjà été envoyé, on attends d'avoir une réponse à celui-ci
	private static void sendREJ() {
		
		char num = Integer.toString(NEXT_NUM).charAt(0);
		if(!REJ_SENT) {
			REJ_FRAME = new Frame('R', num, "");
			REJ_SENT = true;
			System.out.println("\n********************************************************");
			System.out.println("Demande de retransmission");
			System.out.println("Envoi de la Trame => " + REJ_FRAME);
			System.out.println("********************************************************\n");
			PS.flush();
			PS.println(REJ_FRAME.getBinaryFrame());
			TIME_OUT_REJ = System.currentTimeMillis() + TIME_OUT * 1000;
		} else if(REJ_SENT && System.currentTimeMillis() > TIME_OUT_REJ) {
			System.out.println("\n********************************************************");
			System.out.println("TIME_OUT Retransimission du REJ => " + REJ_FRAME);
			System.out.println("********************************************************\n");
			PS.flush();
			PS.println(REJ_FRAME.getBinaryFrame());
			TIME_OUT_REJ = System.currentTimeMillis() + TIME_OUT * 1000;
		} else {
			System.out.println("\n********************************************************");
			System.out.println("La trame reçue ne correspond pas à la trame attendue.");
			System.out.println("Une demande de retransmission à déja été envoyé.");
			System.out.println("********************************************************\n");
		}
		
	}


}
