/**
 *	Basic Steganography program to hide text or image data within
 *  the least significant red, green, and blue bits of images 
 *  supported by ImageIO.read
 *
 *	@author Christopher Suh
 *	@date 19 May 2017
 */

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
	 * 
	 * @param message       The message to be hidden, as a BitInputStream
	 * @param imFile	    The image file (must be supported by ImageIO) to hide the message in
	 * @throws IOException
	 */
	public void Hide(BitInputStream message, String imFile) throws IOException {
		BufferedImage img = ImageIO.read(new File(imFile));
		
		boolean writing = true;
		int y = img.getHeight();
		int x = img.getWidth();
		int xPos = 0;
		int yPos = 0;
		//int secretBits = message.readBits(HIDE_BITS);
		
		while (writing) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");

			/*int argb = img.getRGB(xPos, yPos);
			argb = argb >> HIDE_BITS;
			argb = argb << HIDE_BITS;
			int toHide = argb | secretBits;
			img.setRGB(xPos, yPos, toHide);
			secretBits = message.readBits(HIDE_BITS);
			xPos++;*/
			Color c  = new Color(img.getRGB(xPos, yPos));
			int red = c.getRed();
			int green = c.getGreen();
			int blue = c.getBlue();
			writing = HideBits(red, green, blue, message, img, xPos, yPos);
			xPos++;
		}
		
		String beginning = imFile.substring(0, imFile.length() - 4);
		String end = imFile.substring(imFile.length() - 4);
		String newFile = beginning + "1" + end;
		File f = new File(newFile);
		ImageIO.write(img, "png", f);
	}
	
	/**
	 * Helper method to hide message in the least significant bits of the
	 * red, green, and blue pixels
	 * 
	 * @param red       Red component of the color
	 * @param green     Green component of the color
	 * @param blue      Blue component of the color
	 * @param message   BitInputStream representing the message to be hidden
	 * @param img       Image to hide the message in
	 * @param xPos      x-coordinate of the pixel being modified
	 * @param yPos      y-coordinate of the pixel being modified
	 * @return          boolean toggle representing whether the end of the file has been reached
	 */
	private boolean HideBits(int red, int green, int blue, BitInputStream message, 
			BufferedImage img, int xPos, int yPos) {
		int secretBits = message.readBits(HIDE_BITS);
		boolean writing = true;
		
		if (secretBits != -1) {
			red = red >> HIDE_BITS;
			red = red << HIDE_BITS;
			red = red | secretBits;
			secretBits = message.readBits(HIDE_BITS);
		}
		else 
			writing = false;
		
		if (secretBits != -1) {
			green = green >> HIDE_BITS;
			green = green << HIDE_BITS;
			green = green | secretBits;
			secretBits = message.readBits(HIDE_BITS);
		}else
			writing = false;
		
		if (secretBits != -1) {
			blue = blue >> HIDE_BITS;
			blue = blue << HIDE_BITS;
			blue = blue | secretBits;
		}
		else
			writing = false;
		
		img.setRGB(xPos, yPos, new Color(red, green, blue).getRGB());
		return writing;	
	}
	
	/**
	 * Extract a message or image from the provided image and write it out
	 * @param imFile  The file to read through
	 * @param out     The BitOutputStream to write to
	 * @throws IOException 
	 */
	public void UnHide(String imFile, BitOutputStream out) throws IOException {
		BufferedImage img = ImageIO.read(new File(imFile));
		int y = img.getHeight();
		int x = img.getWidth();
		
		for (int yPos = 0; yPos < y; yPos++) {
			for (int xPos = 0; xPos < x; xPos++) {
				/*int pixel = img.getRGB(xPos, yPos);
				pixel = pixel << INT_BITS - HIDE_BITS;
				pixel = pixel >>> INT_BITS - HIDE_BITS;
				out.writeBits(HIDE_BITS, pixel);*/
				Color c = new Color(img.getRGB(xPos, yPos));
				GetPixelBits(c.getRed(), c.getGreen(), c.getBlue(), out);
			}
		}
	}
	
	/**
	 * 
	 * @param red    Red component of the pixel being examined
	 * @param green  Green component of the pixel being examined
	 * @param blue   Blue component of the pixel being examined
	 * @param out    BitOutputStream to write the hidden bits to
	 */
	private void GetPixelBits(int red, int green, int blue, BitOutputStream out) {
		red = red << INT_BITS - HIDE_BITS;
		red = red >>> INT_BITS - HIDE_BITS;
		out.writeBits(HIDE_BITS,  red);
		green = green << INT_BITS - HIDE_BITS;
		green = green >>> INT_BITS - HIDE_BITS;
		out.writeBits(HIDE_BITS,  green);
		blue = blue << INT_BITS - HIDE_BITS;
		blue = blue >>> INT_BITS - HIDE_BITS;
		out.writeBits(HIDE_BITS,  blue);
	}
	
	/**
	 * Just for fun
	 * @param imFile  The file to get the image size from
	 */
	private void Colors(String imFile) {
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
		BitInputStream message = new BitInputStream("Steg/bluedevil.png");
		s.Hide(message, "Steg/adobe.png");
		BitOutputStream bos = new BitOutputStream("Steg/newFile.txt");
		s.UnHide("Steg/adobe1.png", bos);
		bos.flush();
	}
}
