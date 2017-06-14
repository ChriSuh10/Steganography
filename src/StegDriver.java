import java.io.IOException;

public class StegDriver {
	public static void main(String[] args) throws IOException {
		Steganography s = new Steganography();
		
		// Text
		long t1 = System.nanoTime();
		s.hideMessage("data/TESTINPUT.txt", "data/coutinho.png");
		s.unHideMessage("data/coutinho1.png", "data/newFile.txt");
		//s.getDifference("data/coutinho.png", "data/coutinho1.png");
		long diffText = System.nanoTime() - t1;
		System.out.println("Elapsed time for encoding and decoding text: " + diffText/1E9);
		
		// Image
		/*long t2 = System.nanoTime();
		s.hidePic("data/bluedevil.png", "data/coutinho.png");
		s.unHidePic("data/coutinho2.png", "data/newFile.png");
		//s.getDifference("data/coutinho.png", "data/coutinho2.png");
		long diffPic = System.nanoTime() - t2;
		System.out.println("Elapsed time for encoding and decoding picture: " + diffPic/1E9);*/
	}
}
