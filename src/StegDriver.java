/*
    Driver Class demonstrating the Steganography library
  
    Copyright (C) 2017  Christopher Suh
   
    This file is part of Steganography

    Steganography is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Steganography is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
    	
import java.io.IOException;

public class StegDriver {
	public static void main(String[] args) throws IOException {
		//Steganography s = new Steganography();
		//Steganography s = new Steganography(4, 15, -32, 0xFFF0F0F0);
		Steganography s = new Steganography(4);
		
		// Text
		long t1 = System.nanoTime();
		s.hideMessage("data/TESTINPUT.txt", "data/bluedevil.png");
		s.unHideMessage("data/bluedevil1.png", "data/newFile.txt");
		//Miscellaneous.getDifference("data/coutinho.png", "data/coutinho1.png");
		long diffText = System.nanoTime() - t1;
		System.out.println("Elapsed time for encoding and decoding text: " + diffText/1E9);
		
		// Image
/*		long t2 = System.nanoTime();
		s.hidePic("data/bluedevil.png", "data/coutinho.png");
		s.unHidePic("data/coutinho2.png", "data/newFile.png");
		//Miscellaneous.getDifference("data/coutinho.png", "data/coutinho2.png");
		long diffPic = System.nanoTime() - t2;
		System.out.println("Elapsed time for encoding and decoding picture: " + diffPic/1E9);*/
	}
}
