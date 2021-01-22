package louish.scan_process;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * A list of images. Methods are provided for modifying the images' brightness
 * and contrast, for getting and deleting images, and for checking whether the
 * images have been edited.
 * 
 * @author Louis Hildebrand
 */
public class OrderedImages
{
	private static double RED_WEIGHT = 0.299;
	private static double GREEN_WEIGHT = 0.587;
	private static double BLUE_WEIGHT = 0.114;
	private static double MIN_BRIGHTNESS = 0.25;
	private static double MAX_BRIGHTNESS = 1.15;
	private static double MIN_CONTRAST = 0.5;
	private static double MAX_CONTRAST = 3;

	private ArrayList<BufferedImage> originalImages;
	private ArrayList<BufferedImage> editedImages;
	private ArrayList<Double> initBrightness;
	private ArrayList<Double> initContrast;
	private ArrayList<Double> currentBrightness;
	private ArrayList<Double> currentContrast;
	private boolean edited;

	/**
	 * Instantiates the list of images, measuring their brightness and contrast and
	 * saving it in the appropriate lists. This method is slow and places a task on
	 * the EDT, so it must not be called from the EDT itself.
	 * 
	 * @param pageFiles Files containing the images to be saved
	 * @param frame     The JFrame on which to center the progress bar
	 * @throws IOException
	 */
	public OrderedImages(ArrayList<File> pageFiles, JFrame frame) throws IOException
	{
		// Set up progress bar
		JFrame progressFrame = new JFrame();
		JProgressBar progressBar = new JProgressBar(0, pageFiles.size());
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					progressFrame.setSize(250, 75);
					progressFrame.setLocationRelativeTo(frame);
					progressFrame.setUndecorated(true);
					progressFrame.setResizable(false);
					progressFrame.setResizable(false);
					JPanel progressPanel = new JPanel();
					progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
					progressFrame.add(progressPanel);
					progressPanel.add(Box.createVerticalGlue());
					JLabel msg = new JLabel("Loading in images...");
					msg.setAlignmentX(Component.CENTER_ALIGNMENT);
					msg.setFont(ScanProcessor.TEXT_FONT);
					progressPanel.add(msg);
					progressBar.setFont(ScanProcessor.TEXT_FONT);
					progressBar.setValue(0);
					progressBar.setString("0/" + pageFiles.size());
					progressBar.setStringPainted(true);
					progressPanel.add(progressBar);
					progressPanel.add(Box.createVerticalGlue());
					progressFrame.setVisible(true);
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
		}

		originalImages = new ArrayList<BufferedImage>();
		editedImages = new ArrayList<BufferedImage>();
		initBrightness = new ArrayList<Double>();
		initContrast = new ArrayList<Double>();
		currentBrightness = new ArrayList<Double>();
		currentContrast = new ArrayList<Double>();
		edited = false;

		for (int i = 0; i < pageFiles.size(); i++)
		{
			BufferedImage img = ImageIO.read(pageFiles.get(i));
			originalImages.add(img);
			editedImages.add(copyImage(img));
			double[] properties = getImageProperties(img);
			initBrightness.add(properties[0]);
			currentBrightness.add(properties[0]);
			initContrast.add(properties[1]);
			currentContrast.add(properties[1]);
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + pageFiles.size());
		}

		progressFrame.dispose();
	}

	/**
	 * Creates a deep copy of the given image. Written by Klark:
	 * https://stackoverflow.com/a/3514297/12314816
	 * 
	 * @param bi The image to be copied
	 * @return A deep copy of the given image
	 */
	private static BufferedImage copyImage(BufferedImage bi)
	{
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	/**
	 * Maps the given number linearly from the range [oldMin, oldMax] to the range
	 * [newMin, newMax]
	 * 
	 * @param num    The number to be mapped
	 * @param oldMin The lower bound of the input range
	 * @param oldMax The upper bound of the input range
	 * @param newMin The lower bound of the output range
	 * @param newMax The upper bound of the output range
	 * @return The mapped number
	 */
	private static double map(double num, double oldMin, double oldMax, double newMin, double newMax)
	{
		return newMin + (num - oldMin) / (oldMax - oldMin) * (newMax - newMin);
	}

	/**
	 * Maps the given number from the input range to the output range such that
	 * oldMin becomes newMin, oldMax becomes newMax, and 0 becomes newMidpoint.
	 * 
	 * @param num         The number to be mapped
	 * @param oldMin      The lower bound of the input range
	 * @param oldMax      The upper bound of the input range
	 * @param newMin      The lower bound of the output range
	 * @param newMax      The upper bound of the output range
	 * @param newMidpoint The target value for the input 0
	 * @return The mapped number
	 */
	private static int piecewiseLinearMap(double num, double oldMin, double oldMax, double newMin, double newMax, double newMidpoint)
	{
		if (num < 0)
			return (int) Math.round(map(num, oldMin, 0, newMin, newMidpoint));
		else
			return (int) Math.round(map(num, 0, oldMax, newMidpoint, newMax));
	}

	/**
	 * Measures the brightness and contrast of the given image. Image brightness is
	 * taken to be the mean of the brightness of all pixels in the image, while
	 * image contrast is taken to be the standard deviation of the pixel
	 * brightnesses.
	 * 
	 * @param img The image in which to measure brightness and contrast
	 * @return An array whose first element is the brightness and whose second
	 *         element is the contrast
	 */
	private double[] getImageProperties(BufferedImage img)
	{
		int width = img.getWidth();
		int height = img.getHeight();
		double currentBrightness;
		double sumBrightness = 0;
		double sumBrightnessSquared = 0;

		for (int r = 0; r < height; r++)
		{
			for (int c = 0; c < width; c++)
			{
				currentBrightness = getPixelBrightness(img.getRGB(c, r));
				sumBrightness += currentBrightness;
				sumBrightnessSquared += currentBrightness * currentBrightness;
			}
		}

		double mean = sumBrightness / (width * height);
		double variance = sumBrightnessSquared / (width * height) - mean * mean;
		return new double[] { mean, Math.sqrt(variance) };
	}

	/**
	 * Calculates the brightness of the given pixel using the NTSC formula: Y =
	 * 0.299*R + 0.587*G + 0.114*B
	 * 
	 * @param pixel The pixel whose brightness is to be calculated (in ARGB format)
	 * @return The brightness of the pixel, in the range [0, 255]
	 */
	private double getPixelBrightness(int pixel)
	{
		int r = (pixel >>> 16) & 0xFF;
		int g = (pixel >>> 8) & 0xFF;
		int b = pixel & 0xFF;

		return RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b;
	}

	/**
	 * Sets the brightness of all images.
	 * 
	 * @param brightness The desired brightness (in the range [-100, 100],
	 *                   representing a change relative to the current value)
	 * @param frame      The window on which to center the progress bar
	 */
	public void setBrightness(int brightness, JFrame frame)
	{
		// Set up progress bar
		JFrame progressFrame = new JFrame();
		JProgressBar progressBar = new JProgressBar(0, editedImages.size());
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					progressFrame.setSize(250, 75);
					progressFrame.setLocationRelativeTo(frame);
					progressFrame.setUndecorated(true);
					progressFrame.setResizable(false);
					progressFrame.setResizable(false);
					JPanel progressPanel = new JPanel();
					progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
					progressFrame.add(progressPanel);
					progressPanel.add(Box.createVerticalGlue());
					JLabel msg = new JLabel("Setting brightness to " + brightness + "...");
					msg.setAlignmentX(Component.CENTER_ALIGNMENT);
					msg.setFont(ScanProcessor.TEXT_FONT);
					progressPanel.add(msg);
					progressBar.setFont(ScanProcessor.TEXT_FONT);
					progressBar.setValue(0);
					progressBar.setString("0/" + editedImages.size());
					progressBar.setStringPainted(true);
					progressPanel.add(progressBar);
					progressPanel.add(Box.createVerticalGlue());
					progressFrame.setVisible(true);
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
		}

		edited = true;
		int adjustedBrightness;
		for (int i = 0; i < editedImages.size(); i++)
		{
			// Map the brightness from [-100, 100] to [min * initial brightness, max * initial brightness],
			// with 0 being the initial value
			adjustedBrightness = piecewiseLinearMap(brightness, -100, 100, MIN_BRIGHTNESS * initBrightness.get(i),
					MAX_BRIGHTNESS * initBrightness.get(i), initBrightness.get(i));
			setImageBrightness(i, adjustedBrightness);
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + editedImages.size());
		}

		progressFrame.dispose();
	}

	/**
	 * Attempts to set the given image's brightness to the specified value. This is
	 * achieved by adding a constant offset to the brightness of each pixel in the
	 * image. The new brightness should be in the range [0, 255], but this is not
	 * strictly enforced. The actual resulting brightness may not be exactly as
	 * desired due to limits on the brightness of individual pixels, and this method
	 * does not work well for colour images.
	 * 
	 * @param imgIndex      The index of the image to be modified
	 * @param newBrightness The desired new brightness
	 */
	private void setImageBrightness(int imgIndex, int newBrightness)
	{
		BufferedImage img = editedImages.get(imgIndex);
		double oldBrightness = currentBrightness.get(imgIndex);
		int offset = (int) Math.round(newBrightness - oldBrightness);
		int newPixel;

		for (int r = 0; r < img.getHeight(); r++)
		{
			for (int c = 0; c < img.getWidth(); c++)
			{
				newPixel = offsetPixelBrightness(img.getRGB(c, r), offset);
				img.setRGB(c, r, newPixel);
			}
		}

		currentBrightness.set(imgIndex, (double) newBrightness);
	}

	/**
	 * Modifies the given pixel such that the new brightness is increased (for
	 * positive values) or decreased (for negative values) by the specified offset.
	 * The method attempts to preserve the R:G:B ratio, but will strictly limit each
	 * value to the range [0, 255]. The alpha value is not changed.
	 * 
	 * @param pixel  The pixel to be modified (in ARGB format)
	 * @param offset The amount by which to increase or decrease the pixel's
	 *               brightness
	 * @return A new ARGB pixel with the desired brightness
	 */
	private int offsetPixelBrightness(int pixel, int offset)
	{
		// Calculate by hand instead of using getPixelBrightness() because the individual R, G, B values are needed
		int alpha = (pixel >>> 24) & 0xFF;
		int r = (pixel >>> 16) & 0xFF;
		int g = (pixel >>> 8) & 0xFF;
		int b = pixel & 0xFF;
		double initialBrightness = RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b;

		// Non-black pixel: scale R, G, and B to get the required brightness
		if (initialBrightness != 0)
		{
			double scaleFactor = (initialBrightness + offset) / initialBrightness;
			r *= scaleFactor;
			g *= scaleFactor;
			b *= scaleFactor;
		}
		// Pure black pixel: just add offset to R, G, and B
		else
		{
			r = offset;
			g = offset;
			b = offset;
		}

		// Ensure that the new R, G, B values are valid
		if (r < 0)
			r = 0;
		else if (r > 255)
			r = 255;
		if (g < 0)
			g = 0;
		else if (g > 255)
			g = 255;
		if (b < 0)
			b = 0;
		else if (b > 255)
			b = 255;

		// Create new pixel with the same alpha as before
		int newPixel = b;
		newPixel = newPixel | (g << 8);
		newPixel = newPixel | (r << 16);
		newPixel = newPixel | (alpha << 24);
		return newPixel;
	}

	/**
	 * Sets the contrast of all images.
	 * 
	 * @param contrast The desired contrast (in the range [-100, 100], representing
	 *                 a change relative to the current value)
	 * @param frame    The window on which to center the progress bar
	 */
	public void setContrast(int contrast, JFrame frame)
	{
		// Set up progress bar
		JFrame progressFrame = new JFrame();
		JProgressBar progressBar = new JProgressBar(0, editedImages.size());
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					progressFrame.setSize(250, 75);
					progressFrame.setLocationRelativeTo(frame);
					progressFrame.setUndecorated(true);
					progressFrame.setResizable(false);
					progressFrame.setResizable(false);
					JPanel progressPanel = new JPanel();
					progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
					progressFrame.add(progressPanel);
					progressPanel.add(Box.createVerticalGlue());
					JLabel msg = new JLabel("Setting contrast to " + contrast + "...");
					msg.setAlignmentX(Component.CENTER_ALIGNMENT);
					msg.setFont(ScanProcessor.TEXT_FONT);
					progressPanel.add(msg);
					progressBar.setFont(ScanProcessor.TEXT_FONT);
					progressBar.setValue(0);
					progressBar.setString("0/" + editedImages.size());
					progressBar.setStringPainted(true);
					progressPanel.add(progressBar);
					progressPanel.add(Box.createVerticalGlue());
					progressFrame.setVisible(true);
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			ScanProcessor.showErrorMessage("Error: progress interrupted");
			return;
		}

		edited = true;
		int adjustedContrast;
		for (int i = 0; i < originalImages.size(); i++)
		{
			// Map the contrast from [-100, 100] to [min * initial contrast, max * initial contrast],
			// with 0 being the initial value
			adjustedContrast = piecewiseLinearMap(contrast, -100, 100, MIN_CONTRAST * initContrast.get(i), MAX_CONTRAST * initContrast.get(i),
					initContrast.get(i));
			setImageContrast(i, adjustedContrast);
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + originalImages.size());
		}

		progressFrame.dispose();
	}

	/**
	 * Attempts to set the given image's contrast to the specified value. This is
	 * achieved by scaling the deviation in brightness from the mean by a constant
	 * factor for every pixel in the image. The new contrast should be non-negative,
	 * but this is not strictly enforced. The actual resulting contrast may not be
	 * exactly as desired due to limits on the brightness of individual pixels, and
	 * the method does not work well for colour images.
	 * 
	 * @param imgIndex    The index of the image to be modified
	 * @param newContrast The desired new contrast (which should be positive)
	 */
	private void setImageContrast(int imgIndex, int newContrast)
	{
		BufferedImage img = editedImages.get(imgIndex);
		double avgBrightness = currentBrightness.get(imgIndex);
		double oldContrast = currentContrast.get(imgIndex);
		double scaleFactor = newContrast / oldContrast;

		int newPixel;
		for (int r = 0; r < img.getHeight(); r++)
		{
			for (int c = 0; c < img.getWidth(); c++)
			{
				newPixel = scalePixelContrast(img.getRGB(c, r), scaleFactor, avgBrightness);
				img.setRGB(c, r, newPixel);
			}
		}

		currentContrast.set(imgIndex, (double) newContrast);
	}

	/**
	 * Modifies the brightness of the given pixel such that its deviation from the
	 * mean is scaled by the specified factor. The method attempts to preserve the
	 * R:G:B ratio, but will strictly limit each value to the range [0, 255]. The
	 * alpha value is not changed.
	 * 
	 * @param pixel  The pixel to be modified (in ARGB format)
	 * @param factor The factor by which the deviation in brightness from the mean
	 *               should be scaled
	 * @param mean   The mean brightness in the image
	 * @return A new ARGB pixel with the desired brightness
	 */
	private int scalePixelContrast(int pixel, double factor, double mean)
	{
		// Calculate by hand instead of using getPixelBrightness() because the individual R, G, B values are needed
		int alpha = (pixel >>> 24) & 0xFF;
		int r = (pixel >>> 16) & 0xFF;
		int g = (pixel >>> 8) & 0xFF;
		int b = pixel & 0xFF;
		double initialBrightness = RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b;
		double newBrightness = mean + factor * (initialBrightness - mean);

		if (initialBrightness != 0)
		{
			double scaleFactor = newBrightness / initialBrightness;
			r *= scaleFactor;
			g *= scaleFactor;
			b *= scaleFactor;
		}
		else
		{
			r = (int) newBrightness;
			g = (int) newBrightness;
			b = (int) newBrightness;
		}

		// Ensure that the new R, G, B values are valid
		if (r < 0)
			r = 0;
		else if (r > 255)
			r = 255;
		if (g < 0)
			g = 0;
		else if (g > 255)
			g = 255;
		if (b < 0)
			b = 0;
		else if (b > 255)
			b = 255;

		// Create new pixel with the same alpha as before
		int newPixel = b;
		newPixel = newPixel | (g << 8);
		newPixel = newPixel | (r << 16);
		newPixel = newPixel | (alpha << 24);
		return newPixel;
	}

	/**
	 * Reverts all images to their unedited state by getting a deep copy of the list
	 * of original images and sets the nominal brightness and contrast of all images
	 * to their default values. Calling get() and getOriginal() immediately after
	 * reset() will yield identical results.
	 * 
	 * The method's progress is indicated by a progress bar, which will be centered
	 * on the given JFrame.
	 * 
	 * @param frame The window on which to center the progress bar
	 */
	public void reset(JFrame frame)
	{
		// Set up progress bar
		JFrame progressFrame = new JFrame();
		JProgressBar progressBar = new JProgressBar(0, editedImages.size());
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					progressFrame.setSize(250, 75);
					progressFrame.setLocationRelativeTo(frame);
					progressFrame.setUndecorated(true);
					progressFrame.setResizable(false);
					progressFrame.setResizable(false);
					JPanel progressPanel = new JPanel();
					progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
					progressFrame.add(progressPanel);
					progressPanel.add(Box.createVerticalGlue());
					JLabel msg = new JLabel("Resetting brightness and contrast...");
					msg.setAlignmentX(Component.CENTER_ALIGNMENT);
					msg.setFont(ScanProcessor.TEXT_FONT);
					progressPanel.add(msg);
					progressBar.setFont(ScanProcessor.TEXT_FONT);
					progressBar.setValue(0);
					progressBar.setString("0/" + editedImages.size());
					progressBar.setStringPainted(true);
					progressPanel.add(progressBar);
					progressPanel.add(Box.createVerticalGlue());
					progressFrame.setVisible(true);
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
		}

		edited = false;

		for (int i = 0; i < originalImages.size(); i++)
		{
			editedImages.set(i, copyImage(originalImages.get(i)));
			currentBrightness.set(i, initBrightness.get(i));
			currentContrast.set(i, initContrast.get(i));
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + progressBar.getMaximum());
		}

		progressFrame.dispose();
	}

	/**
	 * Sets the current brightness and contrast values as the default and sets the
	 * current list of edited images as the originals.
	 * 
	 * Changes in brightness or contrast will now be measured relative to these new
	 * values, and getOriginal() will return images from this new set of images.
	 */
	public void setDefault()
	{
		edited = false;
		for (int i = 0; i < originalImages.size(); i++)
		{
			originalImages.set(i, copyImage(editedImages.get(i)));
			initBrightness.set(i, currentBrightness.get(i));
			initContrast.set(i, currentContrast.get(i));
		}
	}

	/**
	 * @param index The index of the desired image
	 * @return The BufferedIndex at the given index in the list of edited images
	 */
	public BufferedImage get(int index)
	{
		return editedImages.get(index);
	}

	/**
	 * @param index The index of the desired image
	 * @return The BufferedIndex at the given index in the list of original images
	 */
	public BufferedImage getOriginal(int index)
	{
		return originalImages.get(index);
	}

	/**
	 * Deletes the image at the given index.
	 * 
	 * @param index The index of the image to be deleted
	 */
	public void delete(int index)
	{
		originalImages.remove(index);
		editedImages.remove(index);
		initBrightness.remove(index);
		initContrast.remove(index);
		currentBrightness.remove(index);
		currentContrast.remove(index);
	}

	/**
	 * Checks if the images have been edited. Any change to the brightness or
	 * contrast will make this TRUE, but using reset() will make it FALSE again
	 * 
	 * @return TRUE if the images have been edited and FALSE otherwise
	 */
	public boolean isEdited()
	{
		return edited;
	}
}
