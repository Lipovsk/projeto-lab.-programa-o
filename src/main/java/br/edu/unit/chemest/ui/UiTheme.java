package br.edu.unit.chemest.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Locale;

/** Presentation defaults and reusable styles for the Swing application. */
public final class UiTheme {
    public static final Color BACKGROUND = new Color(15, 23, 35);
    public static final Color SURFACE = new Color(23, 34, 49);
    public static final Color SURFACE_ALT = new Color(30, 44, 61);
    public static final Color PRIMARY = new Color(40, 143, 177);
    public static final Color PRIMARY_HOVER = new Color(51, 162, 195);
    public static final Color TEXT_PRIMARY = new Color(230, 238, 245);
    public static final Color TEXT_SECONDARY = new Color(160, 179, 197);
    public static final Color SUCCESS = new Color(112, 201, 170);
    public static final Color ERROR = new Color(239, 143, 148);
    public static final Color BORDER = new Color(51, 69, 88);
    public static final Color CONSOLE = new Color(10, 16, 25);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font TITLE = BODY.deriveFont(Font.BOLD, 24f);
    public static final Font SECTION = BODY.deriveFont(Font.BOLD, 16f);
    public static final Font AUXILIARY = BODY.deriveFont(12f);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    public static final int GAP = 12;
    public static final int PADDING = 18;
    public static final int FIELD_HEIGHT = 36;

    private UiTheme() {}

    public static void install() {
        // Metal provides consistent pure Swing painting on every platform.
        try { UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel()); }
        catch (UnsupportedLookAndFeelException e) { throw new IllegalStateException(e); }
        for (String key : new String[]{"Panel", "Label", "Button", "TextField", "TextArea", "Spinner", "Table", "TableHeader", "TabbedPane", "OptionPane", "ComboBox", "List", "Menu", "MenuItem", "CheckBox", "RadioButton", "ToolTip"}) {
            UIManager.put(key + ".font", BODY);
            UIManager.put(key + ".background", SURFACE);
            UIManager.put(key + ".foreground", TEXT_PRIMARY);
        }
        UIManager.put("control", SURFACE);
        UIManager.put("controlText", TEXT_PRIMARY);
        UIManager.put("text", TEXT_PRIMARY);
        UIManager.put("textText", TEXT_PRIMARY);
        UIManager.put("textHighlight", PRIMARY);
        UIManager.put("textHighlightText", Color.WHITE);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("TextField.inactiveForeground", TEXT_SECONDARY);
        UIManager.put("Button.disabledText", TEXT_SECONDARY);
        UIManager.put("ComboBox.selectionBackground", PRIMARY);
        UIManager.put("List.selectionBackground", PRIMARY);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("TabbedPane.selected", SURFACE_ALT);
        UIManager.put("TabbedPane.contentAreaColor", BACKGROUND);
        UIManager.put("TabbedPane.focus", PRIMARY);
        UIManager.put("TabbedPane.selectHighlight", PRIMARY);
        UIManager.put("TabbedPane.tabInsets", new Insets(12, 18, 12, 18));
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.thumbDarkShadow", BORDER);
        UIManager.put("ScrollBar.thumbHighlight", SURFACE_ALT);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BACKGROUND);
        return panel;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JPanel card(String title, Component content) {
        JPanel card = new JPanel(new BorderLayout(GAP, GAP));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));
        card.add(label(title, SECTION, TEXT_PRIMARY), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public static void field(JComponent component) {
        component.setFont(BODY);
        component.setBackground(SURFACE_ALT);
        component.setForeground(TEXT_PRIMARY);
        component.setPreferredSize(new Dimension(component.getPreferredSize().width, FIELD_HEIGHT));
        if (component instanceof JSpinner spinner && spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            field(editor.getTextField());
            spinner.setBorder(BorderFactory.createLineBorder(BORDER));
            return;
        }
        Border normal = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(7, 10, 7, 10));
        Border focused = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY), BorderFactory.createEmptyBorder(7, 10, 7, 10));
        component.setBorder(normal);
        if (component instanceof JTextField text) text.setCaretColor(TEXT_PRIMARY);
        component.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) { component.setBorder(focused); }
            public void focusLost(FocusEvent event) { component.setBorder(normal); }
        });
    }

    public static void button(JButton button, boolean primary) {
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(primary ? PRIMARY : SURFACE_ALT);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? PRIMARY : BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        button.setRolloverEnabled(true);
        button.addChangeListener(event -> {
            button.setBackground(!button.isEnabled() ? SURFACE : button.getModel().isRollover() ? (primary ? PRIMARY_HOVER : BORDER) : (primary ? PRIMARY : SURFACE_ALT));
            button.setForeground(button.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);
        });
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    /** Trim only the white depiction margins for display; CDK output remains unchanged. */
    public static ImageIcon depictionIcon(java.awt.image.BufferedImage source) {
        int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<source.getHeight();y++) for(int x=0;x<source.getWidth();x++) {
            int pixel=source.getRGB(x,y);
            if ((pixel>>>24)!=0 && ((pixel>>16&255)<245 || (pixel>>8&255)<245 || (pixel&255)<245)) {
                left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
            }
        }
        if(right<left) return new ImageIcon(source);
        left=Math.max(0,left-16);top=Math.max(0,top-16);
        right=Math.min(source.getWidth()-1,right+16);bottom=Math.min(source.getHeight()-1,bottom+16);
        return new ImageIcon(source.getSubimage(left,top,right-left+1,bottom-top+1));
    }

    public static JTable table(javax.swing.table.TableModel model, boolean numeric) {
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(BODY);
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(new Color(35, 83, 108));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(BODY.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(SURFACE_ALT);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable owner, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(owner, value, selected, focus, row, column);
                boolean number = numeric && column > 0 && value instanceof Number;
                setText(number ? String.format(Locale.ROOT, "%.6f", ((Number) value).doubleValue()) : value == null ? "" : value.toString());
                setFont(number ? MONO : BODY);
                setHorizontalAlignment(number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBackground(selected ? owner.getSelectionBackground() : row % 2 == 0 ? SURFACE : SURFACE_ALT);
                setForeground(selected ? Color.WHITE : TEXT_PRIMARY);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(focus ? PRIMARY : getBackground()), BorderFactory.createEmptyBorder(0, 10, 0, 10)));
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        if (numeric) table.getColumnModel().getColumn(0).setPreferredWidth(300);
        return table;
    }
}
