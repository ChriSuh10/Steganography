import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Steganography {
	private static final int HIDE_BITS = 2;  //Number of least significant bits to use
	private static final int INT_BITS = 32;  //Number if bits in an int
	
	/**
	 * Hides message within HIDE_BITS least significant bits of the file directed by imFile
	 * @param message       The message to be hidden, as a BitInputStream
	 * @param imFile	    The image file (must be supported by ImageIO) to hide the message in
	 * @throws IOException
	 */
	public void hide(BitInputStream message, String imFile) throws IOException {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(imFile));
		} catch (IOException e) {
		}
		
		int y = img.getHeight();
		int x = img.getWidth();
		
		int xPos = 0;
		int yPos = 0;
		int secretBits = message.readBits(HIDE_BITS);
		while (secretBits != -1) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");

			int argb = img.getRGB(xPos, yPos);
			argb = argb >> HIDE_BITS;
			argb = argb << HIDE_BITS;
			int toHide = argb | secretBits;
			img.setRGB(xPos, yPos, toHide);
			secretBits = message.readBits(HIDE_BITS);
			xPos++;
		}
		
		String beginning = imFile.substring(0, imFile.length() - 4);
		String end = imFile.substring(imFile.length() - 4);
		String newFile = beginning + "1" + end;
		File f = new File(newFile);
		ImageIO.write(img, "png", f);
	}
	
	/**
	 * Extract a message or image from the provided image and write it out
	 * @param imFile  The file to read through
	 * @param out     The BitOutputStream to write to
	 */
	public void unHide(String imFile, BitOutputStream out) {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(imFile));
		} catch (IOException e) {
		}
		int y = img.getHeight();
		int x = img.getWidth();
		
		for (int yPos = 0; yPos < y; yPos++) 
			for (int xPos = 0; xPos < x; xPos++) {
				int pixel = img.getRGB(xPos, yPos);
				pixel = pixel << INT_BITS - HIDE_BITS;
				pixel = pixel >>> INT_BITS - HIDE_BITS;
				out.writeBits(HIDE_BITS, pixel);
			}
	}
	
	/**
	 * Just for fun
	 * @param imFile  The file to get the image size from
	 */
	private void colors(String imFile) {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(imFile));
		} catch (IOException e) {
		}
		int y = img.getHeight();
		int x = img.getWidth();
		
		for (int yPos = 0; yPos < y; yPos++) 
			for (int xPos = 0; xPos < x; xPos++) {
				Color c = new Color(yPos/10, xPos/10, 0);
				img.setRGB(xPos, yPos, c.getRGB());
		}
		File f = new File("Steg/Gradient.png");
		try {
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/*
	 * Main file for testing
	 */
	public static void main(String[] args) throws IOException {
		Steganography s = new Steganography();
		BitInputStream message = new BitInputStream("Steg/TESTINPUT.txt");
		s.hide(message, "Steg/heart.jpg");
		System.out.println("");
		BitOutputStream bos = new BitOutputStream("Steg/newFile.txt");
		s.unHide("Steg/heart1.jpg", bos);
		bos.flush();
	}
}
