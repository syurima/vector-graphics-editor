import java.awt.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.*;
import javax.imageio.*;

enum ShapeType {
    LINE, RECTANGLE, CIRCLE
}

enum OperationType {
    DRAW, EDIT
}

public class Main {
    public static void main(String[] args) {
        VectorGraphicsEditorWindow window = new VectorGraphicsEditorWindow();
        window.setVisible(true);
    }
}

class VectorGraphicsEditorWindow extends JFrame {
    public VectorGraphicsEditorWindow() {
        setTitle("Vector Graphics Editor");
        setSize(800, 600);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        VectorGraphicsEditor editorPanel = new VectorGraphicsEditor();
        add(editorPanel, BorderLayout.CENTER);

        setVisible(true);
    }
}

class VectorGraphicsEditor extends JPanel {
    private final DrawPanel drawPanel;
    private final OperationPanel operationPanel;
    private final ControlPanel controlPanel;

    private ShapeType selectedShape = ShapeType.LINE;
    private OperationType selectedOperation = OperationType.DRAW;
    private Color currentColor = Color.BLACK;

    public VectorGraphicsEditor() {
        setLayout(new BorderLayout());

        // Draw panel
        drawPanel = new DrawPanel();
        add(drawPanel, BorderLayout.CENTER);

        // Operation panel
        operationPanel = new OperationPanel();
        add(operationPanel, BorderLayout.NORTH);

        // Control panel
        controlPanel = new ControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private class DrawPanel extends JPanel 
    {
        private final ArrayList<Shape> shapes = new ArrayList<>();
        private Shape currentShape = null; // Shape being drawn/edited dynamically
        private Point selectedLineEnd = null;
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
                                if (currentShape != null) {
                                    shapes.add(currentShape); // Add the finalized shape to the list
                                }
                                currentShape = null;
                            }
                            break;
                        case EDIT:
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                currentShape = null;
                                selectedLineEnd = null;
                            }
                            break;
                    }
                    repaint(); // Repaint the panel to show the finalized shape
                }
            });
    
            addMouseMotionListener(new MouseMotionAdapter() {
                // mouse dragged event
                @Override
                public void mouseDragged(MouseEvent e) {
                    switch(selectedOperation) {
                        case DRAW:
                            if (SwingUtilities.isLeftMouseButton(e)) {dynamicDrawing(e);} // Draw the shape dynamically
                            break;
                        case EDIT:
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                if (currentShape instanceof Line && selectedLineEnd != null) {
                                    dynamicLineEndEditing(e); // Move the selected line end
                                } else {
                                    dynamicMoving(e); // Move the selected shape
                                }
                            } 
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

        private void dynamicLineEndEditing(MouseEvent e) {
            Graphics g = getGraphics();
            g.setXORMode(getBackground()); // XOR mode for dynamic drawing (better visibility)
            g.setColor(currentColor);
        
            if (currentShape != null) {
                currentShape.draw(g); // Erase the previous shape by drawing it in XOR mode
            }
        
            // Update the position of the selected line end
            selectedLineEnd.setLocation(e.getPoint());
            if (currentShape instanceof Line) {
                Line line = (Line) currentShape;
                line.recalculateCenter();
            }
        
            // draw the updated line
            currentShape.draw(g);
        
            g.dispose();
        }
        
        // Try to find a shape that is in range of the click point (selects one at a time)
        private void selectShape() {
            for (Shape shape : shapes) {
                Point center = shape.getCenter();
                if (center.distance(lastClickPoint) <= CLICK_RADIUS) {
                    currentShape = shape; // Shape selected
                    return;
                }
                else if (shape instanceof Line) {
                    Line line = (Line) shape;
                    if (line.getStart().distance(lastClickPoint) <= CLICK_RADIUS) {
                        currentShape = line;
                        selectedLineEnd = line.getStart(); // Line start selected
                        return;
                    } else if (line.getEnd().distance(lastClickPoint) <= CLICK_RADIUS) {
                        currentShape = line;
                        selectedLineEnd = line.getEnd(); // Line end selected
                        return;
                    }
                }
            }
            currentShape = null;
            selectedLineEnd = null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (Shape shape : shapes) {
                shape.draw(g); // Draw all finalized shapes
            }
        }

        public void clear() {
            shapes.clear();
            repaint();
        }

        public void saveShapes() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Shapes");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("txt", "txt"));
            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try (PrintWriter writer = new PrintWriter(fileToSave)) {
                    for (Shape shape : shapes) {
                        writer.println(shape.toString());
                    }
                    JOptionPane.showMessageDialog(this, "Shapes saved successfully!");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
                }
            }
        }
    
        public void loadShapes() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load Canvas");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("txt", "txt"));
            int userSelection = fileChooser.showOpenDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToLoad = fileChooser.getSelectedFile();
                shapes.clear(); // Clear existing shapes before loading new ones

                try (BufferedReader reader = new BufferedReader(new FileReader(fileToLoad))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Shape shape = Shape.fromString(line);
                        if (shape != null) {
                            shapes.add(shape);
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Canvas loaded successfully!");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage());
                }
                repaint();
            }
        }

        public void exportAsImage() {
            // Create a BufferedImage with the same dimensions as the DrawPanel
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
        
            // Paint the DrawPanel's contents onto the BufferedImage
            this.paint(g2d);
            g2d.dispose();
        
            // Prompt the user to select a file location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export as Image");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("jpg", "jpg"));
            int userSelection = fileChooser.showSaveDialog(this);
        
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                // Ensure the file has a .jpg extension
                if (!fileToSave.getName().toLowerCase().endsWith(".jpg")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".jpg");
                }
        
                try {
                    // Write the BufferedImage to the file
                    ImageIO.write(image, "jpg", fileToSave);
                    JOptionPane.showMessageDialog(this, "Image exported successfully!");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error exporting image: " + e.getMessage());
                }
            }
        }
    }

    private class ControlPanel extends JPanel {
        private JTextField rField, gField, bField;
        private JPanel colorPreview;

        public ControlPanel() {
            //Shape selection buttons
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
            
            this.add(lineButton);
            this.add(rectButton);
            this.add(circleButton);

            // Color selection fields
            rField = new JTextField("0", 3);
            gField = new JTextField("0", 3);
            bField = new JTextField("0", 3);
            JButton colorButton = new JButton("Set Color");
            colorButton.addActionListener(e -> setColor());

            this.add(new JLabel("R:"));
            this.add(rField);
            this.add(new JLabel("G:"));
            this.add(gField);
            this.add(new JLabel("B:"));
            this.add(bField);
            this.add(colorButton);

            // Color preview
            colorPreview = new JPanel();
            colorPreview.setPreferredSize(new Dimension(50, 20));
            colorPreview.setBackground(currentColor);
            colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            this.add(colorPreview);
        }

        public void setColor() {
            try {
                int r = Integer.parseInt(controlPanel.rField.getText());
                int g = Integer.parseInt(controlPanel.gField.getText());
                int b = Integer.parseInt(controlPanel.bField.getText());
    
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
    }

    private class OperationPanel extends JPanel {
        private final JButton exportButton, saveButton, loadButton, clearButton;
        private final JRadioButton drawButton, editButton;

        public OperationPanel() {
            this.add(new JLabel("Mode:"));

            drawButton = new JRadioButton("Draw", true);
            editButton = new JRadioButton("Edit");
            ButtonGroup operationGroup = new ButtonGroup();
            operationGroup.add(drawButton);
            operationGroup.add(editButton);

            drawButton.addActionListener(e -> selectedOperation = OperationType.DRAW);
            editButton.addActionListener(e -> selectedOperation = OperationType.EDIT);

            this.add(drawButton);
            this.add(editButton);

            // Spacer
            this.add(Box.createHorizontalStrut(200));

            // Save, load, and clear buttons
            exportButton = new JButton("Export");
            exportButton.addActionListener(e -> drawPanel.exportAsImage());
            saveButton = new JButton("Save");
            saveButton.addActionListener(e -> drawPanel.saveShapes());
            loadButton = new JButton("Load");
            loadButton.addActionListener(e -> drawPanel.loadShapes());
            clearButton = new JButton("Clear");
            clearButton.addActionListener(e -> drawPanel.clear());

            this.add(exportButton);
            this.add(saveButton);
            this.add(loadButton);
            this.add(clearButton);
        }
    }
}