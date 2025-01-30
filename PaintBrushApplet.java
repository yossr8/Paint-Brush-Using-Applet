import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class PaintBrushApplet extends JApplet {
    private Color selectedColor = Color.BLACK;
    private String selectedShape = "FreeHand";
    private boolean isDotted = false;
    private boolean isFilled = false;
    private Point startPoint, endPoint;
    private BufferedImage canvas;
    private Graphics2D g2d;
    private Stack<BufferedImage> undoStack = new Stack<>();

    @Override
    public void init() {
        setSize(800, 600);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        // Color Buttons
        addColorButton(controlPanel, "Red", Color.RED);
        addColorButton(controlPanel, "Green", Color.GREEN);
        addColorButton(controlPanel, "Blue", Color.BLUE);
		addColorButton(controlPanel, "Black", Color.BLACK);

        // Shape Buttons
        addShapeButton(controlPanel, "Rectangle");
        addShapeButton(controlPanel, "Oval");
        addShapeButton(controlPanel, "Line");
        addShapeButton(controlPanel, "FreeHand");
        addShapeButton(controlPanel, "Eraser");

        // Clear All Button
        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> clearCanvas());
        controlPanel.add(clearButton);

        // Dotted and Filled Checkboxes
        JCheckBox dottedCheckbox = new JCheckBox("Dotted");
        dottedCheckbox.addActionListener(e -> isDotted = dottedCheckbox.isSelected());
        controlPanel.add(dottedCheckbox);

        JCheckBox filledCheckbox = new JCheckBox("Filled");
        filledCheckbox.addActionListener(e -> isFilled = filledCheckbox.isSelected());
        controlPanel.add(filledCheckbox);

        // Undo Button
        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> undoAction());
        controlPanel.add(undoButton);

        add(controlPanel, BorderLayout.NORTH);

        // Drawing Area
        canvas = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
        g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2d.setColor(selectedColor);

        JLabel drawingArea = new JLabel(new ImageIcon(canvas));
        drawingArea.setOpaque(true);
        drawingArea.setBackground(Color.WHITE);

        drawingArea.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                saveState(); // Save the canvas state before drawing
            }

            public void mouseReleased(MouseEvent e) {
                drawShape(e.getPoint());
            }
        });

        drawingArea.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (selectedShape.equals("FreeHand") || selectedShape.equals("Eraser")) {
                    drawShape(e.getPoint());
                }
            }
        });

        add(new JScrollPane(drawingArea), BorderLayout.CENTER);

        // Resize Listener for Dynamic Canvas
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeCanvas(drawingArea);
            }
        });
    }

    private void addColorButton(JPanel panel, String name, Color color) {
        JButton button = new JButton(name);
        button.setBackground(color);
        button.addActionListener(e -> selectedColor = color);
        panel.add(button);
    }

    private void addShapeButton(JPanel panel, String shape) {
        JButton button = new JButton(shape);
        button.addActionListener(e -> selectedShape = shape);
        panel.add(button);
    }

    private void clearCanvas() {
        saveState(); // Save the state before clearing
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2d.setColor(selectedColor);
        repaint();
    }

    private void saveState() {
        BufferedImage state = new BufferedImage(canvas.getWidth(), canvas.getHeight(), canvas.getType());
        Graphics2D g = state.createGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
        undoStack.push(state);
    }

    private void undoAction() {
        if (!undoStack.isEmpty()) {
            BufferedImage previousState = undoStack.pop();
            Graphics g = canvas.getGraphics();
            g.drawImage(previousState, 0, 0, null); // Restore the previous state
            g.dispose();
            g2d = (Graphics2D) canvas.getGraphics(); // Update the Graphics2D object
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Nothing to undo!");
        }
    }

    private void drawShape(Point endPoint) {
        this.endPoint = endPoint;
        g2d.setColor(selectedColor);

        Stroke originalStroke = g2d.getStroke(); // Save the original stroke

        if (isDotted) {
            float[] dashPattern = {5f, 5f}; // Dash length and spacing
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, dashPattern, 0f));
        }

        if (selectedShape.equals("FreeHand")) {
            g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            startPoint = endPoint;
        } else if (selectedShape.equals("Eraser")) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(endPoint.x - 10, endPoint.y - 10, 20, 20);
        } else {
            int x = Math.min(startPoint.x, endPoint.x);
            int y = Math.min(startPoint.y, endPoint.y);
            int width = Math.abs(startPoint.x - endPoint.x);
            int height = Math.abs(startPoint.y - endPoint.y);

            if (selectedShape.equals("Rectangle")) {
                if (isFilled) {
                    g2d.fillRect(x, y, width, height);
                } else {
                    g2d.drawRect(x, y, width, height);
                }
            } else if (selectedShape.equals("Oval")) {
                if (isFilled) {
                    g2d.fillOval(x, y, width, height);
                } else {
                    g2d.drawOval(x, y, width, height);
                }
            } else if (selectedShape.equals("Line")) {
                g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
            }
        }

        g2d.setStroke(originalStroke); // Restore the original stroke
        repaint();
    }

    private void resizeCanvas(JLabel drawingArea) {
        int width = drawingArea.getWidth();
        int height = drawingArea.getHeight();

        if (width > 0 && height > 0) {
            BufferedImage newCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D newG2d = newCanvas.createGraphics();

            // Copy existing content to the new canvas
            newG2d.setColor(Color.WHITE);
            newG2d.fillRect(0, 0, width, height);
            newG2d.drawImage(canvas, 0, 0, null);

            // Update the canvas and graphics context
            canvas = newCanvas;
            g2d = newG2d;
            drawingArea.setIcon(new ImageIcon(canvas));
            repaint();
        }
    }
}
