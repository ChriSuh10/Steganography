import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class Miscellaneous {
	/**
	 * For two images of the exact same dimensions, creates a new image in which
	 * each pixel is the difference of the corresponding pixels of the provided 
	 * images
	 * 
	 * @param ogFilePath   Path of the first image, to be subtracted from
	 * @param modFilePath  Path of the second image, to subtract from the first
	 * @throws IOException
	 */
	public static void getDifference(String ogFilePath, String modFilePath) throws IOException {
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
	private static void colors(String imFile) {
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
	private static void smallPic() throws IOException {
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
	public static void binaryStringtoInt(String s) {
		int ret = 0;
		for (int i = 0; i < s.length(); i++) {
			double factor = Math.pow(2, i);
			int bit = Integer.parseInt(s.charAt(s.length() - i - 1) + "");
			ret += bit * factor;
		}
		System.out.println(ret);
	}

}
