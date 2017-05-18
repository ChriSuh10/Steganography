import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Steganography {
	private static final int HIDE_BITS = 2; 
	
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
				pixel = pixel << 32 - HIDE_BITS;
				pixel = pixel >>> 32 - HIDE_BITS;
				out.writeBits(HIDE_BITS, pixel);
			}
	}
	

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
