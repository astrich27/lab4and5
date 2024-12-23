import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {
    private Double[][] graphicsData;
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean showHorizontalLines = true;

    private double minX, maxX, minY, maxY, scaleX, scaleY;
    private boolean isDragging = false;
    private Point dragStart = null;
    private Rectangle dragRect = null;

    // Добавляем поле для хранения выделенной точки
    private Point2D.Double highlightedPoint = null;

    public GraphicsDisplay() {
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    resetScale(); // Восстановление исходного масштаба
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true; // Начало выделения
                    dragStart = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    if (dragRect != null && dragRect.width > 0 && dragRect.height > 0) {
                        scaleToArea(dragRect); // Масштабирование выделенной области
                    }
                    isDragging = false;
                    dragRect = null;
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHighlightedPoint(e); // Обновляем выделенную точку при движении мыши
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    dragRect = new Rectangle(dragStart);
                    dragRect.add(e.getPoint()); // Рисование рамки выделения
                    repaint();
                }
            }
        });
    }

    // Метод для обновления выделенной точки
    private void updateHighlightedPoint(MouseEvent e) {
        if (graphicsData == null) return;

        Point mousePoint = e.getPoint();
        Point2D.Double newHighlightedPoint = null;

        for (Double[] point : graphicsData) {
            Point2D.Double graphPoint = xyToPoint(point[0], point[1]);
            if (Math.abs(graphPoint.x - mousePoint.x) < 5 && Math.abs(graphPoint.y - mousePoint.y) < 5) {
                newHighlightedPoint = new Point2D.Double(point[0], point[1]);
                break;
            }
        }

        // Если выделенная точка изменилась, обновляем и перерисовываем
        if ((highlightedPoint == null && newHighlightedPoint != null)
                || (highlightedPoint != null && !highlightedPoint.equals(newHighlightedPoint))) {
            highlightedPoint = newHighlightedPoint;
            repaint(); // Перерисовка панели
        }
    }

    private void scaleToArea(Rectangle rect) {
        double newMinX = minX + (rect.x / scaleX);
        double newMaxX = minX + ((rect.x + rect.width) / scaleX);
        double newMinY = maxY - ((rect.y + rect.height) / scaleY);
        double newMaxY = maxY - (rect.y / scaleY);

        minX = newMinX;
        maxX = newMaxX;
        minY = newMinY;
        maxY = newMaxY;

        scaleX = getWidth() / (maxX - minX);
        scaleY = getHeight() / (maxY - minY);

        repaint();
    }

    private void resetScale() {
        calculateBounds(); // Восстановление границ
        repaint();
    }

    public void showGraphics(Double[][] graphicsData) {
        this.graphicsData = graphicsData;
        calculateBounds();
        repaint();
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setShowHorizontalLines(boolean showHorizontalLines) {
        this.showHorizontalLines = showHorizontalLines;
        repaint();
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        double deltaX = x - minX;
        double deltaY = maxY - y;
        return new Point2D.Double(deltaX * scaleX, deltaY * scaleY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graphicsData == null || graphicsData.length == 0) return;

        Graphics2D canvas = (Graphics2D) g;
        if (showAxis) paintAxis(canvas);
        if (showHorizontalLines) paintHorizontalLines(canvas);
        paintGraphics(canvas);
        if (showMarkers) paintMarkers(canvas);

        if (dragRect != null) { // Рисование рамки выделения
            canvas.setColor(Color.BLACK);
            canvas.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 0, new float[]{6, 6}, 0));
            canvas.draw(dragRect);
        }

        // Рисуем координаты выделенной точки, если она есть
        if (highlightedPoint != null) {
            Point2D.Double graphPoint = xyToPoint(highlightedPoint.x, highlightedPoint.y);
            canvas.setColor(Color.BLACK);
            canvas.drawString(String.format("(%.2f, %.2f)", highlightedPoint.x, highlightedPoint.y),
                    (int) graphPoint.x + 5, (int) graphPoint.y - 5);
        }
    }

    private void calculateBounds() {
        minX = graphicsData[0][0];
        maxX = graphicsData[0][0];
        minY = graphicsData[0][1];
        maxY = graphicsData[0][1];

        for (Double[] point : graphicsData) {
            if (point[0] < minX) minX = point[0];
            if (point[0] > maxX) maxX = point[0];
            if (point[1] < minY) minY = point[1];
            if (point[1] > maxY) maxY = point[1];
        }

        maxX += maxX * 0.25;
        minX -= maxX * 0.25;
        maxY += maxX * 0.2;
        minY -= maxX * 0.1;

        scaleX = getWidth() / (maxX - minX);
        scaleY = getHeight() / (maxY - minY);
    }

    protected void paintAxis(Graphics2D canvas) {
        canvas.setStroke(new BasicStroke(2.0f));
        canvas.setColor(Color.BLACK);

        Point2D.Double xStart = xyToPoint(minX, 0);
        Point2D.Double xEnd = xyToPoint(maxX, 0);
        canvas.draw(new Line2D.Double(xStart, xEnd));

        Point2D.Double yStart = xyToPoint(0, minY);
        Point2D.Double yEnd = xyToPoint(0, maxY);
        canvas.draw(new Line2D.Double(yStart, yEnd));
    }

    protected void paintHorizontalLines(Graphics2D canvas) {
        canvas.setColor(Color.RED);
        float[] dashPattern = {8, 1};
        canvas.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

        double[] fractions = {0.9, 0.5, 0.1};
        for (double fraction : fractions) {
            double y = minY + fraction * (maxY - minY);
            Point2D.Double start = xyToPoint(minX, y);
            Point2D.Double end = xyToPoint(maxX, y);
            canvas.draw(new Line2D.Double(start, end));
        }
    }

    //рисование графика
    protected void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(new BasicStroke(2.0f));
        canvas.setColor(Color.BLACK);
        //рисование линиями
        //!!!!
        float[] dashPattern = {20, 5, 10, 5, 5};
        canvas.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

        GeneralPath graph = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (i == 0) graph.moveTo(point.x, point.y);
            else graph.lineTo(point.x, point.y);
        }
        canvas.draw(graph);
    }

//    protected void paintMarkers(Graphics2D canvas) {
//        canvas.setStroke(new BasicStroke(1.0f));
//        for (Double[] point : graphicsData) {
//            Point2D.Double center = xyToPoint(point[0], point[1]);
//            canvas.setColor(point[1] > (maxY + minY) / 2 ? Color.BLUE : Color.RED);
//
//            GeneralPath triangle = new GeneralPath();
//            triangle.moveTo(center.x, center.y + 5); // Нижняя вершина
//            triangle.lineTo(center.x - 5, center.y - 5); // Левая вершина
//            triangle.lineTo(center.x + 5, center.y - 5); // Правая вершина
//            triangle.closePath();
//
//            canvas.fill(triangle); // Закрашиваем треугольник
//        }
//    }

//    protected void paintMarkers(Graphics2D canvas) {
//        canvas.setStroke(new BasicStroke(1.0f));
//        for (Double[] point : graphicsData) {
//            Point2D.Double center = xyToPoint(point[0], point[1]);
//            canvas.setColor(point[1] > (maxY + minY) / 2 ? Color.BLUE : Color.RED);
//
//            // Радиус круга
//            int radius = 5;
//
//            // Рисуем круг
//            canvas.drawOval((int) center.x - radius, (int) center.y - radius, radius * 2, radius * 2);
//
//            // Рисуем крестик внутри круга
//            canvas.drawLine((int) center.x - radius, (int) center.y - radius,
//                    (int) center.x + radius, (int) center.y + radius);
//            canvas.drawLine((int) center.x - radius, (int) center.y + radius,
//                    (int) center.x + radius, (int) center.y - radius);
//        }
//    }

    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(new BasicStroke(1.0f));
        for (Double[] point : graphicsData) {
            Point2D.Double center = xyToPoint(point[0], point[1]);
            canvas.setColor(point[1] > (maxY + minY) / 2 ? Color.BLUE : Color.RED);

            // Создаём ромб
            GeneralPath diamond = new GeneralPath();
            diamond.moveTo(center.x, center.y - 5); // Верхняя точка
            diamond.lineTo(center.x + 5, center.y); // Правая точка
            diamond.lineTo(center.x, center.y + 5); // Нижняя точка
            diamond.lineTo(center.x - 5, center.y); // Левая точка
            diamond.closePath(); // Замыкаем путь

            canvas.fill(diamond); // Закрашиваем ромб
        }
    }
}