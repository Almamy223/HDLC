
public class Test {
	
	private static String PORT;
	
	
	public static void main(String[] args) throws Exception {
		
		if(args.length > 0) {
			PORT = args[0];
			
			for(int i = 1; i < args.length; i++) {
				String file = args[i];
				
				System.out.println("Appuyez sur entrer pour simuler le prochain envoi");
				System.in.read();
				
				String[] arguments = new String[] {"localhost", PORT, file, "0", "0", "0"};
				Sender.main(arguments);
				
				System.out.println("Appuyez sur entrer pour simuler le prochain envoi");
				System.in.read();
				
				String[] arguments2 = new String[] {"localhost", PORT, file, "0", "0.05", "0.10"};
				Sender.main(arguments2);
				
				
				System.out.println("Appuyez sur entrer pour simuler le prochain envoi");
				System.in.read();
				
				String[] arguments3 = new String[] {"localhost", PORT, file, "0", "0.1", "0.2"};
				Sender.main(arguments3);
			}
			
		} else {
			System.out.println("Erreur, arguments manquants : <NUMERO_PORT> <FILE_PATH>* <FILE_PATH>* <FILE_PATH>*");
		}
		
	}

}
