/**************************************************************************
 chdkptpJ - Java CHDK PTP framework.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)

 This file is part of chdkptpJ.

 chdkptpJ is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 chdkptpJ is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * Check for bad- and hotpixels.
 * 
 * 1. Make 3 photos for white "rs -raw -tv=1 w1 -sv=100 -sd=200mm"
 * 
 * 2. Make 3 photos for black "rs -raw -tv=1 b1 -sv=100 -sd=200mm"
 * 
 * 3. dcraw -d -j -t 0 -W -c <file> | convert - <file.png>
 */
public class BadPixelsCalculation {

    static final int THRESHOLD = 64;

    public static void main(String[] args) throws Exception {
        for (String f : new String[] { "/tmp/b1.png", "/tmp/b2.png", "/tmp/b3.png", "/tmp/w1.png",
                "/tmp/w2.png", "/tmp/w3.png" }) {
            check(f);
        }
    }

    static void check(String f) throws Exception {
        System.out.println("Check for " + f);

        int[] cs = new int[256];
        BufferedImage img = ImageIO.read(new File(f));

        // is it white or black ?
        int[] bw = new int[8];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = rgb & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb >> 16) & 0xff;
                int c = (r + g + b) / 3 / 32;
                bw[c]++;
            }
        }
        boolean black;
        if (bw[0] > bw[7] * 10) {
            black = true;
        } else if (bw[7] > bw[0] * 10) {
            black = false;
        } else {
            System.err.println("Not too white or too black");
            System.exit(1);
            return;
        }

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = rgb & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb >> 16) & 0xff;
                int c = (r + g + b) / 3;
                cs[c]++;

                if (black) {
                    if (c > 256 - THRESHOLD) {
                        System.out.println("    " + x + " " + y + " = " + c);
                    }
                } else {
                    if (c < THRESHOLD) {
                        System.out.println("    " + x + " " + y + " = " + c);
                    }
                }
            }
        }
    }
}
