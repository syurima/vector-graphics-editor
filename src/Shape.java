import java.awt.*;


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
            // 3 last vals are RGB values
            int red = Integer.parseInt(s_split[s_split.length - 3]);
            int green = Integer.parseInt(s_split[s_split.length - 2]);
            int blue = Integer.parseInt(s_split[s_split.length - 1]);
            Color color = new Color(red, green, blue);
            switch (shapeType) {
                case "LINE":
                    Point start = new Point(x, y);
                    Point end = new Point(Integer.parseInt(s_split[3]), Integer.parseInt(s_split[4]));
                    return new Line(start, end, color);
                case "RECTANGLE":
                    int width = Integer.parseInt(s_split[3]);
                    int height = Integer.parseInt(s_split[4]);
                    return new Rect(new Point(x, y), new Point(x + width, y + height), color);
                case "CIRCLE":
                    int radius = Integer.parseInt(s_split[3]);
                    return new Circle(new Point(x, y), radius, color);
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
        ((Graphics2D) g).setStroke(new BasicStroke(2));  // Set line weight
        g.setColor(color);
        g.drawLine(start.x, start.y, end.x, end.y);
    }

    @Override
    public String toString() {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        //return "LINE " + start.x + " " + start.y + " " + end.x + " " + end.y + " " + color.getRGB();
        return "LINE " + start.x + " " + start.y + " " + end.x + " " + end.y + " " + red + " " + green + " " + blue;
    }
    @Override
    public Point getCenter() {
        return center;
    }
    public Point getStart() {
        return start;
    }
    public Point getEnd() {
        return end;
    }

    @Override
    public void move(int dx, int dy) {
        start.translate(dx, dy);
        end.translate(dx, dy);
        center.translate(dx, dy);
    }
    public void recalculateCenter() {
        center.setLocation((start.x + end.x) / 2, (start.y + end.y) / 2);
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
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        return "RECTANGLE " + rect.x + " " + rect.y + " " + rect.width + " " + rect.height + " " + red + " " + green + " " + blue;
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
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        return "CIRCLE " + (center.x) + " " + (center.y) + " " + radius + " " + red + " " + green + " " + blue;
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