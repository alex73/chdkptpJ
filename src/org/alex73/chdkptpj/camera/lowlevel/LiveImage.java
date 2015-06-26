/**************************************************************************
 chdkptpJ - Java CHDK PTP framework.

 Copyright (C) 2012-2014 reyalp
               2015 Aleś Bułojčyk (alex73mail@gmail.com)

 This file is part of chdkptpJ. Converted from liveimg.c of chdkptp.

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
package org.alex73.chdkptpj.camera.lowlevel;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

/**
 * YUV->RGB conversion translated from chdkptp/liveimg.c.
 * 
 * Doesn't work correctly yet.
 */
public class LiveImage {
    public final static int Aspect_4_3 = 0;
    public final static int Aspect_16_9 = 1;

    public final static int FrameBuffer_YUV8 = 0; // UYV YYY, used for live view
    public final static int FrameBuffer_PAL8 = 1; // 8 bit palleted. used for

    // See live_view.h#lv_data_header
    private static final int OFFSET_VERSION_MAJOR = 0;
    private static final int OFFSET_VERSION_MINOR = 4;
    private static final int OFFSET_ASPECT_RATIO = 8; // physical aspect ratio of LCD
    private static final int OFFSET_PALETTE_TYPE = 12;
    private static final int OFFSET_PALETTE_DATA_START = 16;
    /**
     * framebuffer descriptions are given as offsets, to allow expanding the structures in minor protocol
     * changes
     */
    private static final int OFFSET_VP_DESC_START = 20;
    private static final int OFFSET_BM_DESC_START = 24;

    // See live_view.h#lv_framebuffer_desc
    /** framebuffer type - note future versions might use different structures depending on type */
    private static final int OFFSETFB_FB_TYPE = 0;
    /** offset of data from start of live view header */
    private static final int OFFSETFB_DATA_START = 4;
    /** buffer width in pixels. data size is always buffer_width*visible_height*(buffer bpp based on type) */
    private static final int OFFSETFB_BUFFER_WIDTH = 8;
    /**
     * visible size in pixels describes data within the buffer which contains image data to be displayed any
     * offsets within buffer data are added before sending, so the top left pixel is always the first first
     * byte of data. width must always be <= buffer_width if buffer_width is > width, the additional data
     * should be skipped visible_height also defines the number of data rows
     */
    private static final int OFFSETFB_VISIBLE_WIDTH = 12;
    private static final int OFFSETFB_VISIBLE_HEIGHT = 16;
    /**
     * margins pixels offsets needed to replicate display position on cameras screen not used for any buffer
     * offsets
     */
    private static final int OFFSETFB_MARGIN_LEFT = 20;
    private static final int OFFSETFB_MARGIN_TOP = 24;
    private static final int OFFSETFB_MARGIN_RIGHT = 28;
    private static final int OFFSETFB_MARGIN_BOTTOM = 32;

    private byte[] buf;
    private ByteBuffer bb;

    // bitmap overlay

    public static void main(String[] a) throws Exception {
        File f = new File("/home/alex/MyShare-Temp/c/p2.bin");
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        in.read(b, 0, b.length);
        ImageIO.write(new LiveImage(b).decode(2), "png", new File("/home/alex/MyShare-Temp/c/p21.png"));
    }

    /**
     * Creates a new instance of
     *
     * @param packet
     *            containing viewport data
     */
    public LiveImage(byte[] packet) {
        this.buf = packet;
        bb = ByteBuffer.wrap(this.buf).order(ByteOrder.LITTLE_ENDIAN);
        System.out.println(this);
    }

    /**
     * 
     * @param par
     *            - 1 or 2 (if 2, every other pixel in the x axis is discarded (for viewports with a 1:2 par))
     * @return
     */
    public BufferedImage decode(int par) {
        int[] pal_rgba = convert_palette();

        int bmStart = bb.getInt(OFFSET_BM_DESC_START);
        int vwidth = bb.getInt(bmStart + OFFSETFB_VISIBLE_WIDTH) / par;
        int vheight = bb.getInt(bmStart + OFFSETFB_VISIBLE_HEIGHT);
        int dispsize = vwidth * vheight;

        BufferedImage result = new BufferedImage(vwidth, vheight, BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = result.getRaster();
        int[] rasterArray = ((DataBufferInt) raster.getDataBuffer()).getData();

        int vpStart = bb.getInt(OFFSET_VP_DESC_START);
        int dataStart = bb.getInt(vpStart + OFFSETFB_DATA_START);
        int o = 0;
        int y_inc = bb.getInt(bmStart + OFFSETFB_VISIBLE_WIDTH);
        int x_inc = par;
        int p = dataStart + (vheight - 1) * y_inc;
        for (int y = 0; y < vheight; y++, p -= y_inc) {
            for (int x = 0; x < vwidth; x += x_inc, o++) {
                int off = p + x;
                int color = bb.getInt(off);
                rasterArray[o] = pal_rgba[color];
            }
        }
        return result;
    }

    /*
     * typedef struct { uint8_t r; uint8_t g; uint8_t b; uint8_t a; } palette_entry_rgba_t;
     */

    int[] convert_palette() {
        PaletteConverter converter = paletteConverters[bb.getInt(OFFSET_PALETTE_TYPE)];
        
        int[] result = new int[256]; // palette_entry_rgba_t

        int paletteStartOffset = bb.getInt(OFFSET_PALETTE_DATA_START);

        for (int i = 0; i < result.length; i++) {
            result[i] = converter.convert(paletteStartOffset, i);
        }
        return result;
    }

    interface PaletteConverter {
        int convert(int paletteStartOffset, int pixelIndex);
    }

    PaletteConverter palette_type5_to_rgba = new PaletteConverter() {
        public int convert(int paletteStartOffset, int pixelIndex) {
            return palette_AYUV_to_rgba(paletteStartOffset, pixelIndex, 4);
        };
    };

    /**
     * liveimg.c#104 (svn rev. 667)
     */
    PaletteConverter[] paletteConverters = new PaletteConverter[] { null, null, null, null, null,
            palette_type5_to_rgba };
    int[] paletteSizes = new int[] { 0, 16, 16, 256, 16, 256 };

    static final byte alpha2_lookup[] = { (byte) 128, (byte) 171, (byte) 214, (byte) 255 };

    /**
     * Convert 32 bit AYUV palette to RGB.
     * 
     * Assumes A only uses 2 bits - 'shift' parameter used to scale A value.
     */
    int palette_AYUV_to_rgba(int paletteStartOffset, int pixel, int shift) {
        // special case for index 0
        if (pixel == 0) {
            return 0;
        }

        // from lower to higher byte: v,u,y,a
        int ayuv = bb.getInt(paletteStartOffset + pixel);
        byte sv = (byte) ayuv;
        byte su = (byte) (ayuv >> 8);
        byte sy = (byte) (ayuv >> 16);
        byte sa = (byte) (ayuv >> 24);

        int r;
        // from lower to higher byte: r,g,b,a
        byte ra = alpha2_lookup[(sa >> shift) & 3];
        byte rr = yuv_to_r(sy, sv);
        byte rg = yuv_to_g(sy, su, sv);
        byte rb = yuv_to_b(sy, su);
        return (((ra) << 24) | ((rb & 0xff) << 16) | ((rg & 0xff) << 8) | ((rr & 0xff)));
    }

    public byte clip_yuv(int v) {
        if (v < 0) {
            return 0;
        } else if (v > 255) {
            return (byte) 0xFF;
        } else {
            return (byte) v;
        }
    }

    public byte yuv_to_r(byte y, byte v) {
        return clip_yuv(((y << 12) + v * 5743 + 2048) >> 12);
    }

    public byte yuv_to_g(byte y, byte u, byte v) {
        return clip_yuv(((y << 12) - u * 1411 - v * 2925 + 2048) >> 12);
    }

    public byte yuv_to_b(byte y, byte u) {
        return clip_yuv(((y << 12) + u * 7258 + 2048) >> 12);
    }
}
