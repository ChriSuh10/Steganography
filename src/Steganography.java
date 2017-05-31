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
import java.util.Arrays;

import javax.imageio.ImageIO;

public class Steganography {
	private static final int HIDE_BITS = 2;  	  			     // Number of least significant bits to use
	private static final int BYTE_BITS = 8;						 // Number of bits in a byte;
	private static final int INT_BITS = 32;  	  			     // Number of bits in an int
	private static final int LONG_BITS = 64; 	  			     // Number of bits in a long
	private static final int RGB_CLEAR_HIDE_BITS = 0xFFFCFCFC;   // Clears HIDE_BITS least significant bits from getRGB 
	
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
		try {
			boolean writing = true;
			int y = img.getHeight();
			int x = img.getWidth();
			int xPos = 0;
			int yPos = 0;
			long fileSize = new File(messagePath).length();
			
			hideHeader(x, y, xPos, yPos, fileSize, img);
			xPos = LONG_BITS % y;
			yPos = LONG_BITS / y;
			
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
			String end = imFilePath.substring(imFilePath.length() - 3);
			String newFile = beginning + "1." + end;
			File f = new File(newFile);
			ImageIO.write(img, end, f);
		}
		finally {
			message.close();
		}
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
	 * Hides the given picture's information within HIDE_BITS least significant bits of
	 * red, green, and blue pixels of another image
	 * 
	 * @param picPath       The path of the file to hide
	 * @param imFilePath    The path of the file to hide picPath within
	 * @throws IOException
	 */
	public void hidePic(String picPath, String imFilePath) throws IOException {
		BufferedImage message = ImageIO.read(new File(picPath));
		BufferedImage img = ImageIO.read(new File(imFilePath));
		int x = message.getWidth();
		int y = message.getHeight();
		int imgXPos = 0;
		int imgYPos = 0;
		int imgX = img.getWidth();
		int imgY = img.getHeight();
		
		// Embed original file width
		hideHeader(x, y, imgXPos, imgYPos, x, img);
		imgXPos = INT_BITS % y;
		imgYPos = INT_BITS / y;
		// Embed original file height
		hideHeader(x, y, imgXPos, imgYPos, y, img);
		imgXPos = (2*INT_BITS) % y;
		imgYPos = (2*INT_BITS) / y;
		
		for (int r = 0; r < y; r++) {
			for (int c = 0; c < x; c++) {
				int hideBits = message.getRGB(c, r);
				Color col = new Color(hideBits);
				int red = col.getRed();
				int green = col.getGreen();
				int blue = col.getBlue();
				
				for (int i = 0; i < BYTE_BITS / HIDE_BITS; i++) {
					if (imgXPos >= imgX) {
						imgYPos++;
						imgXPos = 0;
					}
					if (imgYPos >= imgY)
						throw new IndexOutOfBoundsException("Message too long for medium");
					int rgb = img.getRGB(imgXPos, imgYPos);
					rgb &= RGB_CLEAR_HIDE_BITS;
					int hideFromRed = (red & 3) << 16;
					int hideFromGreen = (green & 3) << 8;
					int hideFromBlue = blue & 3;
					int toHide = 255 << 24;
					toHide |= hideFromRed;
					toHide |= hideFromGreen;
					toHide |= hideFromBlue;
					rgb |= toHide;

					red = red >>> HIDE_BITS;
					green = green >>> HIDE_BITS;
					blue = blue >>> HIDE_BITS;
					img.setRGB(imgXPos, imgYPos, rgb);
					imgXPos++;
				}
			}
		}
		// Create new file with the embedded message
		String beginning = imFilePath.substring(0, imFilePath.length() - 4);
		String end = imFilePath.substring(imFilePath.length() - 3);
		String newFile = beginning + "2." + end;
		File f = new File(newFile);
		ImageIO.write(img, end, f);	
	}
	
	/**
	 * Helper method for embedding header information as a long in the HIDE_BITS
	 * least significant bits of the pixel data
	 * 
	 * @param x       Width of the image that will hold the data
	 * @param y       Height of the image that will hold the data
	 * @param xPos    x-coordinate of the pixel to begin writing information from
	 * @param yPos    y-coordinate of the pixel to begin writing information from
	 * @param header  Long to embed 
	 * @param img     Image to embed header within
	 */
	private void hideHeader(int x, int y, int xPos, int yPos, long header, BufferedImage img) {
		for (int i = 0; i < LONG_BITS; i++) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");
			
			long toHide = header;
			toHide = toHide >>> LONG_BITS - 1;
			int rgb = img.getRGB(xPos, yPos);
			rgb = rgb >>> 1;
			rgb = rgb << 1;
			rgb = rgb | (int) toHide;
			img.setRGB(xPos, yPos, rgb);
			header = header << 1;
			xPos++;
		}
	}
	
	/**
	 * Helper method for embedding header information as a int in the HIDE_BITS
	 * least significant bits of the pixel data
	 * 
	 * @param x       Width of the image that will hold the data
	 * @param y       Height of the image that will hold the data
	 * @param xPos    x-coordinate of the pixel to begin writing information from
	 * @param yPos    y-coordinate of the pixel to begin writing information from
	 * @param header  int to embed 
	 * @param img     Image to embed header within
	 */
	private void hideHeader(int x, int y, int xPos, int yPos, int header, BufferedImage img) {
		for (int i = 0; i < INT_BITS; i++) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");
			
			int toHide = header;
			toHide = toHide >>> INT_BITS - 1;
			int rgb = img.getRGB(xPos, yPos);
			rgb = rgb >>> 1;
			rgb = rgb << 1;
			rgb = rgb | toHide;
			img.setRGB(xPos, yPos, rgb);
			header = header << 1;
			xPos++;
		}
	}
	
	/**
	 * Extract a message or image from the provided image and write it out
	 * 
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
		long fileLength = retrieveLongHeader(x, y, xPos, yPos, img);
		xPos = LONG_BITS % y;
		yPos = LONG_BITS / y;
		
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
	 * Retrieve a long embedded as a header
	 * 
	 * @param x     Width of the image that holds the data
	 * @param y     Height of the image that holds the data
	 * @param xPos  x-coordinate of the pixel to begin retrieving data from
	 * @param yPos  y-coordinate of the pixel to begin retrieving data from
	 * @param img   Image that holds the information
	 * @return      A long embedded within img as a header
	 */
	private long retrieveLongHeader(int x, int y, int xPos, int yPos, BufferedImage img) {
		long header = 0;
		for (int i = 0; i < LONG_BITS; i++) {
			header = header << 1;
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			int rgb = img.getRGB(xPos, yPos);
			rgb = rgb << INT_BITS - 1;
			rgb = rgb >>> INT_BITS - 1;
			header = header | rgb;
			xPos++;
		}
		return header;
	}
	
	/**
	 * Retrieve an int embedded as a header
	 * 
	 * @param x     Width of the image that holds the data
	 * @param y     Height of the image that holds the data
	 * @param xPos  x-coordinate of the pixel to begin retrieving data from
	 * @param yPos  y-coordinate of the pixel to begin retrieving data from
	 * @param img   Image that holds the information
	 * @return      An int embedded within img as a header
	 */
	private int retrieveIntHeader(int x, int y, int xPos, int yPos, BufferedImage img) {
		int header = 0;
		for (int i = 0; i < INT_BITS; i++) {
			header = header << 1;
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			int rgb = img.getRGB(xPos, yPos);
			rgb = rgb << INT_BITS - 1;
			rgb = rgb >>> INT_BITS - 1;
			header = header | rgb;
			xPos++;
		}
		return header;
	}
	
	/**
	 * Retrieves a hidden image from within the HIDE_BITS least significant bits 
	 * of the red, green, and blue pixels of the provided image
	 * 
	 * @param imFile         Path of the image file to extract data from
	 * @param recoveredName  Path of the image file to write extracted data to
	 * @throws IOException   
	 */
	public void unHidePic(String imFile, String recoveredName) throws IOException {
		BufferedImage img = ImageIO.read(new File(imFile));
		
		int imgX = img.getWidth();
		int imgY = img.getHeight();
		int xPos = 0;
		int yPos = 0;
		
		int writeX = retrieveIntHeader(imgX, imgY, xPos, yPos, img);
		xPos = INT_BITS % imgY;
		yPos = INT_BITS / imgY;
		int writeY = retrieveIntHeader(imgX, imgY, xPos, yPos, img);
		xPos = (2*INT_BITS) % imgY;
		yPos = (2*INT_BITS) / imgY;
		BufferedImage writeTo = new BufferedImage(writeX, writeY, BufferedImage.TYPE_4BYTE_ABGR);
		int red = 0;
		int green = 0;
		int blue = 0;
		int count = 0;
		
		int index = (yPos * imgY) + xPos;
		int c = 0; 
		int r = 0;
		
		while (yPos < imgY) {
			if (xPos >= imgX) {
				yPos++;
				xPos = 0;
			}

			if (count == BYTE_BITS / HIDE_BITS) {
				if (c >= writeX) {
					r++;
					c = 0;
				}
				if (r >= writeY)
					break;
				count = 0;
				int toWrite = 255 << 8;
				toWrite |= red;
				toWrite = toWrite << 8;
				toWrite |= green;
				toWrite = toWrite << 8;
				toWrite |= blue;
				writeTo.setRGB(c, r, toWrite);
				c++;
			}
			int rgb = img.getRGB(xPos, yPos);
			Color col = new Color(rgb);
			int colRed = (col.getRed() & 3) << 6;
			int colGreen = (col.getGreen() & 3) << 6;
			int colBlue = (col.getBlue() & 3) << 6;
			red = red >> HIDE_BITS;
			green = green >> HIDE_BITS;
			blue = blue >> HIDE_BITS;
			red |= colRed;
			green |= colGreen;
			blue |= colBlue;
			count++;
			xPos++;
		}
		// Create new file with the embedded message
		String end = recoveredName.substring(recoveredName.length() - 3);
		File f = new File(recoveredName);
		ImageIO.write(writeTo, end, f);
	}
	
	/**
	 * For two images of the exact same dimensions, creates a new image in which
	 * each pixel is the difference of the corresponding pixels of the provided 
	 * images
	 * 
	 * @param ogFilePath   Path of the first image, to be subtracted from
	 * @param modFilePath  Path of the second image, to subtract from the first
	 * @throws IOException
	 */
	public void getDifference(String ogFilePath, String modFilePath) throws IOException {
		BufferedImage og = ImageIO.read(new File(ogFilePath));
		BufferedImage mod = ImageIO.read(new File(modFilePath));
		int x = mod.getWidth();
		int y = mod.getHeight();
		int a = og.getHeight();
		int b = og.getWidth();
		
		for (int r = 0; r < y; r++) {
			for (int c = 0; c < x; c++) {
				int ogPix = og.getRGB(c, r);
				int modPix = mod.getRGB(c, r);
				int diff = ogPix - modPix;
				mod.setRGB(c, r, diff);
			}
		}
		String beginning = modFilePath.substring(0, modFilePath.length() - 4);
		String end = modFilePath.substring(modFilePath.length() - 3);
		String newFile = beginning + "Diff." + end;
		File f = new File(newFile);
		ImageIO.write(mod, end, f);
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
	
	/**
	 * Just for testing
	 * @throws IOException
	 */
	private void smallPic() throws IOException {
		BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_3BYTE_BGR);
		int[] rand = new int[25];
		for (int i = 0; i < rand.length; i++)
			rand[i] = (int) (Math.random() * Integer.MAX_VALUE);
		
		System.out.println(Arrays.toString(rand));
		int count = 0;
		for (int r = 0; r < 5; r++) {
			for (int c = 0; c < 5; c++) {
				img.setRGB(c, r, rand[count]);
				count++;
			}
		}
		// Create new file with the embedded message
		File f = new File("Steg/RANDOM.png");
		ImageIO.write(img, "png", f);
	}
	
	/**
	 * Just for testing
	 * @param s
	 */
	public void binaryStringtoInt(String s) {
		int ret = 0;
		for (int i = 0; i < s.length(); i++) {
			double factor = Math.pow(2, i);
			int bit = Integer.parseInt(s.charAt(s.length() - i - 1) + "");
			ret += bit * factor;
		}
		System.out.println(ret);
	}

	/*
	 * Main file for testing
	 */
	public static void main(String[] args) throws IOException {
		Steganography s = new Steganography();
		s.hideMessage("Steg/TESTINPUT.txt", "Steg/coutinho.png");
		s.unHideMessage("Steg/coutinho1.png", "Steg/newFile.txt");
		s.getDifference("Steg/coutinho.png", "Steg/coutinho1.png");

		s.hidePic("Steg/bluedevil.png", "Steg/coutinho.png");
		s.unHidePic("Steg/coutinho2.png", "Steg/newFile.png");
		s.getDifference("Steg/coutinho.png", "Steg/coutinho2.png");
	}
}
