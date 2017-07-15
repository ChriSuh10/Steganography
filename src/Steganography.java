/**
 *  Basic Steganography program to hide text or image data within
 *  the least significant red, green, and blue bits of images 
 *  supported by ImageIO.read
 *  
 *   Copyright (C) 2017  Christopher Suh
 *   
 *   This file is part of Steganography
 *
 *   Steganography is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Steganography is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class Steganography {
	private final int NumHide;                   // The number of least significant bits to hide info in
	                                             // MUST be a factor of BYTE_BITS to work properly
	
	private final int GetLastNumHideBits;        // Gets the NumHide least significant bits from an int
	private final int ClearNumHideBits;          // Clears NumHide least significant bits from an int
	private final int RGBClearNumHideBits;       // Used to clear the least significant NumHide from 
                                                 // red, green, and blue pixels in getRGB
	
	private static final int HIDE_HEADER = -2;   // To set the last bit from pixel file to 0
	private static final int REVEAL_HEADER = 1;  // To get last bit from pixel file
	private static final int BYTE_BITS = 8;      // Number of bits in a byte;
	private static final int LONG_BITS = 64;     // Number of bits in a long
	
	/**
	 * Default Constructor
	 */
	public Steganography() {
		this.NumHide = 2;
		this.GetLastNumHideBits = 3;
		this.ClearNumHideBits = 0x7FFFFFFC;
		this.RGBClearNumHideBits = 0xFFFCFCFC;
	}
	
	/**
	 * Constructor
	 * Calculates other class variables based on numHide
	 * 
	 * @param numHide  Number of bits to hide data in
	 */
	public Steganography(int numHide) {
		this.NumHide = numHide;
		this.GetLastNumHideBits = (int) (Math.pow(2, numHide) - 1);
		this.ClearNumHideBits = -(int) (Math.pow(2, numHide));
		int rgb = 0;
		rgb |= ~this.ClearNumHideBits;
		rgb |= ~this.ClearNumHideBits << BYTE_BITS;
		rgb |= ~this.ClearNumHideBits << (2 * BYTE_BITS);
		this.RGBClearNumHideBits = ~rgb;
	}
	
	/**
	 * Constructor
	 * 
	 * @param numHide  Number of bits to hide data in
	 * @param clear    Clears NumHide least significant bits from getRGB
	 * @param get      Gets last NumHide bits from rgb ints
	 * @param clear    Clears numHide least significant bits from red, green, and blue ints
	 */
	public Steganography(int numHide, int get, int clear,  int rgb) {
		this.NumHide = numHide;
		this.GetLastNumHideBits = get;
		this.ClearNumHideBits = clear;
		this.RGBClearNumHideBits = rgb;
	}
	
	/**
	 * Hides message within NumHide least significant bits of the file directed by imFile
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
		
		hideHeader(xPos, yPos, fileSize, img);
		xPos = LONG_BITS % y;
		yPos = LONG_BITS / y;

		while (writing) {
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			if (yPos >= y)
				throw new IndexOutOfBoundsException("Message too long for medium");

			Color col  = new Color(img.getRGB(xPos, yPos));
			writing = NumHide(col, message, img, xPos, yPos);
			xPos++;
		}		
		writeImgFile(imFilePath, img, true);
		message.close();
	}
	
	/**
	 * Helper method to hide message in the least significant bits of the
	 * red, green, and blue pixels
	 * 
	 * @param col       Color representing the pixel at xPos, yPos
	 * @param message   BitInputStream representing the message to be hidden
	 * @param img       Image to hide the message in
	 * @param xPos      x-coordinate of the pixel being modified
	 * @param yPos      y-coordinate of the pixel being modified
	 * @return          boolean toggle representing whether the end of the file has been reached
	 */
	private boolean NumHide(Color col, BitInputStream message, BufferedImage img, int xPos, int yPos) {

		int red = col.getRed();
		int green = col.getGreen();
		int blue = col.getBlue();
		int secretBits = message.readBits(NumHide);
		boolean writing = true;
		
		if (secretBits != -1) {
			red &= ClearNumHideBits;
			red |= secretBits;
			secretBits = message.readBits(NumHide);
			green &= ClearNumHideBits;
			green |= secretBits;
			secretBits = message.readBits(NumHide);
			blue &= ClearNumHideBits;
			blue |= secretBits;
		}
		
		if (secretBits == -1)
			writing = false;
		
		img.setRGB(xPos, yPos, new Color(red, green, blue).getRGB());
		return writing;	
	}
	
	/**
	 * Hides the given picture's information within NumHide least significant bits of
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
		hideHeader(imgXPos, imgYPos, x, img);
		imgXPos = LONG_BITS % y;
		imgYPos = LONG_BITS / y;
		// Embed original file height
		hideHeader(imgXPos, imgYPos, y, img);
		imgXPos = (2*LONG_BITS) % y;
		imgYPos = (2*LONG_BITS) / y;
		
		for (int r = 0; r < y; r++) {
			for (int c = 0; c < x; c++) {
				int messageRGB = message.getRGB(c, r);
				Color col = new Color(messageRGB);
				int red = col.getRed();
				int green = col.getGreen();
				int blue = col.getBlue();
				
				for (int i = 0; i < BYTE_BITS / NumHide; i++) {
					if (imgXPos >= imgX) {
						imgYPos++;
						imgXPos = 0;
					}
					if (imgYPos >= imgY)
						throw new IndexOutOfBoundsException("Message too long for medium");
					int rgb = img.getRGB(imgXPos, imgYPos);
					rgb &= RGBClearNumHideBits;
					int hideFromRed = (red & GetLastNumHideBits) << (2 * BYTE_BITS);
					int hideFromGreen = (green & GetLastNumHideBits) << BYTE_BITS;
					int hideFromBlue = blue & GetLastNumHideBits;
					int toHide = 255 << 24;
					toHide |= hideFromRed;
					toHide |= hideFromGreen;
					toHide |= hideFromBlue;
					rgb |= toHide;

					red = red >>> NumHide;
					green = green >>> NumHide;
					blue = blue >>> NumHide;
					img.setRGB(imgXPos, imgYPos, rgb);
					imgXPos++;
				}
			}
		}
		writeImgFile(imFilePath, img, false);
	}
	
	/**
	 * Helper method for embedding header information as a long in the NumHide
	 * least significant bits of the pixel data
	 * 
	 * @param x       Width of the image that will hold the data
	 * @param y       Height of the image that will hold the data
	 * @param xPos    x-coordinate of the pixel to begin writing information from
	 * @param yPos    y-coordinate of the pixel to begin writing information from
	 * @param header  Long to embed 
	 * @param img     Image to embed header within
	 */
	private void hideHeader(int xPos, int yPos, long header, BufferedImage img) {
		int x = img.getWidth();
		int y = img.getHeight();
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
			rgb &= HIDE_HEADER;
			rgb |= toHide;
			img.setRGB(xPos, yPos, rgb);
			header = header << 1;
			xPos++;
		}
	}
	
	/**
	 * Helper method for embedding header information as a int in the NumHide
	 * least significant bits of the pixel data
	 * 
	 * @param x       Width of the image that will hold the data
	 * @param y       Height of the image that will hold the data
	 * @param xPos    x-coordinate of the pixel to begin writing information from
	 * @param yPos    y-coordinate of the pixel to begin writing information from
	 * @param header  int to embed 
	 * @param img     Image to embed header within
	 */
	
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
		long fileLength = retrieveHeader(xPos, yPos, img);
		xPos = LONG_BITS % y;
		yPos = LONG_BITS / y;
		
		long numIterations = ((fileLength * BYTE_BITS) / (NumHide * 3)) + 1; // To compensate for the fact that each 
											                                  // iteration only fetches NumHide * 3 bits
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
		red &= GetLastNumHideBits;
		green &= GetLastNumHideBits;
		blue &= GetLastNumHideBits;
		out.writeBits(NumHide, red);
		out.writeBits(NumHide, green);
		out.writeBits(NumHide, blue);
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
	private long retrieveHeader(int xPos, int yPos, BufferedImage img) {
		int x = img.getWidth();
		long header = 0;
		for (int i = 0; i < LONG_BITS; i++) {
			header = header << 1;
			if (xPos >= x) {
				yPos++;
				xPos = 0;
			}
			int rgb = img.getRGB(xPos, yPos);
			rgb &= REVEAL_HEADER;
			header = header | rgb;
			xPos++;
		}
		return header;
	}
	
	
	/**
	 * Retrieves a hidden image from within the NumHide least significant bits 
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
		
		int writeX = (int) retrieveHeader(xPos, yPos, img);
		xPos = LONG_BITS % imgY;
		yPos = LONG_BITS / imgY;
		int writeY = (int) retrieveHeader(xPos, yPos, img);
		xPos = (2*LONG_BITS) % imgY;
		yPos = (2*LONG_BITS) / imgY;
		BufferedImage writeTo = new BufferedImage(writeX, writeY, BufferedImage.TYPE_4BYTE_ABGR);
		int red = 0;
		int green = 0;
		int blue = 0;

		for (int r = 0; r < writeY; r++) {
			for (int c = 0; c < writeX; c++) {
				for (int count = 0; count < BYTE_BITS / NumHide; count++) {
					if (xPos >= imgX) {
						yPos++;
						xPos = 0;
					}
					int rgb = img.getRGB(xPos, yPos);
					Color col = new Color(rgb);
					int colRed = (col.getRed() & GetLastNumHideBits) << (BYTE_BITS - NumHide);
					int colGreen = (col.getGreen() & GetLastNumHideBits) << (BYTE_BITS - NumHide);
					int colBlue = (col.getBlue() & GetLastNumHideBits) << (BYTE_BITS - NumHide);
					red = red >> NumHide;
					green = green >> NumHide;
					blue = blue >> NumHide;
					red |= colRed;
					green |= colGreen;
					blue |= colBlue;
					xPos++;
				}	
				int toWrite = 255 << BYTE_BITS;
				toWrite |= red;
				toWrite = toWrite << BYTE_BITS;
				toWrite |= green;
				toWrite = toWrite << BYTE_BITS;
				toWrite |= blue;
				writeTo.setRGB(c, r, toWrite);
			}
		}
		// Create new file with the embedded message
		String end = recoveredName.substring(recoveredName.length() - 3);
		File f = new File(recoveredName);
		ImageIO.write(writeTo, end, f);
	}
	
	/**
	 * Helper method for writing an image file after hiding a message or picture in it
	 * @param fileName  The file name of the original image
	 * @param img       The image to write
	 * @param flag      True if the image contains text, false if it contains a picture
	 */
	private void writeImgFile(String fileName, BufferedImage img, boolean flag) {
		String beginning = fileName.substring(0, fileName.length() - 4);
		String end = fileName.substring(fileName.length() - 3);
		String newFile = beginning;
		
		if (flag) {
			newFile += "1." + end;
		} else {
			newFile += "2." + end;
		}
		
		File f = new File(newFile);
		try {
			ImageIO.write(img, end, f);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		File f = new File("data/Gradient.png");
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
		File f = new File("data/RANDOM.png");
		ImageIO.write(img, "png", f);
	}
	
	/**
	 * Just for testing
	 * @param s  The string representing the binary form of an int
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
}
