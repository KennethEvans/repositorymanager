package net.kenevans.git.repositorymanager.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.prefs.Preferences;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.kenevans.git.repositorymanager.model.IConstants;
import net.kenevans.git.repositorymanager.model.RepositoryLocations;
import net.kenevans.git.repositorymanager.model.RepositoryModel;
import net.kenevans.git.repositorymanager.preferences.RepositoriesDialog;
import net.kenevans.git.repositorymanager.utils.ImageUtils;
import net.kenevans.git.repositorymanager.utils.Utils;

/**
 * RepositoryManager is a viewer to view ECG fileNames from the MD100A ECG
 * Monitor.
 * 
 * @author Kenneth Evans, Jr.
 */
public class RepositoryManager extends JFrame implements IConstants
{
    private static final long serialVersionUID = 1L;
    public static final String LS = System.getProperty("line.separator");

    public static final boolean LOAD_TEST_REPOSITORIES = false;

    private ArrayList<RepositoryModel> repositories = new ArrayList<>();

    private RepositoryLocations repositoryLocations;

    /** Keeps the last-used path for the file open dialog. */
    public String defaultOpenPath;
    /** Keeps the last-used path for the file save dialog. */
    public String defaultSavePath;

    // User interface controls (Many do not need to be global)
    private Container contentPane = this.getContentPane();
    private JPanel listPanel = new JPanel();
    private JPanel lowerPanel = new JPanel();
    private DefaultListModel<RepositoryModel> listModel = new DefaultListModel<>();
    private JList<RepositoryModel> list = new JList<>(listModel);
    private JScrollPane listScrollPane;
    private JTextArea summaryTextArea;
    private JTextArea infoTextArea;
    private JPanel summaryPanel = new JPanel();
    private JPanel mainPanel = new JPanel();
    private JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        summaryPanel, lowerPanel);
    private JMenuBar menuBar;
    private JPopupMenu listPopupMenu;

    /** Array of file names for the viewer. */
    public String[] fileNames = {};
    /** The currently selected file name. */
    private String curFileName;

    private Image commitImage;
    private Image pushImage;
    private Image pullImage;
    private Image notFoundImage;
    private Image notTrackingImage;

    private int jPanelHeight;

    /**
     * RepositoryManager constructor.
     */
    public RepositoryManager() {
        repositoryLocations = new RepositoryLocations();
        repositoryLocations.loadFromPreferences();
        getIcons();
        uiInit();
        refresh();
    }

    /**
     * Calculates a list of repositories from the current parent directories and
     * individual repositories.
     */
    public void setRepositories() {
        if(repositoryLocations == null) {
            Utils.errMsg(
                "Cannot set repositories, " + "repositoryLocations = null");
            return;
        }
        repositories.clear();
        // Directories
        File parentDir;
        for(String dirName : repositoryLocations.getParentDirectories()) {
            parentDir = new File(dirName);
            File[] files = parentDir.listFiles();
            for(File dir : files) {
                if(dir.isDirectory()) {
                    // Check if there is a .git repository
                    File[] files1 = dir.listFiles();
                    for(File dir1 : files1) {
                        if(dir1.isDirectory()
                            && dir1.getName().equals(".git")) {
                            repositories.add(new RepositoryModel(dir));
                            continue;
                        }
                    }
                }
            }
        }
        // Individual
        for(String dirName : repositoryLocations.getIndividualRepositories()) {
            repositories.add(new RepositoryModel(dirName));
        }
        // Sort them
        Collections.sort(repositories, new Comparator<RepositoryModel>() {
            @Override
            public int compare(RepositoryModel model1, RepositoryModel model2) {
                // TODO Auto-generated method stub
                return (model2.getFilePath().compareTo(model2.getFilePath()));
            }
        });
    }

    /**
     * Calculates a list of repository locations from the given parent
     * directories and individual repositories.
     * 
     * @param parentDirectories
     * @param individualRepositories
     */
    public void setRepositoryLocations(String[] parentDirectories,
        String[] individualRepositories) {
        ArrayList<String> parentDirectoriesList = new ArrayList<String>(
            Arrays.asList(parentDirectories));
        ArrayList<String> individualRepositoriesList = new ArrayList<String>(
            Arrays.asList(individualRepositories));
        repositoryLocations.setParentDirectories(parentDirectoriesList);
        repositoryLocations
            .setIndividualRepositories(individualRepositoriesList);
        setRepositories();
    }

    private void getIcons() {
        // Determine the size
        JLabel label = new JLabel("W");
        jPanelHeight = label.getPreferredSize().height;

        // Create ImageIcons
        Image image;
        image = ImageUtils.getImageFromClassResource(this.getClass(),
            "/resources/commit.png");
        commitImage = ImageUtils.resize(image, jPanelHeight, jPanelHeight);
        image = ImageUtils.getImageFromClassResource(this.getClass(),
            "/resources/push.png");
        pushImage = ImageUtils.resize(image, jPanelHeight, jPanelHeight);
        image = ImageUtils.getImageFromClassResource(this.getClass(),
            "/resources/pull.png");
        pullImage = ImageUtils.resize(image, jPanelHeight, jPanelHeight);
        image = ImageUtils.getImageFromClassResource(this.getClass(),
            "/resources/notfound.png");
        notFoundImage = ImageUtils.resize(image, jPanelHeight, jPanelHeight);
        image = ImageUtils.getImageFromClassResource(this.getClass(),
            "/resources/nottracking.png");
        notTrackingImage = ImageUtils.resize(image, jPanelHeight, jPanelHeight);
    }

    /**
     * Initializes the user interface.
     */
    void uiInit() {
        this.setLayout(new BorderLayout());

        // Summary Panel
        summaryPanel.setLayout(new BorderLayout());

        // Summary text area
        summaryTextArea = new JTextArea("Welcome to Repository Monitor");
        summaryTextArea.setEditable(false);
        summaryTextArea.setColumns(40);
        JScrollPane summaryScrollPane = new JScrollPane(summaryTextArea);
        summaryPanel.add(summaryScrollPane, BorderLayout.CENTER);

        // List panel
        listScrollPane = new JScrollPane(list);
        listPanel.setLayout(new BorderLayout());
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
                // Internal implementation
                onListItemSelected(ev);
            }
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        listPopupMenu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Test");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RepositoryModel model = list.getSelectedValue();
                if(model != null) {
                    Utils.infoMsg(model.getFilePath());
                } else {
                    Utils.errMsg("Cannot determine selected item");
                }
            }
        });
        listPopupMenu.add(item);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                if(ev.isPopupTrigger()) {
                    // Have to select the item at this point
                    int row = list.locationToIndex(ev.getPoint());
                    list.setSelectedIndex(row);
                    // Show the menu where the click was
                    listPopupMenu.show(list, ev.getX(), ev.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mousePressed(e);
            }
        });

        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
                JLabel label = (JLabel)super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
                RepositoryModel model = (RepositoryModel)value;
                // Set the text
                label.setText(model.getFilePath());
                // Set the icon
                model.calculateState();
                BufferedImage bi = new BufferedImage(3 * jPanelHeight,
                    jPanelHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics g = bi.createGraphics();
                int pos = 0;
                if(model.isNotFound()) {
                    g.drawImage(notFoundImage, pos, 0, null);
                    pos += jPanelHeight;
                } else {
                    if(!model.isClean()) {
                        g.drawImage(commitImage, pos, 0, null);
                        pos += jPanelHeight;
                    }
                    if(model.isNotTracking()) {
                        g.drawImage(notTrackingImage, pos, 0, null);
                        pos += jPanelHeight;
                    } else {
                        if(model.isBehind()) {
                            g.drawImage(pullImage, pos, 0, null);
                            pos += jPanelHeight;
                        }
                        if(model.isAhead()) {
                            g.drawImage(pushImage, pos, 0, null);
                            pos += jPanelHeight;
                        }
                    }
                }
                label.setIcon(new ImageIcon(bi));
                g.dispose();

                return label;
            }
        });

        // Info Panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());

        // Info text area
        infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setColumns(40);
        JScrollPane infoScrollPane = new JScrollPane(infoTextArea);
        infoPanel.add(infoScrollPane, BorderLayout.CENTER);

        // Lower split pane
        JSplitPane lowerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            listPanel, infoPanel);
        lowerPane.setContinuousLayout(true);
        lowerPane.setDividerLocation(LOWER_PANE_DIVIDER_LOCATION);

        // Main split pane
        mainPane.setContinuousLayout(true);
        mainPane.setDividerLocation(MAIN_PANE_DIVIDER_LOCATION);
        if(false) {
            mainPane.setOneTouchExpandable(true);
        }

        // Lower panel
        lowerPanel.setLayout(new BorderLayout());
        lowerPanel.add(lowerPane, BorderLayout.CENTER);

        // Main panel
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(mainPane, BorderLayout.CENTER);

        // Content pane
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the menus.
     */
    private void initMenus() {
        JMenuItem menuItem;

        // Menu
        menuBar = new JMenuBar();

        // File
        JMenu menu = new JMenu();
        menu.setText("File");
        menuBar.add(menu);

        // Refresh
        menuItem = new JMenuItem();
        menuItem.setText("Refresh");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                refresh();
            }
        });
        menu.add(menuItem);

        // Show summary details
        menuItem = new JMenuItem();
        menuItem.setText("Show Summary Details...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showSummaryDetails();
            }
        });
        menu.add(menuItem);

        JSeparator separator = new JSeparator();
        menu.add(separator);

        // File Exit
        menuItem = new JMenuItem();
        menuItem.setText("Exit");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                quit();
            }
        });
        menu.add(menuItem);

        // Tools
        menu = new JMenu();
        menu.setText("Tools");
        menuBar.add(menu);

        // separator = new JSeparator();
        // menu.add(separator);

        JMenu menu1 = new JMenu();
        menu1.setText("Repositories");
        menu.add(menu1);

        menuItem = new JMenuItem();
        menuItem.setText("Manage Repositories...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                manageRepositories();
            }
        });
        menu1.add(menuItem);

        menuItem = new JMenuItem();
        menuItem.setText("Store Repositories");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                boolean res = repositoryLocations.saveToPreferences(true);
                if(res) {
                    Utils.infoMsg(
                        "Current repositories stored in persistent storage");
                } else {
                    Utils.errMsg(
                        "Failed to store current repositories in persistent storage");
                }
            }
        });
        menu1.add(menuItem);

        // Help
        menu = new JMenu();
        menu.setText("Help");
        menuBar.add(menu);

        menuItem = new JMenuItem();
        menuItem.setText("About");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JOptionPane.showMessageDialog(null,
                    new AboutBoxPanel(TITLE + " " + VERSION,
                        "Written by Kenneth Evans, Jr.", "kenevans.net",
                        "Copyright (c) 2016 Kenneth Evans"),
                    "About", JOptionPane.PLAIN_MESSAGE);
            }
        });
        menu.add(menuItem);
    }

    /**
     * Puts the panel in a JFrame and runs the JFrame.
     */
    public void run() {
        try {
            // Create and set up the window.
            this.setTitle(TITLE);
            // USE EXIT_ON_CLOSE not DISPOSE_ON_CLOSE to close any modeless
            // dialogs
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // frame.setLocationRelativeTo(null);

            // Set the icon
            ImageUtils.setIconImageFromResource(this,
                "/resources/repositorymanager.png");

            // Has to be done here. The menus are not part of the JPanel.
            initMenus();
            this.setJMenuBar(menuBar);

            // Display the window
            this.setBounds(20, 20, FRAME_WIDTH, FRAME_HEIGHT);
            this.setVisible(true);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Refreshes list.
     */
    public void refresh() {
        setRepositories();
        // Get the summary
        summaryTextArea.setText(getSummary());
        // Find in the list
        populateList();
    }

    /**
     * Gets a summary of the state of all repositories in the list.
     * 
     * @return
     */
    private String getSummary() {
        int totalCount = 0;
        int dirtyCount = 0;
        int aheadCount = 0;
        int behindCount = 0;
        int notTrackingCount = 0;
        int notFoundCount = 0;
        for(RepositoryModel model : repositories) {
            model.calculateState();
            totalCount++;
            if(!model.isClean()) {
                dirtyCount++;
            }
            if(model.isAhead()) {
                aheadCount++;
            }
            if(model.isBehind()) {
                behindCount++;
            }
            if(model.isNotTracking()) {
                notTrackingCount++;
            }
            if(model.isNotFound()) {
                notFoundCount++;
            }
        }

        StringBuilder sb = new StringBuilder();

        // Summary
        Utils.appendLS(sb);
        Utils.appendLine(sb,
            "Total: " + totalCount + ", Dirty: " + dirtyCount + ", Behind: "
                + behindCount + ", Ahead: " + aheadCount + ", Not tracking: "
                + notTrackingCount + ", Not found: " + notFoundCount);
        return sb.toString();
    }

    /**
     * Gets a summary of the state of all repositories in the list.
     * 
     * @return
     */
    private String getSummaryDetails() {
        int totalCount = 0;
        int dirtyCount = 0;
        int aheadCount = 0;
        int behindCount = 0;
        int notTrackingCount = 0;
        int notFoundCount = 0;
        ArrayList<String> dirtyFiles = new ArrayList<>();
        ArrayList<String> aheadFiles = new ArrayList<>();
        ArrayList<String> behindFiles = new ArrayList<>();
        ArrayList<String> notTrackingFiles = new ArrayList<>();
        ArrayList<String> notFoundFiles = new ArrayList<>();
        for(RepositoryModel model : repositories) {
            model.calculateState();
            totalCount++;
            if(!model.isClean()) {
                dirtyCount++;
                dirtyFiles.add(model.getFilePath());
            }
            if(model.isAhead()) {
                aheadCount++;
                aheadFiles.add(model.getFilePath());
            }
            if(model.isBehind()) {
                behindCount++;
                behindFiles.add(model.getFilePath());
            }
            if(model.isNotTracking()) {
                notTrackingCount++;
                notTrackingFiles.add(model.getFilePath());
            }
            if(model.isNotFound()) {
                notFoundCount++;
                notFoundFiles.add(model.getFilePath());
            }
        }

        StringBuilder sb = new StringBuilder();

        // Summary
        String tab = "    ";
        Utils.appendLS(sb);
        Utils.appendLine(sb,
            "Total: " + totalCount + ", Dirty: " + dirtyCount + ", Behind: "
                + behindCount + ", Ahead: " + aheadCount + ", Not tracking: "
                + notTrackingCount + ", Not found: " + notFoundCount);
        if(dirtyCount > 0) {
            Utils.appendLS(sb);
            Utils.appendLine(sb, "Dirty");
            for(String string : dirtyFiles) {
                Utils.appendLine(sb, tab + string);
            }
        }
        if(behindCount > 0) {
            Utils.appendLS(sb);
            Utils.appendLine(sb, "Behind");
            for(String fileName : behindFiles) {
                Utils.appendLine(sb, tab + fileName);
            }
        }
        if(aheadCount > 0) {
            Utils.appendLS(sb);
            Utils.appendLine(sb, "Ahead");
            for(String fileName : aheadFiles) {
                Utils.appendLine(sb, tab + fileName);
            }
        }
        if(notTrackingCount > 0) {
            Utils.appendLS(sb);
            Utils.appendLine(sb, "Not Tracking");
            for(String fileName : notTrackingFiles) {
                Utils.appendLine(sb, tab + fileName);
            }
        }
        if(notFoundCount > 0) {
            Utils.appendLS(sb);
            Utils.appendLine(sb, "Not Found");
            for(String fileName : notFoundFiles) {
                Utils.appendLine(sb, tab + fileName);
            }
        }
        return sb.toString();
    }

    /**
     * Loads a new model.
     * 
     * @param fileName
     */
    private void loadModel(final RepositoryModel model) {
        if(model == null) {
            Utils.errMsg("loadModel: Model is null");
            return;
        }

        // Needs to be done this way to allow the text to change before reading
        // the image.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Cursor oldCursor = getCursor();
                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    updateInfoText(model);
                } catch(Exception ex) {
                    String msg = "Error loading model: " + model.getFilePath();
                    Utils.excMsg(msg, ex);
                } catch(Error err) {
                    String msg = "Error loading model: " + model.getFilePath();
                    Utils.excMsg(msg, err);
                } finally {
                    setCursor(oldCursor);
                }
            }
        });
    }

    /**
     * Populates the list from the list of profiles.
     */
    private void populateList() {
        list.setEnabled(false);
        listModel.removeAllElements();
        for(RepositoryModel model : repositories) {
            listModel.addElement(model);
        }
        list.validate();
        mainPane.validate();
        list.setEnabled(true);
    }

    /**
     * Handler for the list. Toggles the checked state.
     * 
     * @param ev
     */
    private void onListItemSelected(ListSelectionEvent ev) {
        if(ev.getValueIsAdjusting()) return;
        RepositoryModel model = (RepositoryModel)list.getSelectedValue();
        loadModel(model);
    }

    /**
     * Shows model information.
     */
    private void showSummaryDetails() {

        scrolledTextMsg(null, getSummaryDetails(), "Summary Details",
            DETAILS_WIDTH, DETAILS_HEIGHT);
    }

    /**
     * Brings up a dialog to manage repositories.
     */
    private void manageRepositories() {
        RepositoriesDialog dialog = new RepositoriesDialog(this, this);
        // For modal, use this and dialog.showDialog() instead of
        // dialog.setVisible(true)
        // dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        URL url = RepositoryManager.class
            .getResource("/resources/repositorymanager.png");
        if(url != null) {
            dialog.setIconImage(new ImageIcon(url).getImage());
        }
        dialog.setVisible(true);
        // This only returns on Cancel and always returns true. All actions
        // are done from the dialog.
        // dialog.showDialog();
    }

    /**
     * Displays a scrolled text dialog with the given message.
     * 
     * @param message
     */
    public static void scrolledTextMsg(Frame parent, String message,
        String title, int width, int height) {
        final JDialog dialog = new JDialog(parent);

        // Message
        JPanel jPanel = new JPanel();
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        jPanel.add(scrollPane, BorderLayout.CENTER);
        dialog.getContentPane().add(scrollPane);

        // Close button
        jPanel = new JPanel();
        JButton button = new JButton("OK");
        jPanel.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }

        });
        dialog.getContentPane().add(jPanel, BorderLayout.SOUTH);

        dialog.setTitle(title);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        // Has to be done after set size
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Updates the info text area.
     * 
     * @param model
     */
    public void updateInfoText(RepositoryModel model) {
        String info = "";
        if(model != null) {
            info += model.getInfo() + LS;
        }
        infoTextArea.setText(info);
        infoTextArea.setCaretPosition(0);
    }

    /**
     * Quits the application
     */
    private void quit() {
        System.exit(0);
    }

    /**
     * @return The value of curFileName.
     */
    public String getCurFileName() {
        return curFileName;
    }

    /**
     * Returns the user preference store for the viewer.
     * 
     * @return
     */
    public static Preferences getUserPreferences() {
        return Preferences.userRoot().node(P_PREFERENCE_NODE);
    }

    /**
     * Loads a hard-coded set of repositories for initialization or testing.
     * 
     * @param repositoryManager
     */
    private void loadTestRepositories() {
        String[] TEST_PARENT_DIRS = {
            // Start
            "C:/AndroidStudioProjects",
            // End
        };
        String[] TEST_INDIVIDUAL_REPOSITORIES = {
            // Start
            "C:/Git/SVN/AppInfo", "C:/Git/jgit-cookbook",
            "C:/Git/color-thief-java", "C:/eclipseProjects/GitWorkspace",
            "C:/eclipseWorkspaces/Work/JGit Examples",
            // End
        };
        setRepositoryLocations(TEST_PARENT_DIRS, TEST_INDIVIDUAL_REPOSITORIES);
        refresh();
    }

    /**
     * @return The value of repositoryLocations.
     */
    public RepositoryLocations getRepositoryLocations() {
        return repositoryLocations;
    }

    /**
     * @param repositoryLocations The new value for repositoryLocations.
     */
    public void setRepositoryLocations(
        RepositoryLocations repositoryLocations) {
        this.repositoryLocations = repositoryLocations;
    }

    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Set window decorations
            JFrame.setDefaultLookAndFeelDecorated(true);

            // Set the native look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Make the job run in the AWT thread
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    RepositoryManager app = new RepositoryManager();
                    // DEBUG This needs to be done the first time for now
                    if(LOAD_TEST_REPOSITORIES) {
                        app.loadTestRepositories();
                    }
                    app.run();
                }
            });
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

}