package main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Aplicación de interfaz gráfica (Swing) para el análisis, visualización
 * y búsqueda de patrones geométricos y secuencias numéricas/textuales
 * en los dígitos del número Pi.
 *
 * Soporta carga por archivo físico local (pi_digits.txt) y generación
 * matemática infinita en tiempo real mediante un motor Spigot.
 *
 * @author José González Alonso
 * @version 2.0
 */
@SuppressWarnings("unused")
public class PiPatternAnalyzer extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
     * Motor matemático basado en el algoritmo Spigot (Jeremy Gibbons) para
     * calcular dígitos reales de Pi de forma secuencial e infinita
     * mediante aritmética de precisión arbitraria (BigInteger).
     */
    private static class PiSpigotEngine {
        private BigInteger q = BigInteger.ONE;
        private BigInteger r = BigInteger.ZERO;
        private BigInteger t = BigInteger.ONE;
        private BigInteger k = BigInteger.ONE;
        private BigInteger n = BigInteger.valueOf(3);
        private BigInteger l = BigInteger.valueOf(3);

        /** Caché en memoria para evitar re-calcular dígitos ya procesados. */
        private final List<Byte> cachedDigits = new ArrayList<>();

        /**
         * Obtiene el dígito de Pi en el índice especificado.
         * Si no ha sido calculado previamente, genera todos los dígitos
         * intermedios hasta alcanzar la posición deseada.
         *
         * @param index Posición del dígito decimal deseado (0-indexed).
         * @return Dígito numérico entre 0 y 9.
         */
        public synchronized byte getDigitAt(int index) {
            while (cachedDigits.size() <= index) {
                cachedDigits.add((byte) calculateNextDigit());
            }
            return cachedDigits.get(index);
        }

        /**
         * Ejecuta una iteración de la serie matemática Spigot para extraer
         * el siguiente dígito exacto de Pi.
         *
         * @return El siguiente dígito calculado.
         */
        private int calculateNextDigit() {
            while (true) {
                BigInteger four = BigInteger.valueOf(4);
                if (q.multiply(four).add(r).subtract(t).compareTo(n.multiply(t)) < 0) {
                    int digit = n.intValue();
                    BigInteger nr = BigInteger.TEN.multiply(r.subtract(n.multiply(t)));
                    n = BigInteger.TEN.multiply(q.multiply(BigInteger.valueOf(3)).add(r))
                            .divide(t).subtract(BigInteger.TEN.multiply(n));
                    q = q.multiply(BigInteger.TEN);
                    r = nr;
                    return digit;
                } else {
                    BigInteger nr = q.multiply(BigInteger.TWO).add(r).multiply(l);
                    BigInteger nn = q.multiply(BigInteger.valueOf(7).multiply(k).add(BigInteger.TWO))
                            .add(r.multiply(l)).divide(t.multiply(l));
                    q = q.multiply(k);
                    t = t.multiply(l);
                    l = l.add(BigInteger.TWO);
                    k = k.add(BigInteger.ONE);
                    n = nn;
                    r = nr;
                }
            }
        }
    }

    /**
     * Modos de representación visual disponibles para la cuadrícula de análisis.
     */
    public enum DisplayMode {
        /** Muestra los dígitos en formato numérico tradicional (0-9). */
        NUMBERS,
        /** Muestra pares de dígitos convertidos en caracteres del alfabeto inglés (A-Z). */
        ENGLISH_LETTERS,
        /** Representación por paleta fija de 10 colores distintivos. */
        TEN_COLORS,
        /** Representación en blanco y negro según paridad (Par = Negro, Impar = Blanco). */
        MONOCHROME_BW
    }

    /** Tamaño en píxeles de cada celda individual en la cuadrícula. */
    private static final int CELL_SIZE = 18;

    /** Número total de filas fijas que se muestran en el viewport. */
    private static final int CANVAS_HEIGHT_CELLS = 30;

    /** Paleta de colores para el modo {@link DisplayMode#TEN_COLORS}. */
    private static final Color[] TEN_PALETTE = {
        Color.BLACK,
        Color.RED,
        new Color(0, 150, 0),
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.ORANGE,
        Color.GRAY,
        Color.WHITE
    };

    private final PiSpigotEngine spigotEngine = new PiSpigotEngine();
    private int gridCols = 50;
    private long topRowIndex = 0;
    private DisplayMode currentMode = DisplayMode.TEN_COLORS;
    private long selectionStart = -1;
    private long selectionEnd = -1;

    private RandomAccessFile piFile;
    private boolean isFileLoaded = false;
    private long totalFileDigits = 0;

    private CanvasPanel canvasPanel;
    private final JLabel statusLabel;
    private final JLabel gridInfoLabel;
    private final JTextField jumpField;
    private final JTextField searchField;
    private final JComboBox<Integer> colSelector;

    /**
     * Constructor principal. Configura la ventana Swing, inicializa los componentes
     * de control y carga las fuentes de datos.
     */
    public PiPatternAnalyzer() {
        setTitle("Pi Infinite Pattern & Geometry Analyzer (Spigot Engine)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        canvasPanel = new CanvasPanel();
        initFileSource("pi_digits.txt");

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controlsPanel.setBackground(new Color(235, 238, 242));

        JComboBox<DisplayMode> modeSelector = new JComboBox<>(DisplayMode.values());
        modeSelector.setSelectedItem(currentMode);
        modeSelector.addActionListener(e -> {
            currentMode = (DisplayMode) modeSelector.getSelectedItem();
            updateSearchPlaceholder();
            canvasPanel.repaint();
        });

        Integer[] colOptions = {10, 21, 25, 29, 33, 50, 80, 100};
        colSelector = new JComboBox<>(colOptions);
        colSelector.setSelectedItem(gridCols);
        colSelector.addActionListener(e -> {
            gridCols = (Integer) colSelector.getSelectedItem();
            updateGridDimensions();
            canvasPanel.repaint();
        });

        jumpField = new JTextField(6);
        JButton jumpButton = new JButton("Go to #");
        jumpButton.addActionListener(e -> jumpToDigitIndex());

        searchField = new JTextField(10);
        searchField.setToolTipText("Escribe patrón a buscar y pulsa Enter o Buscar");
        searchField.addActionListener(e -> searchPatternSequence());

        JButton searchButton = new JButton("Find Next");
        searchButton.addActionListener(e -> searchPatternSequence());

        JButton exportButton = new JButton("Export PNG");
        exportButton.addActionListener(e -> exportSelectionToPNG());

        gridInfoLabel = new JLabel();
        updateGridDimensions();
        updateSearchPlaceholder();

        controlsPanel.add(new JLabel("Mode:"));
        controlsPanel.add(modeSelector);
        controlsPanel.add(new JLabel("Cols:"));
        controlsPanel.add(colSelector);
        controlsPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlsPanel.add(new JLabel("Pos:"));
        controlsPanel.add(jumpField);
        controlsPanel.add(jumpButton);
        controlsPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlsPanel.add(new JLabel("Search:"));
        controlsPanel.add(searchField);
        controlsPanel.add(searchButton);
        controlsPanel.add(exportButton);

        statusLabel = new JLabel(" Ready. Scroll with wheel, click/drag to highlight positions.", SwingConstants.LEFT);
        statusLabel.setPreferredSize(new Dimension(800, 28));
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        canvasPanel.addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            if (rotation > 0) {
                topRowIndex += 2;
            } else if (rotation < 0) {
                topRowIndex = Math.max(0, topRowIndex - 2);
            }
            updateStatusText();
            canvasPanel.repaint();
        });

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.add(controlsPanel, BorderLayout.NORTH);
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        infoPanel.setBackground(new Color(220, 225, 230));
        infoPanel.add(gridInfoLabel);
        headerWrapper.add(infoPanel, BorderLayout.SOUTH);

        add(headerWrapper, BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Intenta abrir el archivo externo de dígitos de Pi si existe en el sistema.
     *
     * @param filePath Ruta relativa o absoluta del archivo de texto.
     */
    private void initFileSource(String filePath) {
        try {
            File f = new File(filePath);
            if (f.exists()) {
                piFile = new RandomAccessFile(f, "r");
                totalFileDigits = piFile.length();
                isFileLoaded = true;
            }
        } catch (Exception ignored) {
            isFileLoaded = false;
        }
    }

    /**
     * Obtiene el dígito de Pi en un índice absoluto. Prioriza la lectura
     * del archivo local `pi_digits.txt` si está disponible; en caso contrario,
     * utiliza el motor matematico Spigot.
     *
     * @param index Posición absoluta del dígito buscado (0-indexed).
     * @return Valor numérico del dígito (0 a 9).
     */
    private int getPiDigitAt(long index) {
        if (index < 0) return 0;

        if (isFileLoaded && piFile != null) {
            try {
                if (index < totalFileDigits) {
                    piFile.seek(index);
                    byte b = piFile.readByte();
                    if (b >= '0' && b <= '9') {
                        return b - '0';
                    }
                }
            } catch (IOException ignored) {}
        }

        if (index > Integer.MAX_VALUE - 100) return 0;
        return spigotEngine.getDigitAt((int) index);
    }

    /**
     * Recalcula las dimensiones internas del panel de dibujo en función
     * de las columnas seleccionadas y actualiza el texto informativo.
     */
    private void updateGridDimensions() {
        int widthPx = gridCols * CELL_SIZE;
        int heightPx = CANVAS_HEIGHT_CELLS * CELL_SIZE;
        if (canvasPanel != null) {
            canvasPanel.setPreferredSize(new Dimension(widthPx, heightPx));
            pack();
        }
        if (gridInfoLabel != null) {
            gridInfoLabel.setText(String.format("Grid Specs: %d Cols x %d Rows (%d cells/viewport) | Source: %s",
                    gridCols, CANVAS_HEIGHT_CELLS, gridCols * CANVAS_HEIGHT_CELLS,
                    isFileLoaded ? "pi_digits.txt" : "Generador Matemático Spigot (Pi Infinito Real)"));
        }
    }

    /**
     * Actualiza el texto de sugerencia (tooltip) en la barra de búsqueda
     * dependiendo del modo de visualización actual.
     */
    private void updateSearchPlaceholder() {
        if (currentMode == DisplayMode.ENGLISH_LETTERS) {
            searchField.setToolTipText("Ejemplo de búsqueda: 'A', 'PI' o 'HELLO'");
        } else {
            searchField.setToolTipText("Ejemplo de búsqueda: '31415' o '9265'");
        }
    }

    /**
     * Desplaza el viewport directamente al índice introducido en el campo "Go to #".
     */
    private void jumpToDigitIndex() {
        try {
            long targetIndex = Long.parseLong(jumpField.getText().trim().replaceAll(",", ""));
            if (targetIndex < 0) targetIndex = 0;
            topRowIndex = targetIndex / gridCols;
            selectionStart = targetIndex;
            selectionEnd = targetIndex;
            updateStatusText();
            canvasPanel.repaint();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric digit index (e.g. 100)", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Redirige la búsqueda según el modo de visualización seleccionado.
     */
    private void searchPatternSequence() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        if (currentMode == DisplayMode.ENGLISH_LETTERS) {
            searchLetterSequence(query);
        } else {
            searchNumericSequence(query);
        }
    }

    /**
     * Busca una secuencia de dígitos numéricos en los datos de Pi.
     *
     * @param query Cadena con los dígitos numéricos a localizar.
     */
    private void searchNumericSequence(String query) {
        String digitsOnly = query.replaceAll("\\D+", "");
        if (digitsOnly.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduce dígitos numéricos para buscar (ej. 31415)", "Búsqueda", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int len = digitsOnly.length();
        int[] target = new int[len];
        for (int i = 0; i < len; i++) {
            target[i] = digitsOnly.charAt(i) - '0';
        }

        long maxLimit = isFileLoaded ? totalFileDigits - len : 20000;
        long startPos = (selectionStart >= 0) ? Math.max(selectionStart, selectionEnd) + 1 : (topRowIndex * gridCols);
        if (startPos >= maxLimit) startPos = 0;

        long foundIndex = findNumericMatch(target, startPos, maxLimit);
        if (foundIndex == -1 && startPos > 0) {
            foundIndex = findNumericMatch(target, 0, startPos - 1);
        }

        if (foundIndex != -1) {
            selectionStart = foundIndex;
            selectionEnd = foundIndex + len - 1;
            topRowIndex = foundIndex / gridCols;
            updateStatusText();
            canvasPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "No se encontró la secuencia numérica en el rango escaneado: \"" + digitsOnly + "\"", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Algoritmo auxiliar de búsqueda de coincidencia de patrones numéricos.
     *
     * @param target Array de enteros a buscar.
     * @param start  Índice inicial de búsqueda.
     * @param limit  Índice límite máximo de búsqueda.
     * @return Índice donde comienza la coincidencia, o -1 si no se encontró.
     */
    private long findNumericMatch(int[] target, long start, long limit) {
        int len = target.length;
        for (long i = start; i <= limit; i++) {
            boolean match = true;
            for (int j = 0; j < len; j++) {
                if (getPiDigitAt(i + j) != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    /**
     * Busca una secuencia de letras (A-Z) en los datos mapeados de Pi.
     *
     * @param query Palabra o conjunto de letras a localizar.
     */
    private void searchLetterSequence(String query) {
        String lettersOnly = query.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (lettersOnly.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduce letras para buscar en modo LETRAS (ej. HELLO o PI)", "Búsqueda", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int len = lettersOnly.length();
        char[] target = lettersOnly.toCharArray();

        long maxLimit = isFileLoaded ? totalFileDigits - len - 1 : 20000;
        long startPos = (selectionStart >= 0) ? Math.max(selectionStart, selectionEnd) + 1 : (topRowIndex * gridCols);
        if (startPos >= maxLimit) startPos = 0;

        long foundIndex = findLetterMatch(target, startPos, maxLimit);
        if (foundIndex == -1 && startPos > 0) {
            foundIndex = findLetterMatch(target, 0, startPos - 1);
        }

        if (foundIndex != -1) {
            selectionStart = foundIndex;
            selectionEnd = foundIndex + len - 1;
            topRowIndex = foundIndex / gridCols;
            updateStatusText();
            canvasPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "No se encontró la palabra de letras en el rango escaneado: \"" + lettersOnly + "\"", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Algoritmo auxiliar para localizar la coincidencia de secuencias alfabéticas.
     *
     * @param target Array de caracteres a buscar.
     * @param start  Índice inicial.
     * @param limit  Índice final.
     * @return Índice de inicio de la coincidencia o -1 si no existe.
     */
    private long findLetterMatch(char[] target, long start, long limit) {
        int len = target.length;
        for (long i = start; i <= limit; i++) {
            boolean match = true;
            for (int j = 0; j < len; j++) {
                int d1 = getPiDigitAt(i + j);
                int d2 = getPiDigitAt(i + j + 1);
                char letter = (char) ('A' + ((d1 * 10 + d2) % 26));
                if (letter != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    /**
     * Exporta la región o patrón actualmente seleccionado por el usuario a una imagen formato PNG.
     */
    private void exportSelectionToPNG() {
        if (selectionStart < 0) {
            JOptionPane.showMessageDialog(this, "Please select a region first by clicking/dragging on the grid.", "Export Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        long min = Math.min(selectionStart, selectionEnd);
        long max = Math.max(selectionStart, selectionEnd);
        int totalCells = (int) (max - min) + 1;

        int exportCols = gridCols;
        int exportRows = (int) Math.ceil((double) totalCells / exportCols);

        BufferedImage img = new BufferedImage(exportCols * CELL_SIZE, exportRows * CELL_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        for (int i = 0; i < totalCells; i++) {
            long digitIdx = min + i;
            int d = getPiDigitAt(digitIdx);
            int col = i % exportCols;
            int row = i / exportCols;
            int x = col * CELL_SIZE;
            int y = row * CELL_SIZE;

            if (currentMode == DisplayMode.TEN_COLORS) {
                g2.setColor(TEN_PALETTE[d]);
            } else if (currentMode == DisplayMode.MONOCHROME_BW) {
                g2.setColor((d % 2 == 0) ? Color.BLACK : Color.WHITE);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(d), x + 4, y + 14);
                continue;
            }
            g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
        }
        g2.dispose();

        try {
            File outFile = new File("pi_pattern_selection.png");
            ImageIO.write(img, "png", outFile);
            JOptionPane.showMessageDialog(this, "Exported selection successfully to:\n" + outFile.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to write PNG file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Actualiza el texto de la barra de estado inferior con los datos del
     * rango o celda seleccionada.
     */
    private void updateStatusText() {
        if (selectionStart < 0) {
            statusLabel.setText(String.format(" Viewport Top Row: #%,d", topRowIndex * gridCols));
            return;
        }

        long min = Math.min(selectionStart, selectionEnd);
        long max = Math.max(selectionStart, selectionEnd);

        if (min == max) {
            int val = getPiDigitAt(min);
            statusLabel.setText(String.format(" Selected Position: #%,d | Value: %d | Viewport Top Row: #%,d", min, val, topRowIndex * gridCols));
        } else {
            long count = (max - min) + 1;
            statusLabel.setText(String.format(" Match Found Range: #%,d to #%,d | Length: %,d cells", min, max, count));
        }
    }

    /**
     * Panel personalizado de Swing encargado del renderizado gráfico de la matriz
     * de dígitos y de la gestión de eventos de ratón para selección.
     */
    private class CanvasPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        /**
         * Inicializa los escuchadores de ratón para soportar selección individual o por arrastre.
         */
        public CanvasPanel() {
            setBackground(Color.DARK_GRAY);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    long index = getDigitIndexFromMouse(e.getX(), e.getY());
                    if (index >= 0) {
                        selectionStart = index;
                        selectionEnd = index;
                        updateStatusText();
                        repaint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    long index = getDigitIndexFromMouse(e.getX(), e.getY());
                    if (index >= 0 && selectionStart >= 0) {
                        selectionEnd = index;
                        updateStatusText();
                        repaint();
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        /**
         * Convierte las coordenadas del ratón (x, y) en píxeles al índice absoluto
         * del dígito de Pi en el viewport.
         *
         * @param x Coordenada X del clic/arrastre.
         * @param y Coordenada Y del clic/arrastre.
         * @return Índice global del dígito o -1 si la coordenada está fuera del área útil.
         */
        private long getDigitIndexFromMouse(int x, int y) {
            int col = x / CELL_SIZE;
            int row = y / CELL_SIZE;

            if (col < 0 || col >= gridCols || row < 0 || row >= CANVAS_HEIGHT_CELLS) {
                return -1;
            }

            return ((topRowIndex + row) * gridCols) + col;
        }

        /**
         * Dibuja los elementos visuales en el canvas (celdas, colores, textos y resaltado de selección).
         *
         * @param g Objeto Graphics proporcionado por el motor de pintado de Swing.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            for (int row = 0; row < CANVAS_HEIGHT_CELLS; row++) {
                for (int col = 0; col < gridCols; col++) {
                    long absoluteIndex = ((topRowIndex + row) * gridCols) + col;
                    int digit = getPiDigitAt(absoluteIndex);
                    int x = col * CELL_SIZE;
                    int y = row * CELL_SIZE;

                    switch (currentMode) {
                        case TEN_COLORS -> {
                            g2d.setColor(TEN_PALETTE[digit]);
                            g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        }
                        case MONOCHROME_BW -> {
                            g2d.setColor((digit % 2 == 0) ? Color.BLACK : Color.WHITE);
                            g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        }
                        case NUMBERS -> {
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                            g2d.setColor(Color.BLACK);
                            g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));
                            g2d.drawString(String.valueOf(digit), x + 4, y + 13);
                        }
                        case ENGLISH_LETTERS -> {
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                            g2d.setColor(Color.BLUE.darker());
                            g2d.setFont(new Font("Monospaced", Font.BOLD, 11));
                            
                            int nextDigit = getPiDigitAt(absoluteIndex + 1);
                            char letter = (char) ('A' + ((digit * 10 + nextDigit) % 26));
                            g2d.drawString(String.valueOf(letter), x + 4, y + 13);
                        }
                    }

                    g2d.setColor(new Color(120, 120, 120, 60));
                    g2d.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                    if (selectionStart >= 0) {
                        long minSel = Math.min(selectionStart, selectionEnd);
                        long maxSel = Math.max(selectionStart, selectionEnd);

                        if (absoluteIndex >= minSel && absoluteIndex <= maxSel) {
                            g2d.setColor(new Color(255, 0, 0, 110));
                            g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                            g2d.setColor(Color.YELLOW);
                            g2d.drawRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Punto de entrada principal de la aplicación Java.
     *
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PiPatternAnalyzer app = new PiPatternAnalyzer();
            app.setVisible(true);
        });
    }
}