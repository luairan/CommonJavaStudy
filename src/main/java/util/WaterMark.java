package util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author luairan
 */

public class WaterMark {
    private static final Font font = loadFont();
    private static final int width = 200;
    private static final int height = 100;

    private static Font loadFont() {
        try (java.io.InputStream in = WaterMark.class.getClassLoader().getResourceAsStream("msyh.ttf")) {
            if (in != null) return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.PLAIN, 13);
        } catch (IOException | FontFormatException e) {
            // Fall back to a logical font when the optional bundled font is unavailable.
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    public static byte[] doWaterMark(String str) throws IOException {

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g2 = bi.createGraphics();

        try {
        g2.rotate(Math.atan((double) width / height) + Math.PI * 3 / 2, width / 2, height / 2);
        g2.setPaint(new Color(234, 234, 234));
        if (font != null)
            g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontRenderContext context = g2.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(str, context);
        double x = (width - bounds.getWidth()) / 2;
        double y = (height - bounds.getHeight()) / 2;
        double ascent = -bounds.getY();
        double baseY = y + ascent;

        g2.drawString(str, (int) x, (int) baseY);

        } finally {
            g2.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageIO.write(bi, "png", baos);
        baos.flush();
        return baos.toByteArray();
    }
}
