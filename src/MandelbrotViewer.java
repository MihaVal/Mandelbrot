import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MandelbrotViewer extends JFrame {
    private int width = 800;
    private int height = 600;
    private static final int MAX_ITER = 50;
    private static final int TARGET_FPS = 60;

    private double xMin = -2.0, xMax = 1.0;
    private double yMin = -1.2, yMax = 1.2;
    private double zoomFactor = 0.8;
    private double panSpeed;
    private BufferedImage image;
    private boolean rendering = false;

    public MandelbrotViewer() {
        setTitle("Mandelbrot Set Viewer");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        panSpeed = 0.1 * (xMax - xMin);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: // Movements
                        xMin -= panSpeed;
                        xMax -= panSpeed;
                        break;
                    case KeyEvent.VK_RIGHT:
                        xMin += panSpeed;
                        xMax += panSpeed;
                        break;
                    case KeyEvent.VK_UP:
                        yMin -= panSpeed;
                        yMax -= panSpeed;
                        break;
                    case KeyEvent.VK_DOWN:
                        yMin += panSpeed;
                        yMax += panSpeed;
                        break;
                    case KeyEvent.VK_1: // Zoom in
                        zoom(zoomFactor);
                        break;
                    case KeyEvent.VK_2: // Zoom out
                        zoom(1 / zoomFactor);
                        break;
                    case KeyEvent.VK_S: // Save
                        saveImage();
                        break;
                }
                startRendering();
            }
        });


        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                width = getWidth();
                height = getHeight();
                startRendering();
            }
        });

        startRendering();
    }

    private void zoom(double factor) {
        double xCenter = (xMin + xMax) / 2;
        double yCenter = (yMin + yMax) / 2;
        double xRange = (xMax - xMin) * factor;
        double yRange = (yMax - yMin) * factor;
        xMin = xCenter - xRange / 2;
        xMax = xCenter + xRange / 2;
        yMin = yCenter - yRange / 2;
        yMax = yCenter + yRange / 2;
        panSpeed = 0.1 * (xMax - xMin); // Adjust the pan speed for diff zoom levels
    }

    private void startRendering() {
        if (rendering) return;

        rendering = true;
        new Thread(() -> {
            renderMandelbrot();
            rendering = false;
            repaint();
        }).start();
    }

    private void renderMandelbrot() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double real = xMin + x * (xMax - xMin) / width;
                double imag = yMin + y * (yMax - yMin) / height;
                int color = computePoint(new Complex(real, imag));
                image.setRGB(x, y, color);
            }
        }
    }

    private int computePoint(Complex c) {
        Complex z = new Complex(0, 0);
        int n = 0;

        while (z.abs() <= 2 && n < MAX_ITER) {
            z = z.multiply(z).add(c);
            n++;
        }

        if (n == MAX_ITER) {
            return Color.BLACK.getRGB(); // black for inside of the set
        }

        // color gradient
        float hue = 0.7f + (float) n / MAX_ITER;
        float saturation = 1.0f;
        float brightness = n < MAX_ITER ? 1.0f : 0.0f;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    private void saveImage() {
        try {
            File outputfile = new File("mandelbrot.png");
            ImageIO.write(image, "png", outputfile);
            Logger.log("Image saved to " + outputfile.getAbsolutePath(), LogLevel.Success);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("Failed to save the image.", LogLevel.Error);
        }
    }

    @Override
    public void paint(Graphics g) {
        long frameStartTime = System.currentTimeMillis();
        super.paint(g);
        g.drawImage(image, 0, 0, null);
        long frameEndTime = System.currentTimeMillis();

        // Limit to 60 FPS
        long frameDuration = frameEndTime - frameStartTime;
        long targetFrameDuration = 1000 / TARGET_FPS;
        if (frameDuration < targetFrameDuration) {
            try {
                Thread.sleep(targetFrameDuration - frameDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MandelbrotViewer viewer = new MandelbrotViewer();
            viewer.setVisible(true);
        });
    }

    static class Complex {
        private final double real;
        private final double imag;

        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        public Complex add(Complex other) {
            return new Complex(this.real + other.real, this.imag + other.imag);
        }

        public Complex multiply(Complex other) {
            return new Complex(
                    this.real * other.real - this.imag * other.imag,
                    this.real * other.imag + this.imag * other.real
            );
        }

        public double abs() {
            return Math.sqrt(real * real + imag * imag);
        }
    }
}
