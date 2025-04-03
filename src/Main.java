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
    DRAW, EDIT
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

    private ShapeType selectedShape = ShapeType.LINE;
    private final JRadioButton drawButton, editButton;
    private OperationType selectedOperation = OperationType.DRAW;

    private final JTextField rField, gField, bField;
    private final JPanel colorPreview;
    private Color currentColor = Color.BLACK;

    private final JButton saveButton, loadButton, clearButton;

    public VectorGraphicsEditor() {
        setLayout(new BorderLayout());

        // Draw panel
        drawPanel = new DrawPanel();
        add(drawPanel, BorderLayout.CENTER);

        //Shape selection buttons
        JPanel controlPanel = new JPanel();
        JRadioButton lineButton, rectButton, circleButton;
        lineButton = new JRadioButton("Line", true);
        rectButton = new JRadioButton("Rectangle");
        circleButton = new JRadioButton("Circle");
        ButtonGroup shapeGroup = new ButtonGroup();
        shapeGroup.add(lineButton);
        shapeGroup.add(rectButton);
        shapeGroup.add(circleButton);

        lineButton.addActionListener(e -> selectedShape = ShapeType.LINE);
        rectButton.addActionListener(e -> selectedShape = ShapeType.RECTANGLE);
        circleButton.addActionListener(e -> selectedShape = ShapeType.CIRCLE);
        
        controlPanel.add(lineButton);
        controlPanel.add(rectButton);
        controlPanel.add(circleButton);

        

        // Color selection fields
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

        // Color preview
        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(50, 20));
        colorPreview.setBackground(currentColor);
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        controlPanel.add(colorPreview);

        
        add(controlPanel, BorderLayout.SOUTH);

        // Operation Panel
        JPanel operationPanel = new JPanel();

        // Operation selection buttons
        operationPanel.add(new JLabel("Mode:"));

        drawButton = new JRadioButton("Draw", true);
        editButton = new JRadioButton("Edit");
        ButtonGroup operationGroup = new ButtonGroup();
        operationGroup.add(drawButton);
        operationGroup.add(editButton);

        drawButton.addActionListener(e -> selectedOperation = OperationType.DRAW);
        editButton.addActionListener(e -> selectedOperation = OperationType.EDIT);

        operationPanel.add(drawButton);
        operationPanel.add(editButton);

        // Spacer
        operationPanel.add(Box.createHorizontalStrut(200));

        // Save, load, and clear buttons
        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> drawPanel.saveShapes());
        loadButton = new JButton("Load");
        loadButton.addActionListener(e -> drawPanel.loadShapes());
        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> drawPanel.clear());

        operationPanel.add(saveButton);
        operationPanel.add(loadButton);
        operationPanel.add(clearButton);


        add(operationPanel, BorderLayout.NORTH);
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
            colorPreview.setBackground(currentColor); // Update preview
        } catch (Exception e) { 
            // Values are not integers or out of range
            JOptionPane.showMessageDialog(this, "Invalid color values. Please enter integers between 0 and 255.");
        }
    }

    private class DrawPanel extends JPanel {
        private final ArrayList<Shape> shapes = new ArrayList<>();
        private Shape currentShape = null; // Shape being drawn/edited dynamically
        private Point lastClickPoint;

        private static final int CLICK_RADIUS = 40;
    
        public DrawPanel() {
            addMouseListener(new MouseAdapter() {
                // mouse pressed event
                @Override
                public void mousePressed(MouseEvent e) {
                    lastClickPoint = e.getPoint(); // Store clicked point

                    
                    if (SwingUtilities.isRightMouseButton(e)) { // Right-click in any mode to delete a shape
                        selectShape();
                        // If a shape was found, remove it
                        if (currentShape != null) {
                            shapes.remove(currentShape);
                            currentShape = null;
                            repaint();
                        }
                    } 
                    else if (SwingUtilities.isLeftMouseButton(e)) { // Left-click 
                        switch (selectedOperation) {
                            case DRAW:
                                break;
                            case EDIT:
                                selectShape(); // Select a shape to edit
                                break;
                        }
                    }
                }
                
                // mouse released event
                @Override
                public void mouseReleased(MouseEvent e) {
                    switch (selectedOperation) {
                        case DRAW:
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                shapes.add(currentShape);
                                currentShape = null;
                            }
                            break;
                        case EDIT:
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                currentShape = null;
                            }
                            break;
                    }
                    repaint(); // Repaint the panel to show the finalized shape
                }
            });
    
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    switch(selectedOperation) {
                        case DRAW:
                            if (SwingUtilities.isLeftMouseButton(e)) {dynamicDrawing(e);} // Draw the shape dynamically
                            break;
                        case EDIT:
                            if (SwingUtilities.isLeftMouseButton(e)) {dynamicMoving(e);} // Move the shape dynamically
                            break;
                    }
                }
            });

        }

        // OPERATIONS

        // Dynamic drawing of new shapes when mouse is dragged
        private void dynamicDrawing(MouseEvent e){
            Graphics g = getGraphics();
                g.setXORMode(getBackground()); // XOR mode for dynamic drawing (better visibility)
                g.setColor(currentColor);

                if (currentShape != null) {
                    currentShape.draw(g); // Erase the previous shape by drawing it in XOR mode
                }
                Point startPoint = lastClickPoint;
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

                currentShape.draw(g);
            
                g.dispose();
        };

        // Dynamic display when moving shapes
        private void dynamicMoving(MouseEvent e) {
            Point clickPoint = e.getPoint();

            Graphics g = getGraphics();
            g.setXORMode(getBackground()); // XOR mode for dynamic drawing (better visibility)
            g.setColor(currentColor);

            
            if (currentShape != null) {
                currentShape.draw(g); // Erase the previous shape by drawing it in XOR mode

                int dx = clickPoint.x - lastClickPoint.x;
                int dy = clickPoint.y - lastClickPoint.y;
                lastClickPoint = clickPoint;

                //move the shape and draw it at the new position
                currentShape.move(dx, dy);
                currentShape.draw(g);
            }
        
            g.dispose();
        };
        
        // Try to find a shape that is in range of the click point (selects one at a time)
        private void selectShape() {
            for (Shape shape : shapes) {
                Point center = shape.getCenter();
                if (center.distance(lastClickPoint) <= CLICK_RADIUS) {
                    currentShape = shape;
                    break;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (Shape shape : shapes) {
                shape.draw(g); // Draw all finalized shapes
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
            shapes.clear(); // Clear existing shapes before loading new ones

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
    String toString();                      // Serialize the shape to a string representation
    static Shape fromString(String s) {     // Deserialize the shape from a string representation
        try{
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
        catch (Exception e) {
            return null; // Return null if the string is not a valid shape representation
        }
    }
    Point getCenter();
    public void move(int dx, int dy);
}

class Line implements Shape {
    private final Point start, end;
    private final Point center;
    private final Color color;

    public Line(Point start, Point end, Color color) {
        this.start = start;
        this.end = end;
        this.center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
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
    @Override
    public Point getCenter() {
        return center;
    }

    @Override
    public void move(int dx, int dy) {
        start.translate(dx, dy);
        end.translate(dx, dy);
        center.translate(dx, dy);
    }
}

class Rect implements Shape {
    private final Rectangle rect;
    private final Point center;
    private final Color color;

    public Rect(Point start, Point end, Color color) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        this.rect = new Rectangle(x, y, width, height);
        this.center = new Point(x + width / 2, y + height / 2);
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
    @Override
    public Point getCenter() {
        return center;
    }
    @Override
    public void move(int dx, int dy) {
        rect.translate(dx, dy);
        center.translate(dx, dy);
    }
}

class Circle implements Shape {
    private final Point center;
    private final int radius;
    private final Color color;

    public Circle(Point center, Point edge, Color color) {
        int dx = edge.x - center.x;
        int dy = edge.y - center.y;
        this.radius = (int) Math.sqrt(dx * dx + dy * dy);
        this.center = center;
        this.color = color;
    }

    public Circle(Point center, int radius, Color color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
    }

    @Override
    public String toString() {
        return "CIRCLE " + (center.x) + " " + (center.y) + " " + radius + " " + color.getRGB();
    }

    @Override
    public Point getCenter() {
        return center;
    }
    @Override
    public void move(int dx, int dy) {
        center.translate(dx, dy);
    }
}
