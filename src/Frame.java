

public class Frame {

	private static final String FLAG = "01111110";
	private char type;
	private char num;
	private String data;
	private String crc;
	
	// Constructeur utilisé par l'entité qui créer un trame à envoyer.
	public Frame(char type, char num, String data) {
		
		this.type = type;
		this.num = num; 
		this.data = Controller.stringToBinary(data);
		this.crc = this.computeCRC();
		
	}
	
	// Constructeur utilisé par l'entité qui créer une trame reçue.
	public Frame(String frame) {

		this.type = Controller.binaryToString(frame.substring(0, 8)).charAt(0);
		this.num = Controller.binaryToString(frame.substring(8, 16)).charAt(0);
		this.data = frame.substring(16, frame.length() - 16);
		this.crc = frame.substring(frame.length() - 16);
		
	}
	
	public String getData() {
		return Controller.binaryToString(this.data);
	}
	
	public char getNum() {
		return this.num;
	}
	
	public int getNumValue() {
		return Character.getNumericValue(this.num);
	}
	
	public char getType() {
		return this.type;
	}
	
	public String getCRC() {
		return this.crc;
	}
	
	// Fonction qui nous permet d'obtenir la validité de la trame reçue.
	public boolean isValid() {
		
		String type = Controller.stringToBinary(Character.toString(this.type));
		String num = Controller.stringToBinary(Character.toString(this.num));
		String frame = type + num + this.data;
		
		return Controller.isCrcValid(frame, this.crc);
	}
	
	// Fonction qui nous permet d'obtenir les données binaires après bitStuffing de la Trame à envoyer.
	public String getBinaryFrame() {	
		
		String binaryFrame = Controller.stringToBinary(Character.toString(this.type))
				+ Controller.stringToBinary(Character.toString(this.num))
				+ this.data 
				+ this.crc;
		
		binaryFrame = Controller.bitStuffing(binaryFrame);
		binaryFrame = FLAG + binaryFrame + FLAG;
		
		return binaryFrame;
	}
	
	// Fonction utilisé lors de la création de la Trame qui va être envoyé afin de calculer son CRC
	private String computeCRC() {
		String type = Controller.stringToBinary(Character.toString(this.type));
		String num = Controller.stringToBinary(Character.toString(this.num));
		String frame = type + num + this.data;
		
		return Controller.calculCRC(frame);
	}	
	
	// Fonction qui permet l'affichage de la Trame.
	@Override
	public String toString() {
		String data = new String();
		if(this.data.length() > 0) data = Controller.binaryToString(this.data);
		else data = "Vide";
		String frame = "Type = " + this.getType() + " | Numéro = " + this.getNum() 
				+ " | Données = " + data.replaceAll("\n", " ") + " | CRC = " + this.crc;
		return frame;
	}
	
	
}
