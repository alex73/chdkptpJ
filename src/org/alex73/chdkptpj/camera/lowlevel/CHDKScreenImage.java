package org.alex73.chdkptpj.camera.lowlevel;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

/**
 * Contains logic required to decode a raw CHDK viewport bytes {@link Packet} into a readable
 * {@link BufferedImage}
 * 
 * @author <a href="mailto:alex.camilo@gmail.com">Alex Camilo</a>
 *
 *         TODO : change to chdkptp implementation
 */
public class CHDKScreenImage {
    public final static int Aspect_4_3 = 0;
    public final static int Aspect_16_9 = 1;

    public final static int FrameBuffer_YUV8 = 0; // UYV YYY, used for live view
    public final static int FrameBuffer_PAL8 = 1; // 8 bit palleted. used for

    private byte[] buf;
    
    // bitmap overlay

    /**
     * Creates a new instance of
     *
     * @param packet
     *            containing viewport data
     */
    public CHDKScreenImage(byte[] packet) {
        this.buf = packet;
    }

    /**
     * Decodes a 6 byte uYvYYY 4 pixel group into a provided ARGB int array
     * /suitable for a bufferedImage [color u component for all 4][pixel 0
     * brightness][color v component for all 4][pixel 1 brightness][pixel 2
     * brightness][pixel 3 brightness]
     * 
     * @return current frame from the camera viewport buffer as BufferedImage
     */
    public BufferedImage decodeViewport() {
        // each group of 4 is represented by 6 bytes
        int buflen = ((this.viewportBufferWidth() * this
                .viewportVisableHeight()) / 4) * 6;

        BufferedImage b = new BufferedImage(this.viewportBufferWidth(),
                this.viewportVisableHeight(), BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = b.getRaster();
        DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
        int[] convertedImageArray = dataBuffer.getData();
        int offset = this.viewportDataStart();
        int rgbIndex = 0;
        for (int i = 0; i < buflen; i += 6) {
            byte u = buf[(i + offset)];
            byte y0 = buf[(i + offset) + 1];
            byte v = buf[(i + offset) + 2];
            byte y1 = buf[(i + offset) + 3];
            byte y2 = buf[(i + offset) + 4];
            byte y3 = buf[(i + offset) + 5];

            convertedImageArray[rgbIndex + 0] = ((byte) 0xff) << 24 | // Alpha
                    (yuv_to_r(y0, v) << 16) & (0xff0000) | // Red
                    (yuv_to_g(y0, v, u) << 8) & (0xff00) | // Green
                    (yuv_to_b(y0, u)) & (0xff); // Blue
            convertedImageArray[rgbIndex + 1] = ((byte) 0xff) << 24 | // Alpha
                    (yuv_to_r(y1, v) << 16) & (0xff0000) | // Red
                    (yuv_to_g(y1, v, u) << 8) & (0xff00) | // Green
                    (yuv_to_b(y1, u)) & (0xff); // Blue; // Blue
            convertedImageArray[rgbIndex + 2] = ((byte) 0xff) << 24 | // Alpha
                    (yuv_to_r(y2, v) << 16) & (0xff0000) | // Red
                    (yuv_to_g(y2, v, u) << 8) & (0xff00) | // Green
                    (yuv_to_b(y2, u)) & (0xff); // Blue // Blue
            convertedImageArray[rgbIndex + 3] = ((byte) 0xff) << 24 | // Alpha
                    (yuv_to_r(y3, v) << 16) & (0xff0000) | // Red
                    (yuv_to_g(y3, v, u) << 8) & (0xff00) | // Green
                    (yuv_to_b(y3, u)) & (0xff); // Blue // Blue
            rgbIndex += 4;
        }

        return b;
    }

    @Override
    public String toString() {
        String r = "\nCHDK Image--------\n";
        r += "\tImage Data Header\n";
        r += "\t\tVersion Number:\t\t\t" + getMajorVersion() + "."
                + getMinorVersion() + "\n";
        r += "\t\tAspect Ratio:\t\t\t" + getLCDAspectRatio() + "\n";
        r += "\t\tPallete Type:\t\t\t" + getPalleteType();
        r += "" + "\n";
        r += "\t\tPallete Data Start:\t\t" + getPaletteDataStart() + "\n";
        r += "\t\tViewport Descriptor Start:\t" + getViewportDescriptorStart()
                + "\n";
        r += "\t\tBitmap Descriptor Start:\t" + getBitmapDescriptorStart()
                + "\n";

        r += "\n\tViewportDescriptor@(" + getViewportDescriptorStart() + ")\n";
        r += "\t\tFrame Buffer Type:\t\t" + viewportFramebufferType();
        if (viewportFramebufferType() == FrameBuffer_YUV8)
            r += " (YUV8)";
        else if (viewportFramebufferType() == FrameBuffer_PAL8)
            r += " (PAL8)";
        r += "\n";
        r += "\t\tData Start:\t\t\t" + viewportDataStart();
        if (viewportDataStart() == 0)
            r += "(No Image)";
        else
            r += "[Valid Image]";
        r += "\n";
        r += "\t\tBuffer Width:\t\t\t" + viewportBufferWidth() + "\n";
        r += "\t\tBuffer Visable Width:\t\t" + viewportVisableWidth() + "\n";
        r += "\t\tBuffer Visable Height:\t\t" + viewportVisableHeight() + "\n";
        r += "\t\tMargin Left:\t\t\t" + viewportMarginLeft() + "\n";
        r += "\t\tMargin Top:\t\t\t" + viewportMarginTop() + "\n";
        r += "\t\tMargin Right:\t\t\t" + viewportMarginRight() + "\n";
        r += "\t\tMargin Bottom:\t\t\t" + viewportMarginBottom() + "\n";

        r += "\n\tBitmapDescriptor@(" + getBitmapDescriptorStart() + ")\n";
        r += "\t\tFrame Buffer Type:\t\t" + bitmapFramebufferType();
        if (bitmapFramebufferType() == FrameBuffer_YUV8)
            r += " (YUV8)";
        else if (bitmapFramebufferType() == FrameBuffer_PAL8)
            r += " (PAL8)";
        r += "\n";
        r += "\t\tData Start:\t\t\t" + bitmapDataStart();
        if (bitmapDataStart() == 0)
            r += " (No Image)";
        else
            r += "[Valid Image]";
        r += "\n";
        r += "\t\tBuffer Width:\t\t\t" + bitmapBufferWidth() + "\n";
        r += "\t\tBuffer Visable Width:\t\t" + bitmapVisableWidth() + "\n";
        r += "\t\tBuffer Visable Height:\t\t" + bitmapVisableHeight() + "\n";
        r += "\t\tMargin Left:\t\t\t" + bitmapMarginLeft() + "\n";
        r += "\t\tMargin Top:\t\t\t" + bitmapMarginTop() + "\n";
        r += "\t\tMargin Right:\t\t\t" + bitmapMarginRight() + "\n";
        r += "\t\tMargin Bottom:\t\t\t" + bitmapMarginBottom() + "\n\n";
        return r;
    }

    // field locations for header
    private static final int iViewDataHeaderVersionNumberMajor = 0;
    private static final int iViewDataHeaderVersionNumberMinor = 4;
    private static final int iViewDataHeaderLCDAspectRatio = 8;
    private static final int iViewDataHeaderPalleteType = 12;
    private static final int iViewDataHeaderPalleteDataStart = 16; // So they
    // can change
    // protocol
    // slightly
    // by adding
    // more
    // metadata
    // and still
    // have old
    // software
    // find the
    // sections
    // right.
    // cool.
    private static final int iViewDataHeaderViewportDescriptorStart = 20; // in-chdk.ptp.java.connection.packet
    // pointer
    // to
    // viewport
    // descriptor
    // (ui
    // overlay)
    private static final int iViewDataHeaderBitmapDescriptorStart = 24; // in-chdk.ptp.java.connection.packet-pointer

    // to
    // bitmap
    // descriptor
    // (image)

    public int getMajorVersion() {
        return this.decodeInt(iViewDataHeaderVersionNumberMajor);
    }

    public int getMinorVersion() {
        return this.decodeInt(iViewDataHeaderVersionNumberMinor);
    }

    public int getLCDAspectRatio() {
        return this.decodeInt(iViewDataHeaderLCDAspectRatio);
    }

    public int getPalleteType() {
        return this.decodeInt(iViewDataHeaderPalleteType);
    }

    public int getPaletteDataStart() {
        return this.decodeInt(iViewDataHeaderPalleteDataStart);
    }

    public int getViewportDescriptorStart() {
        return this.decodeInt(iViewDataHeaderViewportDescriptorStart);
    }

    public int getBitmapDescriptorStart() {
        return this.decodeInt(iViewDataHeaderBitmapDescriptorStart);
    }

    private static final int iDescriptorFrameBufferType = 0;
    private static final int iDescriptorFrameBufferDataStart = 4;
    private static final int iDescriptorFrameBufferWidth = 8;
    private static final int iDescriptorFrameBufferVisibleWidth = 12;
    private static final int iDescriptorFrameBufferVisibleHeight = 16;
    private static final int iDescriptorFrameBufferMarginLeft = 20;
    private static final int iDescriptorFrameBufferMarginTop = 24;
    private static final int iDescriptorFrameBufferMarginRight = 28;
    private static final int iDescriptorFrameBufferMarginBottom = 32;

    // Viewport Metadata
    public int viewportFramebufferType() {
        return this.decodeInt(iDescriptorFrameBufferType
                + getViewportDescriptorStart());
    }

    public int viewportDataStart() {
        return this.decodeInt(iDescriptorFrameBufferDataStart
                + getViewportDescriptorStart());
    }

    /*
     * buffer width in pixels data size is always
     * buffer_width*visible_height*(buffer bpp based on type)
     */
    public int viewportBufferWidth() {
        return this.decodeInt(iDescriptorFrameBufferWidth
                + getViewportDescriptorStart());
    }

    /*
     * visible size in pixels describes data within the buffer which contains
     * image data to be displayed any offsets within buffer data are added
     * before sending, so the top left pixel is always the first first byte of
     * data. width must always be <= buffer_width if buffer_width is > width,
     * the additional data should be skipped visible_height also defines the
     * number of data rows
     */
    public int viewportVisableWidth() {
        return this.decodeInt(iDescriptorFrameBufferVisibleWidth
                + getViewportDescriptorStart());
    }

    public int viewportVisableHeight() {
        return this.decodeInt(iDescriptorFrameBufferVisibleHeight
                + getViewportDescriptorStart());
    }

    /*
     * margins pixels offsets needed to replicate display position on cameras
     * screen not used for any buffer offsets
     */
    public int viewportMarginLeft() {
        return this.decodeInt(iDescriptorFrameBufferMarginLeft
                + getViewportDescriptorStart());
    }

    public int viewportMarginTop() {
        return this.decodeInt(iDescriptorFrameBufferMarginTop
                + getViewportDescriptorStart());
    }

    public int viewportMarginRight() {
        return this.decodeInt(iDescriptorFrameBufferMarginRight
                + getViewportDescriptorStart());
    }

    public int viewportMarginBottom() {
        return this.decodeInt(iDescriptorFrameBufferMarginBottom
                + getViewportDescriptorStart());
    }

    // Bitmap Metadata
    public int bitmapFramebufferType() {
        return this.decodeInt(iDescriptorFrameBufferType
                + getBitmapDescriptorStart());
    }

    public int bitmapDataStart() {
        return this.decodeInt(iDescriptorFrameBufferDataStart
                + getBitmapDescriptorStart());
    }

    /*
     * buffer width in pixels data size is always
     * buffer_width*visible_height*(buffer bpp based on type)
     */
    public int bitmapBufferWidth() {
        return this.decodeInt(iDescriptorFrameBufferWidth
                + getBitmapDescriptorStart());
    }

    /*
     * visible size in pixels describes data within the buffer which contains
     * image data to be displayed any offsets within buffer data are added
     * before sending, so the top left pixel is always the first first byte of
     * data. width must always be <= buffer_width if buffer_width is > width,
     * the additional data should be skipped visible_height also defines the
     * number of data rows
     */
    public int bitmapVisableWidth() {
        return this.decodeInt(iDescriptorFrameBufferVisibleWidth
                + getBitmapDescriptorStart());
    }

    public int bitmapVisableHeight() {
        return this.decodeInt(iDescriptorFrameBufferVisibleHeight
                + getBitmapDescriptorStart());
    }

    /*
     * margins pixels offsets needed to replicate display position on cameras
     * screen not used for any buffer offsets
     */
    public int bitmapMarginLeft() {
        return this.decodeInt(iDescriptorFrameBufferMarginLeft
                + getBitmapDescriptorStart());
    }

    public int bitmapMarginTop() {
        return this.decodeInt(iDescriptorFrameBufferMarginTop
                + getBitmapDescriptorStart());
    }

    public int bitmapMarginRight() {
        return this.decodeInt(iDescriptorFrameBufferMarginRight
                + getBitmapDescriptorStart());
    }

    public int bitmapMarginBottom() {
        return this.decodeInt(iDescriptorFrameBufferMarginBottom
                + getBitmapDescriptorStart());
    }

    public byte clip_yuv(int v) {
        return (byte) v;
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

    private int decodeInt(int pos) {
        int b1 = 0x000000ff & buf[pos];
        int b2 = 0x000000ff & buf[pos + 1];
        int b3 = 0x000000ff & buf[pos + 2];
        int b4 = 0x000000ff & buf[pos + 3];
        int r = ((b4 << 24) | (b3 << 16) | (b2 << 8) | (b1 << 0));
        return r;
    }
}
