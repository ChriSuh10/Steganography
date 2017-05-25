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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class Steganography {
	private static final int HIDE_BITS = 2;  // Number of least significant bits to use
	private static final int INT_BITS = 32;  // Number of bits in an int
	private static final int LONG_BITS = 64; // Number of bits in a long
	
	/**
	 * Hides message within HIDE_BITS least significant bits of the file directed by imFile
	 * 
	 * @param messageFilePath  The path of the message to hide
	 * @param imFilePath	   The image file path (must be supported by ImageIO) to hide the message in
	 * @throws IOException
	 */
	public void hideMessage(String messagePath, String imFilePath) throws IOException {
		BufferedImage img = ImageIO.read(new File(imFilePath));
		BitInputStream message = new BitInputStream(messagePath);
		boolean writing = true;
		int y = img.getHeight();
		int x = img.getWidth();
		int xPos = 0;
		int yPos = 0;
		long fileSize = new File(messagePath).length();
		
		// Write file size length in the first 64 blue bits of the file
		for (int i = 0; i < LONG_BITS; i++) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");
			
			long toHide = fileSize;
			toHide = toHide >>> LONG_BITS - 1;
			int rgb = img.getRGB(xPos, yPos);
			rgb = rgb >>> 1;
			rgb = rgb << 1;
			rgb = rgb | (int) toHide;
			img.setRGB(xPos, yPos, rgb);
			fileSize = fileSize << 1;
			xPos++;
		}
		
		while (writing) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");

			Color c  = new Color(img.getRGB(xPos, yPos));
			int red = c.getRed();
			int green = c.getGreen();
			int blue = c.getBlue();
			writing = hideBits(red, green, blue, message, img, xPos, yPos);
			xPos++;
		}
		// Create new file with the embedded message
		String beginning = imFilePath.substring(0, imFilePath.length() - 4);
		String end = imFilePath.substring(imFilePath.length() - 4);
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
	private boolean hideBits(int red, int green, int blue, BitInputStream message, 
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
	 * @param outFile The file to create a BitOutPutStream to
	 * @throws IOException 
	 */
	public void unHideMessage(String imFile, String outFile) throws IOException {
		BufferedImage img = ImageIO.read(new File(imFile));
		BitOutputStream out = new BitOutputStream(outFile);
		int y = img.getHeight();
		int x = img.getWidth();
		int yPos = 0;
		int xPos = 0;
		long fileLength = 0;
		
		// Retrieve file size from first 64 bits
		for (int i = 0; i < LONG_BITS; i++) {
			fileLength = fileLength << 1;
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			int rgb = img.getRGB(xPos, yPos);
			rgb = rgb << INT_BITS - 1;
			rgb = rgb >>> INT_BITS - 1;
			fileLength = fileLength | rgb;
			xPos++;
		}
		long numIterations = ((fileLength * 8)/6) + 1; // To compensate for the fact that each 
											           // iteration only fetches 6 bits
		for (int i = 0; i < numIterations; i++) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			Color pixelCol = new Color(img.getRGB(xPos, yPos));
			getPixelBits(pixelCol.getRed(), pixelCol.getGreen(), pixelCol.getBlue(), out);
			xPos++;
		}
		out.flush();
	}
	
	/**
	 * 
	 * @param red    Red component of the pixel being examined
	 * @param green  Green component of the pixel being examined
	 * @param blue   Blue component of the pixel being examined
	 * @param out    BitOutputStream to write the hidden bits to
	 */
	private void getPixelBits(int red, int green, int blue, BitOutputStream out) {
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
		s.hideMessage("Steg/TESTINPUT.txt", "Steg/bluedevil.png");
		s.unHideMessage("Steg/bluedevil1.png", "Steg/newFile.txt");
	}
}
