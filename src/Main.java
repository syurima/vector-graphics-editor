import java.awt.*;
import javax.swing.*;

import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.*;


enum ShapeType {
    LINE, RECTANGLE, CIRCLE
}

enum OperationType {
    DRAW, MOVE, DELETE
}

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(VectorGraphicsEditorWindow::new);
    }
}

class VectorGraphicsEditorWindow extends JFrame {
    public VectorGraphicsEditorWindow() {
        setTitle("Vector Graphics Editor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        VectorGraphicsEditor editorPanel = new VectorGraphicsEditor();
        add(editorPanel, BorderLayout.CENTER);

        setVisible(true);
    }
}

class VectorGraphicsEditor extends JPanel {
    private final DrawPanel drawPanel;

    private final JRadioButton lineButton, rectButton, circleButton;
    private ShapeType selectedShape = ShapeType.LINE;

    private final JTextField rField, gField, bField;
    private Color currentColor = Color.BLACK;

    private final JButton saveButton, loadButton, clearButton;

    public VectorGraphicsEditor() {
        setLayout(new BorderLayout());

        drawPanel = new DrawPanel();
        add(drawPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        lineButton = new JRadioButton("Line", true);
        rectButton = new JRadioButton("Rectangle");
        circleButton = new JRadioButton("Circle");
        ButtonGroup group = new ButtonGroup();
        group.add(lineButton);
        group.add(rectButton);
        group.add(circleButton);

        lineButton.addActionListener(e -> selectedShape = ShapeType.LINE);
        rectButton.addActionListener(e -> selectedShape = ShapeType.RECTANGLE);
        circleButton.addActionListener(e -> selectedShape = ShapeType.CIRCLE);
        
        controlPanel.add(lineButton);
        controlPanel.add(rectButton);
        controlPanel.add(circleButton);

        rField = new JTextField("0", 3);
        gField = new JTextField("0", 3);
        bField = new JTextField("0", 3);
        JButton colorButton = new JButton("Set Color");
        colorButton.addActionListener(e -> setColor());

        controlPanel.add(new JLabel("R:"));
        controlPanel.add(rField);
        controlPanel.add(new JLabel("G:"));
        controlPanel.add(gField);
        controlPanel.add(new JLabel("B:"));
        controlPanel.add(bField);
        controlPanel.add(colorButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> drawPanel.saveShapes());
        loadButton = new JButton("Load");
        loadButton.addActionListener(e -> drawPanel.loadShapes());
        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> drawPanel.clear());

        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        controlPanel.add(clearButton);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void setColor() {
        try {
            int r = Integer.parseInt(rField.getText());
            int g = Integer.parseInt(gField.getText());
            int b = Integer.parseInt(bField.getText());

            // // Clamp values to 0-255
            // r = Math.max(0, Math.min(255, r));
            // g = Math.max(0, Math.min(255, g));
            // b = Math.max(0, Math.min(255, b));
            
            currentColor = new Color(r, g, b);
        } catch (Exception e) { // Values are not integers or out of range
            JOptionPane.showMessageDialog(this, "Invalid color values. Please enter integers between 0 and 255.");
        }
    }

    private class DrawPanel extends JPanel {
        private final ArrayList<Shape> shapes = new ArrayList<>();
        private Shape currentShape = null; // Shape being drawn dynamically
        private Point startPoint;
    
        public DrawPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                }
    
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (currentShape != null) {
                        shapes.add(currentShape); // Add the final shape to the list
                        currentShape = null; // Reset the current shape
                        repaint();
                    }
                }
            });
    
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    Point endPoint = e.getPoint();
                    switch (selectedShape) {
                        case LINE:
                            currentShape = new Line(startPoint, endPoint, currentColor);
                            break;
                        case RECTANGLE:
                            currentShape = new Rect(startPoint, endPoint, currentColor);
                            break;
                        case CIRCLE:
                            currentShape = new Circle(startPoint, endPoint, currentColor);
                            break;
                    }
                    repaint(); // Repaint to show the dynamic shape
                }
            });
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (Shape shape : shapes) {
                shape.draw(g); // Draw all finalized shapes
            }
            if (currentShape != null) {
                currentShape.draw(g); // Draw the shape being dynamically drawn
            }
        }

        public void saveShapes() {
            try (PrintWriter writer = new PrintWriter(new File("shapes.txt"))) {
                for (Shape shape : shapes) {
                    writer.println(shape.toString());
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file");
            }
        }
    
        public void loadShapes() {
            try (BufferedReader reader = new BufferedReader(new FileReader("shapes.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Shape shape = Shape.fromString(line);
                    if (shape != null) {
                        shapes.add(shape);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading file");
            }
            repaint();
        }

        public void clear() {
            shapes.clear();
            repaint();
        }
    }
}

interface Shape {
    void draw(Graphics g);
    String toString(); // Serialize the shape to a string representation
    static Shape fromString(String s) { // Deserialize the shape from a string representation
        // split the string into parts
        String[] s_split = s.split(" ");
        String shapeType = s_split[0];
        int x = Integer.parseInt(s_split[1]);
        int y = Integer.parseInt(s_split[2]);
        String color = s_split[s_split.length - 1];
        switch (shapeType) {
            case "LINE":
                Point start = new Point(x, y);
                Point end = new Point(Integer.parseInt(s_split[3]), Integer.parseInt(s_split[4]));
                return new Line(start, end, new Color(Integer.parseInt(color)));
            case "RECTANGLE":
                int width = Integer.parseInt(s_split[3]);
                int height = Integer.parseInt(s_split[4]);
                return new Rect(new Point(x, y), new Point(x + width, y + height), new Color(Integer.parseInt(color)));
            case "CIRCLE":
                int radius = Integer.parseInt(s_split[3]);
                return new Circle(new Point(x, y), radius, new Color(Integer.parseInt(color)));
            default:
                return null; // Unknown shape type
        }
    }
}

class Line implements Shape {
    private final Point start, end;
    private final Color color;

    public Line(Point start, Point end, Color color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }
    
    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawLine(start.x, start.y, end.x, end.y);
    }

    @Override
    public String toString() {
        return "LINE " + start.x + " " + start.y + " " + end.x + " " + end.y + " " + color.getRGB();
    }
}

class Rect implements Shape {
    private final Rectangle rect;
    private final Color color;

    public Rect(Point start, Point end, Color color) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        this.rect = new Rectangle(x, y, width, height);
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public String toString() {
        return "RECTANGLE " + rect.x + " " + rect.y + " " + rect.width + " " + rect.height + " " + color.getRGB();
    }
}

class Circle implements Shape {
    private final int x, y, radius;
    private final Color color;

    public Circle(Point center, Point edge, Color color) {
        int dx = edge.x - center.x;
        int dy = edge.y - center.y;
        this.radius = (int) Math.sqrt(dx * dx + dy * dy);
        this.x = center.x - radius;
        this.y = center.y - radius;
        this.color = color;
    }

    public Circle(Point center, int radius, Color color) {
        this.x = center.x - radius;
        this.y = center.y - radius;
        this.radius = radius;
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x, y, radius * 2, radius * 2);
    }

    @Override
    public String toString() {
        return "CIRCLE " + (x + radius) + " " + (y + radius) + " " + radius + " " + color.getRGB();
    }
}
