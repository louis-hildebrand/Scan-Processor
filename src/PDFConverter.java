import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import com.itextpdf.io.IOException;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

/**
 * Utility class for converting files to PDF. This is separate from
 * ScanProcessor to avoid the name collision between java.awt.Image and
 * com.itextpdf.layout.element.Image.
 * 
 * @author Louis Hildebrand
 */
public class PDFConverter
{
	public static int SUCCESS = 0;
	public static int CANCELLED = 1;
	public static int IO_ERROR = 2;
	public static int RENAME_FAIL = 3;
	public static String TEMP_OUT_DIR =
			System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local" + File.separator + "Temp";

	/**
	 * Writes all the given files to one PDF. The PDF is initially saved in
	 * TEMP_OUT_DIR, and if the conversion is successful the file is moved to the
	 * intended location. An integer is returned indicating the status of the PDF.
	 * 
	 * @param images          The files to be converted to PDF
	 * @param outputDirectory The directory in which to save the PDF
	 * @param pdfName         The name for the PDF
	 * @param progressBar     A progress bar to track the method's progress
	 * @return A code indicating the status of the PDF (successfully saved and
	 *         renamed, successfully saved but not renamed, failed to save due to IO
	 *         error, failed to save due to cancellation)
	 */
	public static int convertToPDF(ArrayList<File> images, File outputDirectory, String pdfName, JProgressBar progressBar)
	{
		String pdfOut = outputDirectory.getAbsolutePath() + File.separator + pdfName + ".pdf";

		if (new File(pdfOut).exists())
		{
			String msg = pdfOut + " already exists. Would you like to overwrite it?";
			int choice = JOptionPane.showOptionDialog(null, msg, "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					null, null);
			if (choice == JOptionPane.NO_OPTION)
				return CANCELLED;
		}

		// Initially save the PDF in the Temp folder and move it to the intended destination if the conversion is successful
		String tempOut = TEMP_OUT_DIR + File.separator + "scan_processor_";
		int k = 0;
		while (new File(tempOut + k + ".pdf").exists())
		{
			k++;
		}
		tempOut = tempOut + k + ".pdf";

		try
		{
			PdfWriter pw = new PdfWriter(tempOut);
			PdfDocument pdfDoc = new PdfDocument(pw);
			Document doc = new Document(pdfDoc, PageSize.LETTER);
			doc.setMargins(0, 0, 0, 0);

			Image img;
			for (int i = 0; i < images.size(); i++)
			{
				img = new Image(ImageDataFactory.create(images.get(i).getAbsolutePath()));
				doc.add(img);
				progressBar.setValue(i + 1);
				progressBar.setString((i + 1) + "/" + progressBar.getMaximum());
			}

			doc.close();

			// Move the file to the intended destination
			File dest = new File(pdfOut);
			if (dest.exists())
				dest.delete();
			boolean renameSuccess = new File(tempOut).renameTo(dest);
			if (renameSuccess)
				return SUCCESS;
			else
				return RENAME_FAIL;
		}
		catch (FileNotFoundException | MalformedURLException | IOException e)
		{
			new File(tempOut).delete();
			return IO_ERROR;
		}
	}
}
