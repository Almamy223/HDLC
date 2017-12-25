import java.io.*;
import java.net.*;
import java.util.*;


public class Sender {

	private static Socket SOCKET;
	private static String MACHINE_NAME;
	private static int PORT;
	private static String FILE_NAME;
	
	private static String MESSAGE;
	private static ArrayList<Frame> LIST_FRAMES;

	private static final int TIME_OUT = 3; // Temps en seconde du Time Out.
	private static final int WINDOW_SIZE = 7; // Taille de la fenêtre.
	private static final int FRAME_SIZE = 10; // Nombre de character maximum par trame.
	
	private static double ERROR_PROB = 0.07; // Probabilité de perte lors de l'envoi de la Trame.
	private static double ADD_SUB_FLIP_PROB = 0.15; // Probabilité que des bits soient ajoutées/supprimé ou modifié dans une Trame.
	
	
	private static PrintStream PS;
	private static InputStreamReader ISR;
	private static BufferedReader BR;
	

	public static void main(String[] args) throws Exception {
		
		MESSAGE = new String();
		LIST_FRAMES = new ArrayList<Frame>();
		
		// Initialisation de l'émetteur. 
		if(args.length < 4) {
			System.out.println("Erreur, arguments manquants : <Nom_Machine> <Numero_Port> <Nom_fichier> <0> <PROBA_ERREUR>* <PROBA_CORROMPRE>*");
		}
		else {
			try {
				MACHINE_NAME = args[0];
				PORT = Integer.parseInt(args[1]);
				FILE_NAME = args[2];
				
				
				if(args.length > 4) ERROR_PROB = Double.parseDouble(args[4]);
				if(args.length > 4) ADD_SUB_FLIP_PROB = Double.parseDouble(args[5]);
			} catch (Exception e) {
				System.out.println("Erreur, le numéro de port doit être un nombre.");
				System.exit(0);
			}
			getFrames();
			if(LIST_FRAMES.size() > 0)
				startConnexion();
		}
		
	}
	
	// Etablissement de connexion.
	private static void startConnexion() throws UnknownHostException, IOException {
		try {
			SOCKET = new Socket(MACHINE_NAME, PORT);
		} catch (Exception e) {
			System.out.println("Aucun récepteur pour ce port.");
			System.exit(0);
		}
		
		PS = new PrintStream(SOCKET.getOutputStream());
		ISR = new InputStreamReader(SOCKET.getInputStream());
		BR = new BufferedReader(ISR);
		
		for(int i = 0; i < 30; i++) {
			System.out.println();
		}
		
		boolean attemptConnexion = true;
		Frame CFrame = new Frame('C', '0', "");
		
		System.out.println("***********************************");
		System.out.println("*       DEMANDE DE CONNEXION      *");
		System.out.println("***********************************\n");
		
		// Demande de connexion
		while(attemptConnexion) {
			System.out.println("En attente de connexion...");
			PS.println(CFrame.getBinaryFrame());
			
			if(!timeOut()) {
				String message = Controller.getFrame(BR.readLine());
				if(message.length() >= 32) {
					Frame frame = new Frame(message);
					if(frame.isValid()) {
						/*
						 *  Puisque le récepteur est obligé d'accepter la connexion,
						 *  nous n'avons pas besoin de vérifier le type de la Trame reçue.
						 */
						System.out.println("Frame reçue => " + frame); 
						attemptConnexion = false;
					}
				}
			}
		}
		System.out.println("\n***********************************");
		System.out.println("*         CONNEXION ETABLIE       *");
		System.out.println("***********************************\n");
		// La connexion est établie
		run();
		
	}
	
	// Fonction qui permet l'envoie de toutes les Trames.
	private static void run() throws IOException {

		int nbFrameSent = 0;
		int index = 0;
		while(index < LIST_FRAMES.size()) {
			
			Frame frame = LIST_FRAMES.get(index);
			
			if(nbFrameSent == WINDOW_SIZE - 1 || index == LIST_FRAMES.size() -1) {
				sendFrame(frame);

				index -= getConfirmation(frame.getNum());
				
				nbFrameSent = 0;
			} else {
				
				sendFrame(frame);
				
				// Si l'on a reçu un message après l'envoie de la trame.
				if(BR.ready()) {
					String message = Controller.getFrame(BR.readLine());
					if(message.length() > 31) {
						Frame frameReceived = new Frame(message);
						
						if(frameReceived.isValid()) {
							int numFrame = frameReceived.getNumValue();
							index -= ((((frame.getNumValue() - numFrame) % 8) + 8) % 8);
							System.out.println("\n********************************************************");
							System.out.println("Trame reçue (REJ"+ numFrame +") => " + frameReceived);
							System.out.println("Retransmission à partir de la trame " + numFrame);
							System.out.println("********************************************************\n");
							nbFrameSent = 0;
						}
					}
				} else {
					nbFrameSent++;
					index++;
				}
			}
		}
		
		endConnexion();
	}
	
	// Fin de connexion.
	private static void endConnexion() {
		Frame frame = new Frame('F', '0', "");
		PS.println(frame.getBinaryFrame());
		System.out.println("\n********************************************************");
		System.out.println("                      FIN DE CONNEXION                  ");
		System.out.println("********************************************************\n");
		
	}
	
	// Fonction qui s'assure de récuperer les confirmations de chaque fenêtre.
	private static int getConfirmation(char num) throws IOException {
		boolean attemptConfirmation = true;
		int numFrame = -1;
		
		while(attemptConfirmation) {
			System.out.println("En attente de la confirmation..");
			Frame pFrame = new Frame('P', num, "");
			PS.println(pFrame.getBinaryFrame());
			
			if(!timeOut()) {
				String message = Controller.getFrame(BR.readLine());
				if(message.length() >= 32) {
					Frame frame = new Frame(message);
					
					if(frame.isValid()) {
						int number = Character.getNumericValue(num);
						switch(frame.getType()) {
							case 'A':
								if(number == frame.getNumValue()) {
									numFrame = -1;
									System.out.println("\n********************************************************");
									System.out.println("Trame reçue (RR"+ frame.getNumValue() +") => " + frame);
									System.out.println("********************************************************\n");
								}
								else {
									numFrame = ((((number - frame.getNumValue()) % 8) + 8) % 8);
									System.out.println("\n********************************************************");
									System.out.println("Trame reçue (RR"+ numFrame +") => " + frame);
									System.out.println("********************************************************\n");
								}
								break;
							case 'R':
								numFrame = ((((number - frame.getNumValue()) % 8) + 8) % 8);
								System.out.println("\n********************************************************");
								System.out.println("Trame reçue (REJ"+ frame.getNumValue() +") => " + frame);
								System.out.println("Retransmission à partir de la trame " + frame.getNumValue());
								System.out.println("********************************************************\n");
								break;
						}

						attemptConfirmation = false;
					}
				}
			}
		}
		return numFrame;
	}
	
	// Foncrtion qui permet de gérer le TimeOut
	private static boolean timeOut() throws IOException {
		long start = System.currentTimeMillis() + TIME_OUT * 1000;
		
		while(!BR.ready()) {
			if (System.currentTimeMillis() > start) {
				return true;
			}
		}
		return false;
	}

	
	// Récupération des données du fichier passé en paramètre.
	private static void getMessage(){
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(FILE_NAME));
			StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        line = br.readLine();
		        if(line !=null) sb.append(System.lineSeparator());
		    }
		    MESSAGE = sb.toString();
		    
		}
		catch (FileNotFoundException e) {
			System.out.println("Erreur, le fichier entré est introuvable");
		}
		catch (IOException e) {
			System.out.println("Erreur lors de la lecture du fichier.");
		}
	}
	
	// Partitionne le fichier en trames.
	private static void getFrames() {
		getMessage();
		
		int i = 0;
	    int frameNumber = 0;
		    
		while(i + FRAME_SIZE < MESSAGE.length()) {
			int end = i + FRAME_SIZE;
			String data = MESSAGE.substring(i, end);
			Frame frame = new Frame('I', Integer.toString(frameNumber).charAt(0), data);
			LIST_FRAMES.add(frame);
			i = end;
			frameNumber = (frameNumber + 1) % 8;
		}
		
		if(MESSAGE.length() > 0) {
			String data = MESSAGE.substring(i);
			Frame frame = new Frame('I', Integer.toString(frameNumber).charAt(0), data);
			LIST_FRAMES.add(frame);
		}
	}
	
	// Fonction qui permet d'envoyer les frames en introduisant la probabilité de perte et d'erreur.
	private static void sendFrame(Frame frame) {
		if(Math.random() < ERROR_PROB) {
			System.out.println("Erreur de Transmission de la Trame " + frame.getNum());
		} else if(Math.random() < ADD_SUB_FLIP_PROB) {
			PS.println(Controller.corruptFrame(frame.getBinaryFrame()));
			System.out.println("Envoi de Trame => " + frame);
		} else {
			PS.println(frame.getBinaryFrame());
			System.out.println("Envoi de Trame => " + frame);
		}
	}

}
