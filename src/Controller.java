import java.util.Arrays;

public class Controller {
	
	
	// Fonction qui permet de parser le text en binaire.
	public static String stringToBinary(String data) {
		
	    byte[] bytes = data.getBytes();
	    StringBuilder binary = new StringBuilder();
	    for (byte b : bytes) {
	       int val = b;
	       for (int i = 0; i < 8; i++) {
	          binary.append((val & 128) == 0 ? 0 : 1);
	          val <<= 1;
	       }
	    }
	    
	    return binary.toString();    
	}
	
	// Fonction qui permet de parser une String binaire en text.
	public static String binaryToString(String bits) {
		
		StringBuilder sb = new StringBuilder(); 
		Arrays.stream(bits.split("(?<=\\G.{8})")).forEach(s -> sb.append((char) Integer.parseInt(s, 2)));
		
		return sb.toString(); 
	}
	
	// Fonction qui permet d'introduire le bitStuffing dans une trame à envoyer.
	public static String bitStuffing(String data) {
		
		int score = 0;
		for(int i = 0; i < data.length(); i++) {
			if(data.charAt(i) == '0') score = 0;
			else if(data.charAt(i) == '1' && score < 4) score++;
			else if(data.charAt(i) == '1' && score == 4) {
				data = data.substring(0, i+1) + '0' + data.substring(i+1);
				score = 0;
				i++;
			}
		}
		
		return data;
	}
	
	// Fonction qui permet d'enlever le bitStuffing d'une trame reçue.
	public static String reverseBitStuffing(String data) {
		
		try {
			int score = 0;
			for(int i = 0; i < data.length(); i++) {
				if(data.charAt(i) == '0') score = 0;
				else if(data.charAt(i) == '1' && score < 4) score++;
				else if(data.charAt(i) == '1' && score == 4) {
					data = data.substring(0, i+1) + data.substring(i+2);
					score = 0;
				}
			}
			
			return data;
		} catch (Exception e){
			return "";
		}
		
	}
	
	// Fonction qui permet de parser une string en Bytes.
	public static byte[] stringToBytes(String bitString) {
		
		int n = (int) Math.ceil(bitString.length()/8.0);
		byte[] bytes = new byte[n];
		for (int i = 0; i < n; i++) {
			if (bitString.length() > 8) {
				int b = Integer.parseInt(bitString.substring(0,8), 2);
				bytes[i] = (byte) b;
				bitString = bitString.substring(8,bitString.length());
			}
			else {
				int b = Integer.parseInt(bitString.substring(0,bitString.length()), 2);
				bytes[i] = (byte) b;
			}
		}
		return bytes;
	}
	
	// Fonction qui permet le calcul du CRC
	public static String calculCRC(String data) {
		
		byte[] buffer = stringToBytes(data);
	    int crc = 0xFFFF;
	    
	    for (int j = 0; j < buffer.length ; j++) {
	        crc = ((crc  >>> 8) | (crc  << 8) )& 0xffff;
	        crc ^= (buffer[j] & 0xff);
	        crc ^= ((crc & 0xff) >> 4);
	        crc ^= (crc << 12) & 0xffff;
	        crc ^= ((crc & 0xFF) << 5) & 0xffff;
	    }
	    crc &= 0xffff;
	    
	    return String.format("%16s", Integer.toBinaryString(crc)).replace(' ', '0');
	}
	
	// Fonction qui nous permet de vérifier la validité du CRC et donc la validité des données reçues.
	public static boolean isCrcValid (String data, String crc) {
		return crc.equals(calculCRC(data));
	}
	
	// Fonction qui nous permet d'extraire un Trame d'un message reçu.
	public static String getFrame(String message) {
		
		String frame = new String();
		String FLAG = "01111110";
		int start = -1;
		int end = -1;
		
		for(int i = 0; i + FLAG.length() < message.length(); i++) {
			if(message.substring(i, i + FLAG.length()).equals(FLAG))  {
				if(start == -1) start = i;
				else {
					end = i;
					break;
				}
			}
		}
		if(end == -1 && message.substring(message.length()-8).equals(FLAG)) end = message.length() - 8;
		
		if(start == -1 || end == - 1) return "";
		
		frame = message.substring(start + FLAG.length(), end);
		return Controller.reverseBitStuffing(frame);
	}
	
	// Fonction qui permet de corrompre la Trame
	public static String corruptFrame(String frame) {
		
		String corruptedFrame = new String();
		for(int i = 0; i < frame.length(); i++) {
			double random = Math.random();
			if(random < 0.05) {
				corruptedFrame += frame.charAt(i);
				if(random < 0.5)
					corruptedFrame += "1";
				else 
					corruptedFrame += "0";
			} else if(random > 0.1 && random < 0.15) {
				if(frame.charAt(i) == '1') corruptedFrame += "0";
				else 
					corruptedFrame += "1";
			} else {
				corruptedFrame += frame.charAt(i);
			}
		}
		return corruptedFrame;
	}

	
}
