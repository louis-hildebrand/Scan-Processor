
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// TODO Check if PDF name is valid
// TODO Bug when renaming multiple files whose initial numbers don't start at 1?

/**
 * Main class in the program. It sets up the GUI and handles all events.
 * 
 * @author Louis Hildebrand
 */
public class ScanProcessor implements ActionListener, ChangeListener, FocusListener, ItemListener
{
	// Constants
	private static final int WIN_WIDTH = 1150;
	private static final int WIN_HEIGHT = 800;
	public static final int MAX_IMG_WIDTH = 527;
	public static final int MAX_IMG_HEIGHT = 682;
	private static final int ZOOMED_PANE_WIDTH = 1050;
	private static final int ZOOMED_PANE_HEIGHT = 800;
	private static final int ZOOMED_IMG_WIDTH = 1015;
	private static final int ZOOMED_IMG_HEIGHT = 740;
	private static final Color OPTION_PANE_COLOR = Color.DARK_GRAY;
	private static final Color IMG_PANE_COLOR = Color.GRAY;
	private static final Color IMG_PREVIEW_BACKGROUND = Color.BLACK;
	private static final Color TEXT_FIELD_COLOR = Color.GRAY;
	private static final Color TEXT_COLOR = Color.WHITE;
	private static final Color ERROR_TEXT_COLOR = Color.RED;
	private static final Font TITLE_FONT = new Font("Dialog", Font.BOLD, 18);
	public static final Font TEXT_FONT = new Font("Dialog", Font.PLAIN, 16);
	private static final int IN_FORMAT_TEXT_WIDTH = 550;
	private static final int OUT_FORMAT_TEXT_WIDTH = 550;
	private static final int SPACING = 20;
	private static final int DIR_NAME_WIDTH = 550;
	private static final int DIR_NAME_HEIGHT = 25;
	private static final int BOT_SPACING = 400;
	private static final int IMG_EDIT_PANEL_WIDTH = 550;
	private static final int VALUE_LABEL_WIDTH = 60;
	private static final int DEFAULT_BRIGHTNESS = 0;
	private static final int DEFAULT_CONTRAST = 0;
	private static final int PREVIEW_BUTTON_SPACING = 55;
	private static final File DEFAULT_IN_DIR = new File(System.getProperty("user.home") + File.separator + "Pictures");
	private static final String DEFAULT_IN_PREFIX = "img";
	private static final File DEFAULT_OUT_DIR = new File(System.getProperty("user.home") + File.separator + "Pictures");
	private static final String DEFAULT_OUT_PREFIX = "out";
	private static final String BACKUP_OUT_PREFIX = "output";
	private static final String DEFAULT_PDF_NAME = "merged_pages";

	// Active GUI elements
	private JFrame window;
	private JLabel inputDirectoryLabel;
	private JTextField inPrefixText;
	private JTextField minNumberText;
	private JTextField maxNumberText;
	private JCheckBox interlacePages;
	private JCheckBox reverseEvenPages;
	private JLabel outputDirectoryLabel;
	private JTextField outPrefixText;
	private JTextField pdfNameText;
	private JLabel brightnessValueLabel;
	private JSlider brightnessSlider;
	private JLabel contrastValueLabel;
	private JSlider contrastSlider;
	private JButton resetButton;
	private JCheckBox rename;
	private JCheckBox adjustBrightnessAndContrast;
	private JCheckBox convertToPDF;
	private JLabel imagePreviewName;
	private JLabel imagePreview;
	private JButton previewLeftButton;
	private JButton previewRightButton;
	private JToggleButton showOriginal;
	private ArrayList<Component> activeComponents;
	private ArrayList<Component> lockedComponents;

	// State
	private File inputDirectory = DEFAULT_IN_DIR;
	private String inPrefix = DEFAULT_IN_PREFIX;
	private int minFileNum = 1;
	private int maxFileNum = 999;
	private String outPrefix = DEFAULT_OUT_PREFIX;
	private File outputDirectory = DEFAULT_OUT_DIR;
	private String pdfName = DEFAULT_PDF_NAME;
	private int brightness = DEFAULT_BRIGHTNESS;
	private int contrast = DEFAULT_CONTRAST;
	private boolean ignoreStateChange = false;
	private boolean ignoreFocusLost = false;
	private boolean isEditingAllowed = true;
	private int imagePreviewIndex = 0;
	private ArrayList<File> orderedFiles;
	private OrderedImages orderedImages;
	private JFrame progressFrame;

	/**
	 * Creates an instance of ScanProcessor and calls createAndShowGUI() on the EDT
	 * 
	 * @param args Unused arguments
	 */
	public static void main(String[] args)
	{
		ScanProcessor sp = new ScanProcessor();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				sp.createAndShowGUI();
			}
		});
	}

	/**
	 * Sets up the GUI
	 */
	public void createAndShowGUI()
	{
		activeComponents = new ArrayList<Component>();
		lockedComponents = new ArrayList<Component>();

		window = new JFrame("Scan processing");
		window.setSize(WIN_WIDTH, WIN_HEIGHT);
		window.setResizable(false);
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 2));
		window.add(mainPanel);

		// Left panel (settings)
		JPanel optionPanel = new JPanel();
		optionPanel.setBackground(OPTION_PANE_COLOR);
		optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(optionPanel);

		// -- Input directory
		JPanel inDirPanel = new JPanel();
		inDirPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		inDirPanel.setOpaque(false);
		inDirPanel.setLayout(new BoxLayout(inDirPanel, BoxLayout.PAGE_AXIS));
		optionPanel.add(inDirPanel);

		JLabel inDirTitle = new JLabel("Input directory");
		inDirTitle.setFont(TITLE_FONT);
		inDirTitle.setForeground(TEXT_COLOR);
		inDirPanel.add(inDirTitle);

		inputDirectoryLabel = new JLabel();
		inputDirectoryLabel.setOpaque(true);
		inputDirectoryLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		inputDirectoryLabel.setBackground(TEXT_FIELD_COLOR);
		inputDirectoryLabel.setFont(TEXT_FONT);
		inputDirectoryLabel.setForeground(TEXT_COLOR);
		inputDirectoryLabel.setMinimumSize(new Dimension(DIR_NAME_WIDTH, DIR_NAME_HEIGHT));
		inputDirectoryLabel.setMaximumSize(new Dimension(DIR_NAME_WIDTH, DIR_NAME_HEIGHT));
		inDirPanel.add(inputDirectoryLabel);

		JButton inDirSelectButton = new JButton("Change directory");
		inDirSelectButton.setFont(TEXT_FONT);
		inDirSelectButton.setActionCommand("CHANGE_IN_DIR");
		inDirSelectButton.addActionListener(this);
		inDirPanel.add(inDirSelectButton);
		activeComponents.add(inDirSelectButton);

		optionPanel.add(Box.createRigidArea(new Dimension(10, SPACING)));

		// -- Input format
		JPanel inFormatPanel = new JPanel();
		inFormatPanel.setOpaque(false);
		inFormatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		inFormatPanel.setLayout(new BoxLayout(inFormatPanel, BoxLayout.PAGE_AXIS));
		optionPanel.add(inFormatPanel);

		JLabel inFormatLabel = new JLabel("Input format");
		inFormatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		inFormatLabel.setFont(TITLE_FONT);
		inFormatLabel.setForeground(TEXT_COLOR);
		inFormatPanel.add(inFormatLabel);

		JPanel inPrefixPanel = new JPanel();
		inPrefixPanel.setMaximumSize(new Dimension(IN_FORMAT_TEXT_WIDTH, Integer.MAX_VALUE));
		inPrefixPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		inPrefixPanel.setOpaque(false);
		inPrefixPanel.setLayout(new BorderLayout());
		inFormatPanel.add(inPrefixPanel);

		JLabel inPrefixLabel = new JLabel("Prefix: ");
		inPrefixLabel.setFont(TEXT_FONT);
		inPrefixLabel.setForeground(TEXT_COLOR);
		inPrefixPanel.add(inPrefixLabel, BorderLayout.WEST);

		inPrefixText = new JTextField(inPrefix);
		inPrefixText.setActionCommand("UPDATE_IN_PREFIX");
		inPrefixText.addActionListener(this);
		inPrefixText.addFocusListener(this);
		inPrefixText.setBackground(TEXT_FIELD_COLOR);
		inPrefixText.setFont(TEXT_FONT);
		inPrefixText.setForeground(TEXT_COLOR);
		inPrefixPanel.add(inPrefixText);
		activeComponents.add(inPrefixText);

		JPanel minNumberPanel = new JPanel();
		minNumberPanel.setMaximumSize(new Dimension(IN_FORMAT_TEXT_WIDTH, Integer.MAX_VALUE));
		minNumberPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		minNumberPanel.setOpaque(false);
		minNumberPanel.setLayout(new BoxLayout(minNumberPanel, BoxLayout.LINE_AXIS));
		inFormatPanel.add(minNumberPanel);

		JLabel minNumberLabel = new JLabel("Min number: ");
		minNumberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		minNumberLabel.setFont(TEXT_FONT);
		minNumberLabel.setForeground(TEXT_COLOR);
		minNumberPanel.add(minNumberLabel);

		minNumberText = new JTextField(minFileNum);
		minNumberText.setActionCommand("UPDATE_MIN");
		minNumberText.addActionListener(this);
		minNumberText.addFocusListener(this);
		minNumberText.setBackground(TEXT_FIELD_COLOR);
		minNumberText.setFont(TEXT_FONT);
		minNumberText.setForeground(TEXT_COLOR);
		minNumberPanel.add(minNumberText);
		activeComponents.add(minNumberText);

		JPanel maxNumberPanel = new JPanel();
		maxNumberPanel.setMaximumSize(new Dimension(IN_FORMAT_TEXT_WIDTH, Integer.MAX_VALUE));
		maxNumberPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		maxNumberPanel.setOpaque(false);
		maxNumberPanel.setLayout(new BoxLayout(maxNumberPanel, BoxLayout.LINE_AXIS));
		inFormatPanel.add(maxNumberPanel);

		JLabel maxNumberLabel = new JLabel("Max number: ");
		maxNumberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		maxNumberLabel.setFont(TEXT_FONT);
		maxNumberLabel.setForeground(TEXT_COLOR);
		maxNumberPanel.add(maxNumberLabel);

		maxNumberText = new JTextField(maxFileNum);
		maxNumberText.setActionCommand("UPDATE_MAX");
		maxNumberText.addActionListener(this);
		maxNumberText.addFocusListener(this);
		maxNumberText.setBackground(TEXT_FIELD_COLOR);
		maxNumberText.setFont(TEXT_FONT);
		maxNumberText.setForeground(TEXT_COLOR);
		maxNumberPanel.add(maxNumberText);
		activeComponents.add(maxNumberText);

		interlacePages = new JCheckBox("Interlace pages");
		interlacePages.setSelected(true);
		interlacePages.setOpaque(false);
		interlacePages.setAlignmentX(Component.LEFT_ALIGNMENT);
		interlacePages.setFont(TEXT_FONT);
		interlacePages.setForeground(TEXT_COLOR);
		interlacePages.addItemListener(this);
		inFormatPanel.add(interlacePages);
		activeComponents.add(interlacePages);

		reverseEvenPages = new JCheckBox("Reverse even-numbered pages");
		reverseEvenPages.setSelected(true);
		reverseEvenPages.setOpaque(false);
		reverseEvenPages.setAlignmentX(Component.LEFT_ALIGNMENT);
		reverseEvenPages.setFont(TEXT_FONT);
		reverseEvenPages.setForeground(TEXT_COLOR);
		reverseEvenPages.addItemListener(this);
		inFormatPanel.add(reverseEvenPages);
		activeComponents.add(reverseEvenPages);

		optionPanel.add(Box.createRigidArea(new Dimension(10, SPACING)));

		// -- Output directory
		JPanel outDirPanel = new JPanel();
		outDirPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outDirPanel.setOpaque(false);
		outDirPanel.setLayout(new BoxLayout(outDirPanel, BoxLayout.PAGE_AXIS));
		optionPanel.add(outDirPanel);

		JLabel outDirTitle = new JLabel("Output directory");
		outDirTitle.setFont(TITLE_FONT);
		outDirTitle.setForeground(TEXT_COLOR);
		outDirPanel.add(outDirTitle);

		outputDirectoryLabel = new JLabel();
		outputDirectoryLabel.setOpaque(true);
		outputDirectoryLabel.setBackground(TEXT_FIELD_COLOR);
		outputDirectoryLabel.setFont(TEXT_FONT);
		outputDirectoryLabel.setForeground(TEXT_COLOR);
		outputDirectoryLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		outputDirectoryLabel.setMinimumSize(new Dimension(DIR_NAME_WIDTH, DIR_NAME_HEIGHT));
		outputDirectoryLabel.setMaximumSize(new Dimension(DIR_NAME_WIDTH, DIR_NAME_HEIGHT));
		outDirPanel.add(outputDirectoryLabel);
		setOutputDirectory(DEFAULT_OUT_DIR);

		JButton outDirSelectButton = new JButton("Change directory");
		outDirSelectButton.setFont(TEXT_FONT);
		outDirSelectButton.setActionCommand("CHANGE_OUT_DIR");
		outDirSelectButton.addActionListener(this);
		outDirPanel.add(outDirSelectButton);
		activeComponents.add(outDirSelectButton);

		optionPanel.add(Box.createRigidArea(new Dimension(10, SPACING)));

		// -- Output format
		JPanel outFormatPanel = new JPanel();
		outFormatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outFormatPanel.setOpaque(false);
		outFormatPanel.setLayout(new BoxLayout(outFormatPanel, BoxLayout.PAGE_AXIS));
		optionPanel.add(outFormatPanel);

		JLabel outFormatTitle = new JLabel("Output format");
		outFormatTitle.setFont(TITLE_FONT);
		outFormatTitle.setForeground(TEXT_COLOR);
		outFormatPanel.add(outFormatTitle);

		JPanel outPrefixPanel = new JPanel();
		outPrefixPanel.setMaximumSize(new Dimension(OUT_FORMAT_TEXT_WIDTH, Integer.MAX_VALUE));
		outPrefixPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outPrefixPanel.setOpaque(false);
		outPrefixPanel.setLayout(new BorderLayout());
		outFormatPanel.add(outPrefixPanel);

		JLabel outPrefixLabel = new JLabel("Prefix: ");
		outPrefixLabel.setFont(TEXT_FONT);
		outPrefixLabel.setForeground(TEXT_COLOR);
		outPrefixPanel.add(outPrefixLabel, BorderLayout.WEST);

		outPrefixText = new JTextField(outPrefix);
		outPrefixText.setActionCommand("UPDATE_OUT_PREFIX");
		outPrefixText.addActionListener(this);
		outPrefixText.addFocusListener(this);
		outPrefixText.setBackground(TEXT_FIELD_COLOR);
		outPrefixText.setFont(TEXT_FONT);
		outPrefixText.setForeground(TEXT_COLOR);
		outPrefixPanel.add(outPrefixText);
		activeComponents.add(outPrefixText);

		JPanel pdfNamePanel = new JPanel();
		pdfNamePanel.setMaximumSize(new Dimension(OUT_FORMAT_TEXT_WIDTH, Integer.MAX_VALUE));
		pdfNamePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		pdfNamePanel.setOpaque(false);
		pdfNamePanel.setLayout(new BorderLayout());
		outFormatPanel.add(pdfNamePanel);

		JLabel pdfNameLabel = new JLabel("PDF name: ");
		pdfNameLabel.setFont(TEXT_FONT);
		pdfNameLabel.setForeground(TEXT_COLOR);
		pdfNamePanel.add(pdfNameLabel, BorderLayout.WEST);

		pdfNameText = new JTextField(pdfName);
		pdfNameText.setActionCommand("UPDATE_PDF_NAME");
		pdfNameText.addActionListener(this);
		pdfNameText.addFocusListener(this);
		pdfNameText.setBackground(TEXT_FIELD_COLOR);
		pdfNameText.setFont(TEXT_FONT);
		pdfNameText.setForeground(TEXT_COLOR);
		pdfNamePanel.add(pdfNameText);
		activeComponents.add(pdfNameText);

		optionPanel.add(Box.createRigidArea(new Dimension(10, SPACING)));

		// -- Image editing
		JPanel editPanel = new JPanel();
		editPanel.setOpaque(false);
		editPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.PAGE_AXIS));
		optionPanel.add(editPanel);

		JLabel editLabel = new JLabel("Edit image");
		editLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		editLabel.setFont(TITLE_FONT);
		editLabel.setForeground(TEXT_COLOR);
		editPanel.add(editLabel);

		JPanel brightnessPanel = new JPanel();
		brightnessPanel.setOpaque(false);
		brightnessPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		brightnessPanel.setMaximumSize(new Dimension(IMG_EDIT_PANEL_WIDTH, Integer.MAX_VALUE));
		brightnessPanel.setLayout(new BoxLayout(brightnessPanel, BoxLayout.LINE_AXIS));
		editPanel.add(brightnessPanel);

		JLabel brightnessLabel = new JLabel("Brightness:  ");
		brightnessLabel.setFont(TEXT_FONT);
		brightnessLabel.setForeground(TEXT_COLOR);
		brightnessPanel.add(brightnessLabel);

		brightnessValueLabel = new JLabel();
		brightnessValueLabel.setText(Integer.toString(DEFAULT_BRIGHTNESS));
		brightnessValueLabel.setMinimumSize(new Dimension(VALUE_LABEL_WIDTH, 0));
		brightnessValueLabel.setPreferredSize(new Dimension(VALUE_LABEL_WIDTH, brightnessValueLabel.getPreferredSize().height));
		brightnessValueLabel.setMaximumSize(new Dimension(VALUE_LABEL_WIDTH, Integer.MAX_VALUE));
		brightnessValueLabel.setHorizontalAlignment(JLabel.CENTER);
		brightnessValueLabel.setOpaque(true);
		brightnessValueLabel.setBackground(TEXT_FIELD_COLOR);
		brightnessValueLabel.setFont(TEXT_FONT);
		brightnessValueLabel.setForeground(TEXT_COLOR);
		brightnessValueLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		brightnessPanel.add(brightnessValueLabel);

		brightnessSlider = new JSlider(-100, 100, DEFAULT_BRIGHTNESS);
		brightnessSlider.setOpaque(false);
		brightnessSlider.addChangeListener(this);
		brightnessPanel.add(brightnessSlider);
		activeComponents.add(brightnessSlider);

		JPanel contrastPanel = new JPanel();
		contrastPanel.setOpaque(false);
		contrastPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		contrastPanel.setMaximumSize(new Dimension(IMG_EDIT_PANEL_WIDTH, Integer.MAX_VALUE));
		contrastPanel.setLayout(new BoxLayout(contrastPanel, BoxLayout.LINE_AXIS));
		editPanel.add(contrastPanel);

		JLabel contrastLabel = new JLabel("Contrast:");
		contrastLabel.setMinimumSize(brightnessLabel.getMinimumSize());
		contrastLabel.setPreferredSize(brightnessLabel.getPreferredSize());
		contrastLabel.setMaximumSize(brightnessLabel.getMaximumSize());
		contrastLabel.setFont(TEXT_FONT);
		contrastLabel.setForeground(TEXT_COLOR);
		contrastPanel.add(contrastLabel);

		contrastValueLabel = new JLabel();
		contrastValueLabel.setText(Integer.toString(DEFAULT_CONTRAST));
		contrastValueLabel.setMinimumSize(new Dimension(VALUE_LABEL_WIDTH, 0));
		contrastValueLabel.setPreferredSize(new Dimension(VALUE_LABEL_WIDTH, contrastValueLabel.getPreferredSize().height));
		contrastValueLabel.setMaximumSize(new Dimension(VALUE_LABEL_WIDTH, Integer.MAX_VALUE));
		contrastValueLabel.setHorizontalAlignment(JLabel.CENTER);
		contrastValueLabel.setOpaque(true);
		contrastValueLabel.setBackground(TEXT_FIELD_COLOR);
		contrastValueLabel.setFont(TEXT_FONT);
		contrastValueLabel.setForeground(TEXT_COLOR);
		contrastValueLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		contrastPanel.add(contrastValueLabel);

		contrastSlider = new JSlider(-100, 100, DEFAULT_CONTRAST);
		contrastSlider.setOpaque(false);
		contrastSlider.addChangeListener(this);
		contrastPanel.add(contrastSlider);
		activeComponents.add(contrastSlider);

		resetButton = new JButton("Reset");
		resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		resetButton.setFont(TEXT_FONT);
		resetButton.addActionListener(this);
		resetButton.setActionCommand("RESET_DEFAULT");
		editPanel.add(resetButton);
		activeComponents.add(resetButton);

		optionPanel.add(Box.createRigidArea(new Dimension(10, SPACING)));

		// -- Operations
		JPanel operationPanel = new JPanel();
		operationPanel.setOpaque(false);
		operationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		operationPanel.setLayout(new BoxLayout(operationPanel, BoxLayout.PAGE_AXIS));
		optionPanel.add(operationPanel);

		JLabel operationLabel = new JLabel("Operations");
		operationLabel.setFont(TITLE_FONT);
		operationLabel.setForeground(TEXT_COLOR);
		operationPanel.add(operationLabel);

		rename = new JCheckBox("Rename files");
		rename.setAlignmentX(Component.LEFT_ALIGNMENT);
		rename.setFont(TEXT_FONT);
		rename.setForeground(TEXT_COLOR);
		rename.setOpaque(false);
		rename.setSelected(false);
		operationPanel.add(rename);
		activeComponents.add(rename);

		adjustBrightnessAndContrast = new JCheckBox("Adjust brightness and contrast");
		adjustBrightnessAndContrast.setAlignmentX(Component.LEFT_ALIGNMENT);
		adjustBrightnessAndContrast.setFont(TEXT_FONT);
		adjustBrightnessAndContrast.setForeground(TEXT_COLOR);
		adjustBrightnessAndContrast.setOpaque(false);
		adjustBrightnessAndContrast.setSelected(true);
		operationPanel.add(adjustBrightnessAndContrast);
		activeComponents.add(adjustBrightnessAndContrast);

		convertToPDF = new JCheckBox("Convert to PDF and merge");
		convertToPDF.setAlignmentX(Component.LEFT_ALIGNMENT);
		convertToPDF.setFont(TEXT_FONT);
		convertToPDF.setForeground(TEXT_COLOR);
		convertToPDF.setOpaque(false);
		convertToPDF.setSelected(true);
		operationPanel.add(convertToPDF);
		activeComponents.add(convertToPDF);

		optionPanel.add(Box.createRigidArea(new Dimension(10, SPACING)));

		// -- Run program
		JButton runProcessor = new JButton("Go!");
		runProcessor.setAlignmentX(Component.LEFT_ALIGNMENT);
		runProcessor.setFont(TEXT_FONT);
		runProcessor.setActionCommand("RUN");
		runProcessor.addActionListener(this);
		optionPanel.add(runProcessor);
		activeComponents.add(runProcessor);

		optionPanel.add(Box.createRigidArea(new Dimension(10, BOT_SPACING)));

		// Right panel (image preview)
		JPanel imgPanel = new JPanel();
		imgPanel.setBackground(IMG_PANE_COLOR);
		imgPanel.setLayout(new BoxLayout(imgPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(imgPanel);

		imgPanel.add(Box.createVerticalGlue());

		imagePreviewName = new JLabel("...");
		imagePreviewName.setAlignmentX(Component.CENTER_ALIGNMENT);
		imagePreviewName.setFont(TEXT_FONT);
		imagePreviewName.setForeground(TEXT_COLOR);
		imgPanel.add(imagePreviewName);

		imagePreview = new JLabel();
		imagePreview.setOpaque(true);
		imagePreview.setBackground(IMG_PREVIEW_BACKGROUND);
		imagePreview.setAlignmentX(Component.CENTER_ALIGNMENT);
		imagePreview.setHorizontalAlignment(JLabel.CENTER);
		imagePreview.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		imagePreview.setMinimumSize(new Dimension(MAX_IMG_WIDTH, MAX_IMG_HEIGHT));
		imagePreview.setPreferredSize(new Dimension(MAX_IMG_WIDTH, MAX_IMG_HEIGHT));
		imagePreview.setMaximumSize(new Dimension(MAX_IMG_WIDTH, MAX_IMG_HEIGHT));
		imgPanel.add(imagePreview);

		JPanel previewButtonPanel = new JPanel();
		previewButtonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		previewButtonPanel.setOpaque(false);
		previewButtonPanel.setLayout(new BoxLayout(previewButtonPanel, BoxLayout.LINE_AXIS));
		imgPanel.add(previewButtonPanel);

		JButton deleteButton = new JButton("Delete");
		deleteButton.setActionCommand("DELETE");
		deleteButton.addActionListener(this);
		deleteButton.setFont(TEXT_FONT);
		previewButtonPanel.add(deleteButton);
		activeComponents.add(deleteButton);

		JButton zoomButton = new JButton("Zoom");
		zoomButton.setActionCommand("ZOOM");
		zoomButton.addActionListener(this);
		zoomButton.setFont(TEXT_FONT);
		previewButtonPanel.add(zoomButton);
		activeComponents.add(zoomButton);

		previewButtonPanel.add(Box.createRigidArea(new Dimension(PREVIEW_BUTTON_SPACING, 1)));

		previewLeftButton = new JButton("<");
		previewLeftButton.setActionCommand("PREVIEW_LEFT");
		previewLeftButton.addActionListener(this);
		previewLeftButton.setFont(TEXT_FONT);
		previewLeftButton.setEnabled(false);
		previewButtonPanel.add(previewLeftButton);
		activeComponents.add(previewLeftButton);

		previewRightButton = new JButton(">");
		previewRightButton.setActionCommand("PREVIEW_RIGHT");
		previewRightButton.addActionListener(this);
		previewRightButton.setFont(TEXT_FONT);
		previewButtonPanel.add(previewRightButton);
		activeComponents.add(previewRightButton);

		previewButtonPanel.add(Box.createRigidArea(new Dimension(PREVIEW_BUTTON_SPACING, 1)));

		showOriginal = new JToggleButton("Original");
		showOriginal.setActionCommand("SHOW_ORIGINAL");
		showOriginal.addActionListener(this);
		showOriginal.setFont(TEXT_FONT);
		previewButtonPanel.add(showOriginal);
		activeComponents.add(showOriginal);

		JButton refreshButton = new JButton("Refresh");
		refreshButton.setActionCommand("REFRESH");
		refreshButton.addActionListener(this);
		refreshButton.setFont(TEXT_FONT);
		previewButtonPanel.add(refreshButton);
		activeComponents.add(refreshButton);

		imgPanel.add(Box.createVerticalGlue());

		// Set the directory at the end to avoid null pointer errors from updating the image preview
		setInputDirectory(DEFAULT_IN_DIR);

		window.setVisible(true);
	}

	/**
	 * Creates a popup dialog with the desired error message
	 * 
	 * @param msg The error message to be shown
	 */
	public static void showErrorMessage(String msg)
	{
		JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Changes the directory in which to look for the files to be processed. If
	 * there are unsaved edits to the images in the current input directory, the
	 * user will be notified and asked for confirmation before proceeding. Once the
	 * new directory is set, setAutoBounds() and updateOrderedImages() are called to
	 * update the list of images.
	 * 
	 * @param dir The new input directory
	 */
	private void setInputDirectory(File dir)
	{
		if (!dir.exists())
		{
			showErrorMessage("Error: " + dir.getAbsolutePath() + " could not be found");
			return;
		}

		if (orderedImages != null && orderedImages.isEdited())
		{
			String msg = "There are unsaved changes in the current directory. Proceed?";
			int choice = JOptionPane.showConfirmDialog(null, msg, "Confirm directory change", JOptionPane.YES_NO_OPTION);
			if (choice != JOptionPane.YES_OPTION)
				return;

			// Reset the brightness and contrast
			brightness = DEFAULT_BRIGHTNESS;
			ignoreStateChange = true;
			brightnessSlider.setValue(DEFAULT_BRIGHTNESS);
			brightnessValueLabel.setText(Integer.toString(brightnessSlider.getValue()));
			contrast = DEFAULT_CONTRAST;
			ignoreStateChange = true;
			contrastSlider.setValue(DEFAULT_CONTRAST);
			contrastValueLabel.setText(Integer.toString(contrastSlider.getValue()));
		}

		inputDirectory = dir;
		inputDirectoryLabel.setText("  " + dir.getAbsolutePath());

		if (setAutoBounds())
		{
			Thread updateThread = new Thread(new Runnable()
			{
				public void run()
				{
					setImageEditingAllowed(true);
					updateOrderedPages();
					updateImagePreview();
				}
			});
			updateThread.start();
		}
		else
		{
			orderedFiles = null;
			orderedImages = null;
			setImageEditingAllowed(false);
			updateImagePreview();
		}
	}

	/**
	 * Changes the directory in which to save the resulting files (if the "Rename
	 * files" and/or "Convert to PDF and merge" options are selected).
	 * 
	 * @param dir The new output directory
	 */
	private void setOutputDirectory(File dir)
	{
		if (!dir.exists())
		{
			showErrorMessage("Error: " + dir.getAbsolutePath() + " could not be found");
			return;
		}

		outputDirectory = dir;
		outputDirectoryLabel.setText("  " + dir.getAbsolutePath());
	}

	/**
	 * Disables all active GUI components (e.g. buttons, sliders, etc.) in
	 * activeComponents. Components that were already locked are added to the
	 * lockedComponents list.
	 */
	private void lockGUI()
	{
		for (Component c : activeComponents)
		{
			if (!c.isEnabled())
				lockedComponents.add(c);
			else
			{
				ignoreStateChange = true;
				c.setEnabled(false);
				ignoreStateChange = false;
			}
		}
	}

	/**
	 * Enables all GUI components that are not in the lockedComponents list. The
	 * lockedComponents list is also emptied. This method should always follow a
	 * call to lockGUI(), as it depends on the lockedComponents list.
	 */
	private void unlockGUI()
	{
		for (Component c : activeComponents)
		{
			if (lockedComponents.contains(c))
				lockedComponents.remove(c);
			else
			{
				ignoreStateChange = true;
				c.setEnabled(true);
				ignoreStateChange = false;
			}
		}
	}

	/**
	 * Checks if the given filename is properly formatted. A valid filename must
	 * have the correct prefix followed immediately by a 3-digit integer within the
	 * current bounds (including the endpoints). It must also have a .jpg or .jpeg
	 * extension.
	 * 
	 * @param filename The filename to check
	 * @return TRUE if the filename is properly formatted and FALSE otherwise
	 */
	private boolean hasGoodFormat(String filename)
	{
		// Check for .jpg or .jpeg extension
		if (filename.length() <= 5)
			return false;
		else if (filename.substring(filename.length() - 4).equals(".jpg"))
			filename = filename.substring(0, filename.length() - 4);
		else if (filename.substring(0, filename.length() - 5).equals(".jpeg"))
			filename = filename.substring(0, filename.length() - 5);
		else
			return false;

		String requiredPrefix = inPrefix.trim().toLowerCase();
		String actualPrefix = filename.substring(0, requiredPrefix.length()).toLowerCase();

		// Check prefix
		if (!actualPrefix.equals(requiredPrefix))
			return false;

		// Check length
		if (filename.length() != requiredPrefix.length() + 3)
			return false;

		// Check that the last characters are a valid integer in the required range
		try
		{
			int actualInt = Integer.parseInt(filename.substring(actualPrefix.length()));
			if (actualInt < minFileNum || actualInt > maxFileNum)
				return false;
		}
		catch (NumberFormatException e)
		{
			return false;
		}

		return true;
	}

	/**
	 * Finds the minimum and maximum file numbers among all valid files in the
	 * current directory.
	 * 
	 * The file format is checked using hasGoodFormat(), and the bounds are
	 * temporarily set to 0 and 999 to find files even if they are outside the
	 * current bounds.
	 * 
	 * @return {min, max} if at least one valid file was found in the current
	 *         directory and NULL otherwise
	 */
	private int[] getBounds()
	{
		// Set the min and max to 0 and 999 so that the file number isn't taken into account
		// when checking the format
		int oldMinNumber = minFileNum;
		int oldMaxNumber = maxFileNum;
		minFileNum = 0;
		maxFileNum = 999;

		int max = -1;
		int min = Integer.MAX_VALUE;
		int count = 0;
		String filename;
		String currentNumStr;
		int currentNum;
		for (File f : inputDirectory.listFiles())
		{
			filename = f.getName();
			if (hasGoodFormat(filename))
			{
				count++;
				currentNumStr = filename.substring(filename.length() - 7, filename.length() - 4);
				currentNum = Integer.parseInt(currentNumStr);
				if (currentNum > max)
					max = currentNum;
				if (currentNum < min)
					min = currentNum;
			}
		}

		minFileNum = oldMinNumber;
		maxFileNum = oldMaxNumber;
		if (count == 0)
			return null;
		else
			return new int[] { min, max };
	}

	/**
	 * Automatically sets the maximum and minimum file numbers. If there is at least
	 * one valid file in the current directory, the bounds are set to the maximum
	 * and minimum file numbers found by getBounds(). Otherwise, the max and min are
	 * both set to 001.
	 * 
	 * @return TRUE if at least one valid file was found in the current directory
	 *         and FALSE otherwise
	 */
	private boolean setAutoBounds()
	{
		int[] bounds = getBounds();
		if (bounds == null)
		{
			// No files found
			minFileNum = 1;
			minNumberText.setText(String.format("%03d", minFileNum));
			maxFileNum = 1;
			maxNumberText.setText(String.format("%03d", maxFileNum));
			return false;
		}
		else
		{
			minFileNum = bounds[0];
			minNumberText.setText(String.format("%03d", bounds[0]));
			maxFileNum = bounds[1];
			maxNumberText.setText(String.format("%03d", bounds[1]));
			return true;
		}
	}

	/**
	 * Checks that the given string is valid and, if it is, sets it as the new
	 * minimum file number. A valid file number must be a non-negative integer, must
	 * be less than or equal to the current maximum, and must be exactly three
	 * digits long (with leading zeroes if it is less than 100).
	 * 
	 * @param min The new minimum file number
	 */
	private void setMinNumber(String min)
	{
		if (min.length() != 3)
		{
			showErrorMessage("The minimum number must have a length of 3");
			minNumberText.setText(String.format("%03d", minFileNum));
			return;
		}

		try
		{
			int newMinInt = Integer.parseInt(min);

			if (newMinInt > maxFileNum)
			{
				showErrorMessage("The minimum number cannot be greater than the maximum number");
				minNumberText.setText(String.format("%03d", minFileNum));
				return;
			}
			if (newMinInt < 0)
			{
				showErrorMessage("The minimum number cannot be negative");
				minNumberText.setText(String.format("%03d", minFileNum));
				return;
			}

			if (minFileNum != newMinInt)
			{
				minFileNum = newMinInt;
				Thread updateThread = new Thread(new Runnable()
				{
					public void run()
					{
						setImageEditingAllowed(true);
						updateOrderedPages();
						updateImagePreview();
					}
				});
				updateThread.start();
			}
		}
		catch (NumberFormatException e)
		{
			showErrorMessage(String.format("\"%s\" is not a valid integer!", min));
			minNumberText.setText(String.format("%03d", minFileNum));
		}
	}

	/**
	 * Checks that the given string is valid and, if it is, sets it as the new
	 * maximum file number. A valid file number must be a non-negative integer, must
	 * be greater than or equal to the current minimum, and must be exactly three
	 * digits long (with leading zeroes if it is less than 100).
	 * 
	 * @param max The new maximum file number
	 */
	private void setMaxNumber(String max)
	{
		if (max.length() != 3)
		{
			showErrorMessage("The maximum number must have a length of 3");
			maxNumberText.setText(String.format("%03d", maxFileNum));
			return;
		}

		try
		{
			int newMaxInt = Integer.parseInt(max);

			if (newMaxInt < minFileNum)
			{
				showErrorMessage("The maximum number cannot be less than the maximum number");
				maxNumberText.setText(String.format("%03d", maxFileNum));
				return;
			}
			if (newMaxInt < 0)
			{
				showErrorMessage("The maximum number cannot be negative");
				maxNumberText.setText(String.format("%03d", maxFileNum));
				return;
			}

			if (maxFileNum != newMaxInt)
			{
				maxFileNum = newMaxInt;
				Thread updateThread = new Thread(new Runnable()
				{
					public void run()
					{
						setImageEditingAllowed(true);
						updateOrderedPages();
						updateImagePreview();
					}
				});
				updateThread.start();
			}
		}
		catch (NumberFormatException e)
		{
			showErrorMessage(String.format("\"%s\" is not a valid integer!", max));
			maxNumberText.setText(String.format("%03d", maxFileNum));
		}
	}

	/**
	 * Sets the image preview according to the current list of images and index. If
	 * the list is not null and the index is valid, the image is set as the icon for
	 * the imagePreview label and the filename is displayed above. Otherwise, the
	 * label is made blank.
	 */
	private void updateImagePreview()
	{
		if (orderedImages != null && imagePreviewIndex < orderedFiles.size())
		{
			File file = orderedFiles.get(imagePreviewIndex);
			imagePreviewName.setText(file.getName());
			BufferedImage buffImg;
			if (showOriginal.isSelected())
				buffImg = orderedImages.getOriginal(imagePreviewIndex);
			else
				buffImg = orderedImages.get(imagePreviewIndex);

			// Get image dimensions
			int width, height;
			// -- Tall image
			if (buffImg.getHeight() * MAX_IMG_WIDTH >= buffImg.getWidth() * MAX_IMG_HEIGHT)
			{
				height = MAX_IMG_HEIGHT;
				width = buffImg.getWidth() * height / buffImg.getHeight();
			}
			// -- Wide image
			else
			{
				width = MAX_IMG_WIDTH;
				height = buffImg.getHeight() * width / buffImg.getWidth();
			}

			imagePreview.setIcon(new ImageIcon(buffImg.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
		}
		else
		{
			imagePreviewName.setText("No images found");
			imagePreview.setIcon(null);
		}
	}

	/**
	 * Gets all the valid files in the current directory, in order. The order is
	 * determined by the "Interlace pages" and "Reverse even-numbered pages"
	 * options. The files are saved in the orderedFiles list, and the images are
	 * then loaded in to the orderedImages list. The GUI is locked until all the
	 * images are loaded.
	 * 
	 * If "Interlace pages" is selected, the files are considered to be numbered
	 * such that all odd-numbered pages were scanned before the even-numbered pages.
	 * For example, the files img001, img002, img003, img004, img005 would be
	 * returned in the order img001, img004, img002, img005, img003.
	 * 
	 * If "Reverse even-numbered pages" is selected, the even-numbered pages are
	 * added to the list in reverse order. In the previous example, img001, img004,
	 * img002, img005, img003 would be replaced by img001, img005, img002, img004,
	 * img003. Note that this option is only available if "Interlace pages" is also
	 * selected.
	 * 
	 * This method calls the OrderedImages constructor, which is slow and places a
	 * task on the EDT. As a result, this method must not be called directly from
	 * the EDT.
	 */
	private void updateOrderedPages()
	{
		lockGUI();

		ArrayList<File> allPages = new ArrayList<File>();

		File file;
		// Find all pages with valid format
		for (int i = minFileNum; i <= maxFileNum; i++)
		{
			file = new File(inputDirectory + File.separator + inPrefix + String.format("%03d", i) + ".jpg");

			if (file.exists())
				allPages.add(file);
			// If a page was deleted, pretend it's still there for sorting purposes
			else
				allPages.add(null);
		}

		// Get pages in the right order
		ArrayList<File> orderedPages = new ArrayList<File>();
		// -- Pages already in order
		if (!interlacePages.isSelected())
		{
			for (File f : allPages)
			{
				if (f != null)
					orderedPages.add(f);
			}
		}
		// -- Odd-numbered pages all listed before even-numbered pages
		else
		{
			int numOddPages = (allPages.size() + 1) / 2;

			// Get odd-numbered pages
			ArrayList<File> oddPages = new ArrayList<File>();
			for (int i = 0; i < numOddPages; i++)
			{
				oddPages.add(allPages.get(i));
			}

			// Get even-numbered pages
			ArrayList<File> evenPages = new ArrayList<File>();
			for (int i = numOddPages; i < allPages.size(); i++)
			{
				evenPages.add(allPages.get(i));
			}
			if (reverseEvenPages.isSelected())
				reverseArray(evenPages);

			orderedPages = mergeArrays(oddPages, evenPages);
		}

		orderedFiles = orderedPages;
		try
		{
			orderedImages = null;
			System.gc();
			orderedImages = new OrderedImages(orderedFiles, window);

			// Update image preview index and buttons
			if (imagePreviewIndex >= orderedFiles.size())
				imagePreviewIndex = (orderedFiles.size() > 0 ? orderedFiles.size() - 1 : 0);
			if (imagePreviewIndex == 0)
				previewLeftButton.setEnabled(false);
			else
				previewLeftButton.setEnabled(true);
			if (orderedFiles.size() == 0 || imagePreviewIndex == orderedFiles.size() - 1)
				previewRightButton.setEnabled(false);
			else
				previewRightButton.setEnabled(true);

			unlockGUI();
		}
		catch (IOException e)
		{
			showErrorMessage("An unexpected IO error occurred and the images could not be loaded");
			orderedFiles = null;
			orderedImages = null;
			minFileNum = 1;
			minNumberText.setText(String.format("%03d", minFileNum));
			maxFileNum = 1;
			maxNumberText.setText(String.format("%03d", maxFileNum));
			unlockGUI();
			return;
		}
	}

	/**
	 * Performs an in-order reversal on the given ArrayList.
	 * 
	 * @param array The array to reverse
	 */
	private void reverseArray(ArrayList<File> array)
	{
		for (int i = 0; i < array.size() / 2; i++)
		{
			File temp = array.get(i);
			array.set(i, array.get(array.size() - 1 - i));
			array.set(array.size() - 1 - i, temp);
		}
	}

	/**
	 * Combines the two lists into one, adding one element from the first list and
	 * then one element from the second list until the end is reached.
	 * 
	 * For example, if odds = {A, B, C} and evens = {D, E} then the method will
	 * return {A, D, B, E, C}.
	 * 
	 * @param odds  The first list to be merged
	 * @param evens The second list to be merged
	 * @return A list with alternating elements from the two input lists
	 */
	private ArrayList<File> mergeArrays(ArrayList<File> odds, ArrayList<File> evens)
	{
		int i = 0;
		int j = 0;
		ArrayList<File> combinedList = new ArrayList<File>();

		while (true)
		{
			// End of odd numbered pages: add all remaining even pages and return
			if (i == odds.size())
			{
				while (j < evens.size())
				{
					if (evens.get(j) != null)
						combinedList.add(evens.get(j));
					j++;
				}
				return combinedList;
			}
			// End of even-numbered pages: add all remaining odd pages and return
			else if (j == evens.size())
			{
				while (i < odds.size())
				{
					if (odds.get(i) != null)
						combinedList.add(odds.get(i));
					i++;
				}
				return combinedList;
			}
			else
			{
				if (odds.get(i) != null)
					combinedList.add(odds.get(i));
				if (evens.get(j) != null)
					combinedList.add(evens.get(j));
				i++;
				j++;
			}
		}
	}

	/**
	 * Performs all the operations selected by the user (renaming the files, saving
	 * the brightness and contrast edits, and/or converting to PDF). A progress bar
	 * shows the progress in each step, along with messages indicating whether each
	 * step was successful. The GUI is locked until all operations are complete.
	 */
	private void processPages()
	{
		if (orderedFiles == null)
		{
			showErrorMessage("There are no pages to process!");
			return;
		}

		if (!(adjustBrightnessAndContrast.isSelected() && isEditingAllowed) && !rename.isSelected() && !convertToPDF.isSelected())
		{
			showErrorMessage("You haven't enabled any operations!");
			return;
		}

		lockGUI();

		// Set up progress bar
		progressFrame = new JFrame();
		JPanel progressPanel = new JPanel();
		JProgressBar progressBar = new JProgressBar(0, orderedFiles.size());
		JLabel progressLabel = makeLabel("...", TITLE_FONT, TEXT_COLOR, Component.LEFT_ALIGNMENT);
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					progressFrame.setSize(new Dimension(350, 300));
					progressFrame.setUndecorated(true);
					progressFrame.setResizable(false);
					progressFrame.setLocationRelativeTo(window);
					progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.PAGE_AXIS));
					progressPanel.setBackground(OPTION_PANE_COLOR);
					JScrollPane scrollPane = new JScrollPane(progressPanel);
					progressFrame.add(scrollPane);
					progressPanel.add(progressLabel);
					progressBar.setStringPainted(true);
					progressBar.setFont(TEXT_FONT);
					progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
					progressPanel.add(progressBar);
					progressPanel.add(Box.createVerticalStrut(15));
					progressFrame.setVisible(true);
				}
			});
		}
		catch (InvocationTargetException | InterruptedException e)
		{
		}

		int[] unsuccessful;
		String msg;
		// Save the image edits
		if (adjustBrightnessAndContrast.isSelected() && isEditingAllowed)
		{
			if (!orderedImages.isEdited())
			{
				msg = "- There are no image edits to save!";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
			}
			else
			{
				progressLabel.setText("Saving brightness and contrast edits");
				unsuccessful = adjustBrightnessAndContrast(progressBar);
				msg = "- " + (orderedFiles.size() - unsuccessful.length) + "/" + orderedFiles.size() + " edits successfully saved";
				progressPanel.add(makeLabel(msg, TEXT_FONT, TEXT_COLOR, Component.LEFT_ALIGNMENT));
				if (unsuccessful.length > 0)
				{
					msg = "  The edits to the following pages could not be saved:";
					progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
					for (int i = 0; i < unsuccessful.length; i++)
					{
						msg = "  > " + orderedFiles.get(unsuccessful[i]).getName();
						progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
					}
				}
			}
			progressPanel.revalidate();
		}

		// Rename files
		if (rename.isSelected())
		{
			progressBar.setValue(0);
			progressBar.setString("0/" + progressBar.getMaximum());
			progressLabel.setText("Renaming files...");
			unsuccessful = renameFiles(progressLabel, progressBar);
			if (unsuccessful.length == 1 && unsuccessful[0] == -1)
			{
				msg = "- The files could not be renamed due to a";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
				msg = "  name collision in the output directory";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
			}
			else
			{
				msg = "- " + (orderedFiles.size() - unsuccessful.length) + "/" + orderedFiles.size() + " files successfully renamed";
				progressPanel.add(makeLabel(msg, TEXT_FONT, TEXT_COLOR, Component.LEFT_ALIGNMENT));
				if (unsuccessful.length > 0)
				{
					msg = "  The following files could not be renamed:";
					progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
					for (int i = 0; i < unsuccessful.length; i++)
					{
						msg = "  > " + orderedFiles.get(unsuccessful[i]).getName();
						progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
					}
				}
			}
			progressPanel.revalidate();
		}

		// Merge files to one PDF
		if (convertToPDF.isSelected())
		{
			progressBar.setValue(0);
			progressBar.setString("0/" + progressBar.getMaximum());
			progressLabel.setText("Merging files...");
			int outcome = PDFConverter.convertToPDF(orderedFiles, outputDirectory, pdfName, progressBar);
			if (outcome == PDFConverter.SUCCESS)
			{
				msg = "- Successfully merged all files to ";
				progressPanel.add(makeLabel(msg, TEXT_FONT, TEXT_COLOR, Component.LEFT_ALIGNMENT));
				msg = "  " + outputDirectory + File.separator + pdfName + ".pdf";
				progressPanel.add(makeLabel(msg, TEXT_FONT, TEXT_COLOR, Component.LEFT_ALIGNMENT));
			}
			else if (outcome == PDFConverter.CANCELLED)
			{
				msg = "- The conversion was cancelled";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
			}
			else if (outcome == PDFConverter.IO_ERROR)
			{
				msg = "- The PDF could not be created due to an";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
				msg = "  unexpected I/O error";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
			}
			else if (outcome == PDFConverter.RENAME_FAIL)
			{
				msg = "- The PDF could not be renamed to";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
				msg = "  " + outputDirectory + File.separator + pdfName + ".pdf";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
				msg = "  The file is now available at";
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
				msg = "  " + PDFConverter.TEMP_OUT_DIR;
				progressPanel.add(makeLabel(msg, TEXT_FONT, ERROR_TEXT_COLOR, Component.LEFT_ALIGNMENT));
			}
		}

		progressPanel.add(Box.createVerticalStrut(15));
		JButton closeButton = new JButton("Close");
		closeButton.setFont(TEXT_FONT);
		closeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				progressFrame.setVisible(false);
				progressFrame.dispose();
				progressFrame = null;
			}
		});
		progressPanel.add(closeButton);
		progressPanel.revalidate();

		unlockGUI();
	}

	/**
	 * Saves the brightness and contrast edits in all the images. Also sets the new
	 * brightness and contrast values as the defaults in the ordered list of images.
	 * 
	 * @param progressBar A progress bar to track the method's progress
	 * @return The indices of all files for which the edits could not be saved
	 */
	private int[] adjustBrightnessAndContrast(JProgressBar progressBar)
	{
		// Set the edits as default
		orderedImages.setDefault();
		brightness = 0;
		brightnessSlider.setValue(0);
		contrast = 0;
		contrastSlider.setValue(0);

		ArrayList<Integer> unsuccessful = new ArrayList<Integer>();
		for (int i = 0; i < orderedFiles.size(); i++)
		{
			try
			{
				ImageIO.write(orderedImages.get(i), "jpg", orderedFiles.get(i));
			}
			catch (IOException e)
			{
				unsuccessful.add(i);
			}
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + progressBar.getMaximum());
		}

		int[] unsuccessfulIndices = new int[unsuccessful.size()];
		for (int i = 0; i < unsuccessful.size(); i++)
		{
			unsuccessfulIndices[i] = unsuccessful.get(i);
		}
		return unsuccessfulIndices;
	}

	/**
	 * Renames all the files in the ordered list. The output directory is first
	 * scanned for name collisions, and the method fails if any collision is
	 * detected. If the renaming is successful, the new file names will all have the
	 * output prefix selected by the user, and will be numbered in ascending order
	 * starting at 001.
	 * 
	 * If the renaming is successful, the input prefix, output prefix, and input
	 * directory are updated to reflect the new location and names of the renamed
	 * files.
	 * 
	 * @param status      The JLabel on which to display the stage the method is in
	 *                    (checking for collisions or renaming)
	 * @param progressBar A progress bar to track the method's progress
	 * @return The indices of all files for which the renaming failed unexpectedly,
	 *         or {-1} if any name collision was detected
	 */
	private int[] renameFiles(JLabel status, JProgressBar progressBar)
	{
		String outFilePath;
		ArrayList<Integer> unsuccessfulOps = new ArrayList<Integer>();

		// First check that there are no name collisions
		status.setText("Checking for collisions...");
		for (int i = 0; i < orderedFiles.size(); i++)
		{
			outFilePath = outputDirectory.getAbsolutePath() + File.separator + outPrefix + String.format("%03d", i + 1) + ".jpg";
			if (new File(outFilePath).exists())
				return new int[] { -1 };
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + progressBar.getMaximum());
		}

		// Rename all files
		status.setText("Renaming files...");
		for (int i = 0; i < orderedFiles.size(); i++)
		{
			outFilePath = outputDirectory.getAbsolutePath() + File.separator + outPrefix + String.format("%03d", i + 1) + ".jpg";
			boolean success = orderedFiles.get(i).renameTo(new File(outFilePath));
			if (!success)
				unsuccessfulOps.add(i);
			progressBar.setValue(i + 1);
			progressBar.setString((i + 1) + "/" + progressBar.getMaximum());
		}

		int[] unsuccessful = new int[unsuccessfulOps.size()];
		for (int i = 0; i < unsuccessfulOps.size(); i++)
		{
			unsuccessful[i] = unsuccessfulOps.get(i);
		}

		// Update prefix, etc. to display the renamed images in order
		inPrefix = outPrefix;
		inPrefixText.setText(inPrefix);
		outPrefix = DEFAULT_OUT_PREFIX;
		if (outPrefix.equals(inPrefix))
			outPrefix = BACKUP_OUT_PREFIX;
		outPrefixText.setText(outPrefix);
		ignoreStateChange = true;
		interlacePages.setSelected(false);
		reverseEvenPages.setEnabled(false);
		if (!lockedComponents.contains(reverseEvenPages))
			lockedComponents.add(reverseEvenPages);
		inputDirectory = outputDirectory;
		inputDirectoryLabel.setText("  " + inputDirectory.getAbsolutePath());
		updateOrderedPages();
		updateImagePreview();

		return unsuccessful;
	}

	/**
	 * Utility method that creates a JLabel with the desired characteristics
	 * 
	 * @param text       The text to be displayed in the label
	 * @param font       The label's font
	 * @param foreground The text colour
	 * @param alignment  The alignment of the entire label
	 * @return A JLabel with the given characteristics
	 */
	private static JLabel makeLabel(String text, Font font, Color foreground, float alignment)
	{
		JLabel newLabel = new JLabel(text);
		newLabel.setFont(font);
		newLabel.setForeground(foreground);
		newLabel.setAlignmentX(alignment);
		return newLabel;
	}

	/**
	 * Deletes the file at the specified index in the current list of ordered files.
	 * The user is asked for confirmation before proceeding. If no files remain
	 * after the deletion, orderedFiles and orderedImages are set to NULL. If the
	 * deleted file was the final element in the list and there is at least one
	 * other file remaining, the image preview index is decremented by one to show
	 * the new final image. In any case, the image preview is updated.
	 * 
	 * @param index The index of the file to be deleted in orderedFiles
	 */
	private void deleteFile(int index)
	{
		File f = orderedFiles.get(index);

		int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete " + f.getAbsolutePath() + "?", "File deletion warning",
				JOptionPane.YES_NO_CANCEL_OPTION);
		if (choice == JOptionPane.YES_OPTION)
		{
			f.delete();
			orderedFiles.remove(index);
			orderedImages.delete(index);
			if (orderedFiles.size() == 0)
			{
				orderedFiles = null;
				orderedImages = null;
				setImageEditingAllowed(false);
			}
			else if (imagePreviewIndex == orderedFiles.size())
				imagePreviewIndex--;
			updateImagePreview();
		}
	}

	/**
	 * Creates a new window with the zoomed-in image. The dimensions are chosen to
	 * fully fill the frame and the image is placed in a scroll pane to allow the
	 * user to view the rest of the image.
	 */
	private void makeZoomedImage()
	{
		JFrame zoomedFrame = new JFrame();
		zoomedFrame.setSize(ZOOMED_PANE_WIDTH, ZOOMED_PANE_HEIGHT);
		zoomedFrame.setResizable(false);
		zoomedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JLabel zoomedImage = new JLabel();
		Image img;
		if (showOriginal.isSelected())
			img = orderedImages.getOriginal(imagePreviewIndex);
		else
			img = orderedImages.get(imagePreviewIndex);

		// Get image dimensions
		int width, height;
		// -- Tall image
		if (img.getHeight(null) * ZOOMED_IMG_WIDTH >= img.getWidth(null) * ZOOMED_IMG_HEIGHT)
		{
			width = ZOOMED_IMG_WIDTH;
			height = img.getHeight(null) * width / img.getWidth(null);
		}
		// -- Wide image
		else
		{
			height = ZOOMED_IMG_HEIGHT;
			width = img.getWidth(null) * height / img.getHeight(null);
		}

		img = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		zoomedImage.setIcon(new ImageIcon(img));

		JScrollPane scrollPane = new JScrollPane(zoomedImage);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		zoomedFrame.add(scrollPane);

		zoomedFrame.setVisible(true);
	}

	/**
	 * Sets whether the user can adjust the brightness and contrast sliders. If
	 * FALSE, the brightness slider, contrast slider, and reset button are all
	 * disabled. If TRUE, all those components are enabled.
	 * 
	 * @param isAllowed
	 */
	private void setImageEditingAllowed(boolean isAllowed)
	{
		isEditingAllowed = isAllowed;
		brightnessSlider.setEnabled(isAllowed);
		contrastSlider.setEnabled(isAllowed);
		resetButton.setEnabled(isAllowed);
	}

	/**
	 * Handles events from buttons and text fields.
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		switch (e.getActionCommand())
		{
			case "CHANGE_IN_DIR":
				JFileChooser fcIn = new JFileChooser();
				fcIn.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (fcIn.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
					setInputDirectory(fcIn.getSelectedFile());
				break;
			case "UPDATE_IN_PREFIX":
				ignoreFocusLost = true;
				String newInPrefix = inPrefixText.getText().trim().toLowerCase();
				if (newInPrefix.equals(outPrefix))
				{
					showErrorMessage("The input and output prefixes cannot be identical");
					inPrefixText.setText(inPrefix);
				}
				else if (!newInPrefix.equals(inPrefix))
				{
					inPrefix = newInPrefix;
					inPrefixText.setText(inPrefix);
					// Update the list of pages if any were found, otherwise just make the lists null
					if (setAutoBounds())
					{
						Thread updateThread = new Thread(new Runnable()
						{
							public void run()
							{
								setImageEditingAllowed(true);
								updateOrderedPages();
								updateImagePreview();
							}
						});
						updateThread.start();
					}
					else
					{
						orderedFiles = null;
						orderedImages = null;
						setImageEditingAllowed(false);
						updateImagePreview();
					}
				}
				else
					inPrefixText.setText(inPrefix);
				break;
			case "UPDATE_MIN":
				ignoreFocusLost = true;
				setMinNumber(minNumberText.getText());
				break;
			case "UPDATE_MAX":
				ignoreFocusLost = true;
				setMaxNumber(maxNumberText.getText());
				break;
			case "CHANGE_OUT_DIR":
				JFileChooser fcOut = new JFileChooser();
				fcOut.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (fcOut.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
					setOutputDirectory(fcOut.getSelectedFile());
				break;
			case "UPDATE_OUT_PREFIX":
				ignoreFocusLost = true;
				String newOutPrefix = outPrefixText.getText().trim().toLowerCase();
				if (newOutPrefix.equals(inPrefix))
				{
					showErrorMessage("The input and output prefixes cannot be identical");
					outPrefixText.setText(outPrefix);
				}
				else
				{
					outPrefix = newOutPrefix;
					outPrefixText.setText(outPrefix);
				}
				break;
			case "UPDATE_PDF_NAME":
				ignoreFocusLost = true;
				String newName = pdfNameText.getText().trim();
				if (newName.equals(""))
				{
					showErrorMessage("The PDF name cannot be blank");
					pdfNameText.setText(pdfName);
				}
				else
				{
					pdfName = newName;
					pdfNameText.setText(pdfName);
				}
				break;
			case "RESET_DEFAULT":
				if (orderedImages.isEdited())
				{
					Thread resetThread = new Thread(new Runnable()
					{
						public void run()
						{
							lockGUI();
							ignoreStateChange = true;
							brightnessSlider.setValue(DEFAULT_BRIGHTNESS);
							brightness = DEFAULT_BRIGHTNESS;
							ignoreStateChange = true;
							contrastSlider.setValue(DEFAULT_CONTRAST);
							contrast = DEFAULT_CONTRAST;
							orderedImages.reset(window);
							updateImagePreview();
							unlockGUI();
						}
					});
					resetThread.start();
				}
				break;
			case "RUN":
				Thread runThread = new Thread(new Runnable()
				{

					public void run()
					{
						processPages();
					}

				});
				runThread.start();
				break;
			case "PREVIEW_LEFT":
				imagePreviewIndex--;
				if (imagePreviewIndex == 0)
					previewLeftButton.setEnabled(false);
				if (!previewRightButton.isEnabled())
					previewRightButton.setEnabled(true);
				updateImagePreview();
				break;
			case "PREVIEW_RIGHT":
				imagePreviewIndex++;
				if (imagePreviewIndex == orderedFiles.size() - 1)
					previewRightButton.setEnabled(false);
				if (!previewLeftButton.isEnabled())
					previewLeftButton.setEnabled(true);
				updateImagePreview();
				break;
			case "DELETE":
				deleteFile(imagePreviewIndex);
				break;
			case "ZOOM":
				makeZoomedImage();
				break;
			case "SHOW_ORIGINAL":
				updateImagePreview();
				break;
			case "REFRESH":
				// Reset edits
				brightness = 0;
				brightnessSlider.setValue(0);
				contrast = 0;
				contrastSlider.setValue(0);
				if (setAutoBounds())
				{
					Thread refreshThread = new Thread(new Runnable()
					{
						public void run()
						{
							lockGUI();
							setImageEditingAllowed(true);
							updateOrderedPages();
							updateImagePreview();
							unlockGUI();
						}
					});
					refreshThread.start();
				}
				else
				{
					orderedFiles = null;
					orderedImages = null;
					setImageEditingAllowed(false);
					updateImagePreview();
				}
				break;
		}
	}

	/**
	 * Handles state changes from sliders (i.e. the brightness and contrast
	 * sliders). In every case, the brightness and contrast labels are updated to
	 * reflect the current value of the slider. If the user has finished adjusting
	 * the value and has selected a different value from the one currently saved in
	 * the corresponding instance variable, the images are edited. If
	 * ignoreStateChange is TRUE the method will set it to FALSE and return, but
	 * only after updating the label.
	 */
	@Override
	public void stateChanged(ChangeEvent e)
	{
		Object source = e.getSource();

		if (source == brightnessSlider)
		{
			brightnessValueLabel.setText(Integer.toString(brightnessSlider.getValue()));
			if (ignoreStateChange)
			{
				ignoreStateChange = false;
				return;
			}
			if (!brightnessSlider.getValueIsAdjusting() && brightnessSlider.getValue() != brightness)
			{
				brightness = brightnessSlider.getValue();
				Thread brightnessThread = new Thread(new Runnable()
				{
					public void run()
					{
						lockGUI();
						orderedImages.setBrightness(brightness, window);
						updateImagePreview();
						unlockGUI();
					}
				});
				brightnessThread.start();
			}
		}
		else if (source == contrastSlider)
		{
			contrastValueLabel.setText(Integer.toString(contrastSlider.getValue()));
			if (ignoreStateChange)
			{
				ignoreStateChange = false;
				return;
			}
			if (!contrastSlider.getValueIsAdjusting() && contrastSlider.getValue() != contrast)
			{
				contrast = contrastSlider.getValue();
				Thread contrastThread = new Thread(new Runnable()
				{

					public void run()
					{
						lockGUI();
						orderedImages.setContrast(contrast, window);
						updateImagePreview();
						unlockGUI();
					}

				});
				contrastThread.start();
			}
		}
	}

	/**
	 * Empty method to satisfy the requirements of the FocusListener interface.
	 */
	@Override
	public void focusGained(FocusEvent e)
	{
	}

	/**
	 * Handles focus lost events from text boxes. The text is simply reset to match
	 * the value saved in the corresponding instance variable (e.g. if inPrefixText
	 * loses focus, its text is reset to inPrefix). If ignoreFocusLost is TRUE, the
	 * method sets it to FALSE and returns immediately.
	 */
	@Override
	public void focusLost(FocusEvent e)
	{
		if (ignoreFocusLost)
		{
			ignoreFocusLost = false;
			return;
		}

		Object source = e.getSource();
		if (source == inPrefixText)
			inPrefixText.setText(inPrefix);
		else if (source == minNumberText)
			minNumberText.setText(String.format("%03d", minFileNum));
		else if (source == maxNumberText)
			minNumberText.setText(String.format("%03d", minFileNum));
		else if (source == outPrefixText)
			outPrefixText.setText(outPrefix);
		else if (source == pdfNameText)
			pdfNameText.setText(pdfName);
	}

	/**
	 * Handles state changes from check boxes (i.e. "Interlace pages" and "Reverse
	 * even-numbered pages"). If ignoreStateChange is TRUE, the method sets it to
	 * false and returns immediately.
	 */
	@Override
	public void itemStateChanged(ItemEvent e)
	{
		if (ignoreStateChange)
		{
			ignoreStateChange = false;
			return;
		}

		Object source = e.getSource();

		if (source == interlacePages)
		{
			// Interlace pages deselected: deactivate "reverse even-numbered pages" option
			if (!interlacePages.isSelected())
				reverseEvenPages.setEnabled(false);
			// Interlace pages selected: activate "reverse even-numbered pages" option
			else
				reverseEvenPages.setEnabled(true);

			Thread updateThread = new Thread(new Runnable()
			{
				public void run()
				{
					updateOrderedPages();
					updateImagePreview();
				}
			});
			updateThread.start();
		}
		else if (source == reverseEvenPages)
		{
			Thread updateThread = new Thread(new Runnable()
			{

				public void run()
				{
					updateOrderedPages();
					updateImagePreview();
				}
			});
			updateThread.start();
		}
	}
}
