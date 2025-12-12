/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package ui;

import db.db;
import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.util.HashMap;
import java.util.Map;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import com.toedter.calendar.JDateChooser;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JComboBox;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.util.List;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;

/**
 *
 * @author bathi
 */
public class SalesAnalysis extends javax.swing.JPanel {

    /**
     * Creates new form RegionalSales
     */
    public SalesAnalysis() {
        initComponents();
        loadRegionsToComboBox(regionComboBox);
    }

    //  Load products to combo box    
    public void loadRegionsToComboBox(JComboBox<String> productComboBox) {

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            // Get product names
            String query = "SELECT DISTINCT region FROM transactions ORDER BY region";
            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            productComboBox.removeAllItems();

            productComboBox.addItem("All Regions");

            // Add all products
            while (rs.next()) {
                String regionName = rs.getString("region");
                regionComboBox.addItem(regionName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading products: " + e.getMessage());
        }

    }

    public void dataAnalysis() {

        if (!qtyRadioBtn.isSelected() && !revenueRadioBtn.isSelected()) {
            JOptionPane.showMessageDialog(this, "Select a metric to analyze", "Metric Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String regionName = (String) regionComboBox.getSelectedItem();

        if (regionName == null) {
            JOptionPane.showMessageDialog(this, "Select a region", "Region Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();

        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(this, "Please select dates to analyze", "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean byQuantity = qtyRadioBtn.isSelected();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbDateFormat.format(fromSelectedDate);
        String toDate = dbDateFormat.format(toSelectedDate);

        try {
            updateTable(fromDate, toDate, byQuantity, regionName);
            updateBarChart(fromDate, toDate, byQuantity, regionName);
            updateSidePanel(fromDate, toDate, byQuantity, regionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //  Update table   
    public void updateTable(String fromDate, String toDate, boolean byQuantity, String regionName) {

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String orderBy = byQuantity ? "ORDER BY total_qty DESC" : "ORDER BY total_revenue DESC";

            String query;

            if (regionName.equals("All Regions")) {

                query = "SELECT pro_name, SUM(qty) as total_qty, SUM(total_price) as total_revenue, region "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "GROUP BY pro_name, region "
                        + orderBy;

                pst = con.prepareStatement(query);
                pst.setString(1, fromDate);
                pst.setString(2, toDate);

            } else {

                query = "SELECT pro_name, SUM(qty) as total_qty, SUM(total_price) as total_revenue, region "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "AND region = ? "
                        + "GROUP BY pro_name, region "
                        + orderBy;

                pst = con.prepareStatement(query);
                pst.setString(1, fromDate);
                pst.setString(2, toDate);
                pst.setString(3, regionName);

            }

            rs = pst.executeQuery();
            DefaultTableModel model = (DefaultTableModel) productTable.getModel();
            model.setRowCount(0);

            DecimalFormat df = new DecimalFormat("#,##0.00");
            int rank = 1;

            while (rs.next()) {
                Object[] row = new Object[5];
                row[0] = rank++;
                row[1] = rs.getString("pro_name");
                row[2] = rs.getInt("total_qty");
                row[3] = "Rs. " + df.format(rs.getDouble("total_revenue"));
                row[4] = rs.getString("region");
                model.addRow(row);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating table: " + e.getMessage());
            e.printStackTrace();
        }

    }

    //  Method to update the side panel according to selected date range    
    public void updateSidePanel(String fromDate, String toDate, boolean byQuantity, String regionName) {
        
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            // Get summary statistics
            String summaryQuery;
            if (regionName.equals("All Regions")) {
                summaryQuery = "SELECT COUNT(DISTINCT pro_name) as product_count, "
                        + "SUM(qty) as total_quantity, "
                        + "SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y')";
            } else {
                summaryQuery = "SELECT COUNT(DISTINCT pro_name) as product_count, "
                        + "SUM(qty) as total_quantity, "
                        + "SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "AND region = ?";
            }

            pst = con.prepareStatement(summaryQuery);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            if (!regionName.equals("All Regions")) {
                pst.setString(3, regionName);
            }

            rs = pst.executeQuery();

            if (rs.next()) {
                // Update labels with summary data
                String totalProducts = String.valueOf(rs.getInt("product_count"));
                String totalQuantity = String.valueOf(rs.getInt("total_quantity"));

                DecimalFormat df = new DecimalFormat("#,##0.00");
                String revenueStr = "Rs. " + df.format(rs.getDouble("total_revenue"));

                totalProductLabel.setText(totalProducts);
                totalSoldLabel.setText(totalQuantity);
                totalRevenueLabel.setText(revenueStr);
            }

            rs.close();
            pst.close();

            String orderBy;
            if (byQuantity) {
                orderBy = "ORDER BY SUM(qty) DESC";
            } else {
                orderBy = "ORDER BY SUM(total_price) DESC";
            }

            String topProductQuery;
            if (regionName.equals("All Regions")) {
                topProductQuery = "SELECT pro_name "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "GROUP BY pro_name "
                        + orderBy + " LIMIT 1";
            } else {
                topProductQuery = "SELECT pro_name "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "AND region = ? "
                        + "GROUP BY pro_name "
                        + orderBy + " LIMIT 1";
            }

            pst = con.prepareStatement(topProductQuery);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            if (!regionName.equals("All Regions")) {
                pst.setString(3, regionName);
            }

            rs = pst.executeQuery();

            if (rs.next()) {
                String topProduct = rs.getString("pro_name");
                topProductLabel.setText(topProduct != null ? topProduct : "No data");
            } else {
                topProductLabel.setText("No data");
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Set default values on error
            topProductLabel.setText("Error");
            totalProductLabel.setText("0");
            totalSoldLabel.setText("0");
            totalRevenueLabel.setText("Rs. 0.00");
        }
        
    }

    public Map<String, Double> getTopProductsByDateRange(String fromDate, String toDate, boolean byQuantity, String regionName) {

        Map<String, Double> productSales = new HashMap<>();

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String orderBy = byQuantity ? "ORDER BY total_qty DESC" : "ORDER BY total_revenue DESC";

            String query;
            if (regionName.equals("All Regions")) {
                query = "SELECT pro_name, SUM(qty) as total_qty, SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "GROUP BY pro_name "
                        + orderBy + " "
                        + "LIMIT 10";
            } else {
                query = "SELECT pro_name, SUM(qty) as total_qty, SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "AND region = ? "
                        + "GROUP BY pro_name "
                        + orderBy + " "
                        + "LIMIT 10";
            }

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            if (!regionName.equals("All Regions")) {
                pst.setString(3, regionName);
            }

            rs = pst.executeQuery();

            while (rs.next()) {
                String product = rs.getString("pro_name");
                double value;
                if (byQuantity) {
                    value = rs.getDouble("total_qty"); // Quantity
                } else {
                    value = rs.getDouble("total_revenue"); // Revenue
                }
                productSales.put(product, value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return productSales;
    }

    private void updateBarChart(String fromDate, String toDate, boolean byQuantity, String regionName) {

        Map<String, Double> data = getTopProductsByDateRange(fromDate, toDate, byQuantity, regionName);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        int count = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {

            if (count >= 10) {
                break;
            }

            dataset.addValue(entry.getValue(), "Sales", entry.getKey());
            count++;
        }

        String chartTitle;
        String yAxisLabel;

        if (byQuantity) {
            chartTitle = "Top 10 Products by Quantity Sold";
            yAxisLabel = "Quantity Sold";
        } else {
            chartTitle = "Top 10 Products by Revenue";
            yAxisLabel = "Revenue (Rs.)";
        }

        if (!regionName.equals("All Regions")) {
            chartTitle += " - " + regionName;
        }

        JFreeChart barChart = ChartFactory.createBarChart(chartTitle, "Product", yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);

        // Customize appearance
        barChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));

        CategoryPlot plot = barChart.getCategoryPlot();

        // horizontal bars for product name
        plot.setOrientation(PlotOrientation.HORIZONTAL);

        // Color bars based on rank
        CategoryItemRenderer renderer = plot.getRenderer();
        for (int i = 0; i < dataset.getRowCount(); i++) {

            float hue = 0.6f - (i * 0.05f);
            renderer.setSeriesPaint(i, Color.getHSBColor(hue, 0.8f, 0.9f));
        }

        // Format labels
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 11));

        // Angle product names
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        renderer.setBaseItemLabelsVisible(true);
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setBaseItemLabelFont(new Font("SansSerif", Font.PLAIN, 9));

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(550, 400));
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setMaximumDrawHeight(2000);
        chartPanel.setMouseWheelEnabled(true);

        barChartPanel.removeAll();
        barChartPanel.setLayout(new BorderLayout());
        barChartPanel.add(chartPanel, BorderLayout.CENTER);
        barChartPanel.revalidate();
        barChartPanel.repaint();

    }

    //  Generate report method    
    public void generateReport(String fromDate, String toDate, boolean byQuantity, String regionName) {

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        String totalProducts = "";
        String totalQuantity = "";
        String totalRevenue = "";
        String topProduct = "";

        try {
            con = db.getConnection();

            // Get summary statistics
            String summaryQuery;
            if (regionName.equals("All Regions")) {
                summaryQuery = "SELECT COUNT(DISTINCT pro_name) as product_count, "
                        + "SUM(qty) as total_quantity, "
                        + "SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y')";
            } else {
                summaryQuery = "SELECT COUNT(DISTINCT pro_name) as product_count, "
                        + "SUM(qty) as total_quantity, "
                        + "SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "AND region = ?";
            }

            pst = con.prepareStatement(summaryQuery);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            if (!regionName.equals("All Regions")) {
                pst.setString(3, regionName);
            }

            rs = pst.executeQuery();

            if (rs.next()) {
                totalProducts = String.valueOf(rs.getInt("product_count"));
                totalQuantity = String.valueOf(rs.getInt("total_quantity"));

                DecimalFormat df = new DecimalFormat("#,##0.00");
                totalRevenue = "Rs. " + df.format(rs.getDouble("total_revenue"));
            }
            rs.close();
            pst.close();

            // Get top product
            String orderBy = byQuantity ? "ORDER BY total_qty DESC" : "ORDER BY total_revenue DESC";
            String topProductQuery;

            if (regionName.equals("All Regions")) {
                topProductQuery = "SELECT pro_name, SUM(qty) as total_qty, SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "GROUP BY pro_name "
                        + orderBy + " LIMIT 1";
            } else {
                topProductQuery = "SELECT pro_name, SUM(qty) as total_qty, SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "AND region = ? "
                        + "GROUP BY pro_name "
                        + orderBy + " LIMIT 1";
            }

            pst = con.prepareStatement(topProductQuery);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            if (!regionName.equals("All Regions")) {
                pst.setString(3, regionName);
            }

            rs = pst.executeQuery();

            if (rs.next()) {
                topProduct = rs.getString("pro_name");
            } else {
                topProduct = "No data";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        generatePDFReport(fromDate, toDate, byQuantity, regionName, topProduct, totalProducts, totalQuantity, totalRevenue);
    }

    private void generatePDFReport(String fromDate, String toDate, boolean byQuantity, String regionName, String topProduct, String totalProducts, String totalQuantity, String totalRevenue) {

        OutputStream os = null;

        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy");
            SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy");
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

            // Parse dates for display
            Date fromDisplayDate = inputFormat.parse(fromDate);
            Date toDisplayDate = inputFormat.parse(toDate);

            // XHTML content for best selling report
            StringBuilder html = new StringBuilder();
            html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            html.append("<head>");
            html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
            html.append("<title>Best-Selling Products Report</title>");
            html.append("<style type=\"text/css\">");
            html.append("@page { size: A4; margin: 1.5cm; }");
            html.append("body { font-family: Arial, sans-serif; margin: 15px; line-height: 1.4; font-size: 11px; }");
            html.append("h1 { color: #2c3e50; text-align: center; margin-bottom: 8px; font-size: 18px; }");
            html.append("h2 { color: #34495e; border-bottom: 1px solid #3498db; padding-bottom: 3px; margin-top: 15px; margin-bottom: 10px; font-size: 14px; }");
            html.append(".report-info { text-align: center; color: #7f8c8d; margin-bottom: 10px; font-size: 10px; }");
            html.append(".filter-info { background: #f8f9fa; padding: 10px; border-radius: 5px; margin: 10px 0; }");

            // Summary cards
            html.append(".summary-container { width: 100%; margin: 10px 0; }");
            html.append(".summary-table { width: 100%; border-collapse: collapse; }");
            html.append(".summary-card { padding: 10px; background: #f8f9fa; border: 1px solid #dee2e6; text-align: center; }");
            html.append(".card-title { font-weight: bold; color: #495057; font-size: 10px; margin-bottom: 5px; }");
            html.append(".card-value { font-size: 13px; color: #28a745; font-weight: bold; }");

            // Table styles
            html.append("table.data-table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 10px; }");
            html.append("th { background-color: #3498db; color: white; padding: 8px; text-align: left; font-weight: bold; }");
            html.append("td { padding: 6px; border: 1px solid #ddd; }");
            html.append(".avoid-break { page-break-inside: avoid; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body class=\"avoid-break\">");

            // Report header
            html.append("<h1>Best-Selling Products Analysis Report</h1>");
            html.append("<div class=\"report-info\">");
            html.append("<p><strong>Report Period:</strong> ").append(displayFormat.format(fromDisplayDate)).append(" to ").append(displayFormat.format(toDisplayDate)).append("</p>");
            html.append("<p><strong>Generated On:</strong> ").append(displayFormat.format(new Date())).append("</p>");
            html.append("</div>");

            // Filter information
            html.append("<div class=\"filter-info\">");
            html.append("<p><strong>Analysis Criteria:</strong></p>");
            html.append("<p>• Metric: ").append(byQuantity ? "Quantity Sold" : "Revenue").append("</p>");
            html.append("<p>• Region: ").append(regionName).append("</p>");
            html.append("</div>");

            // Summary Cards
            html.append("<h2>Performance Summary</h2>");
            html.append("<div class=\"summary-container\">");
            html.append("<table class=\"summary-table\">");
            html.append("<tr>");
            html.append("<td width=\"25%\"><div class=\"summary-card\"><div class=\"card-title\">Top Product</div><div class=\"card-value\">").append(topProduct).append("</div></div></td>");
            html.append("<td width=\"25%\"><div class=\"summary-card\"><div class=\"card-title\">Total Products</div><div class=\"card-value\">").append(totalProducts).append("</div></div></td>");
            html.append("<td width=\"25%\"><div class=\"summary-card\"><div class=\"card-title\">Total Quantity</div><div class=\"card-value\">").append(totalQuantity).append("</div></div></td>");
            html.append("<td width=\"25%\"><div class=\"summary-card\"><div class=\"card-title\">Total Revenue</div><div class=\"card-value\">").append(totalRevenue).append("</div></div></td>");
            html.append("</tr>");
            html.append("</table>");
            html.append("</div>");

            // Table Data - Top Products
            html.append("<h2>Top Selling Products</h2>");
            html.append("<table class=\"data-table\">");
            html.append("<tr><th>Rank</th><th>Product Name</th><th>Quantity Sold</th><th>Total Revenue</th><th>Region</th></tr>");

            DefaultTableModel model = (DefaultTableModel) productTable.getModel();

            for (int row = 0; row < model.getRowCount(); row++) {
                html.append("<tr>");
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    String cellValue = value != null ? value.toString() : "";

                    // Escape special XML characters
                    cellValue = cellValue.replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&apos;");

                    html.append("<td>").append(cellValue).append("</td>");
                }
                html.append("</tr>");
            }

            html.append("</table>");

            html.append("</body>");
            html.append("</html>");

            // Save PDF file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save PDF Report");
            String defaultFileName = "Best_Seller_Report_" + fileNameFormat.format(new Date()) + ".pdf";
            fileChooser.setSelectedFile(new File(defaultFileName));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }

                os = new FileOutputStream(file);
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(html.toString());
                renderer.layout();
                renderer.createPDF(os);
                os.close();

                JOptionPane.showMessageDialog(this, "PDF report generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        barChartPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        productTable = new javax.swing.JTable();
        jLabel10 = new javax.swing.JLabel();
        generateReportBtn = new javax.swing.JButton();
        refreshBtn = new javax.swing.JButton();
        dateFromChooser = new com.toedter.calendar.JDateChooser();
        dateToChooser = new com.toedter.calendar.JDateChooser();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        topProductLabel = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        totalSoldLabel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        totalRevenueLabel = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        totalProductLabel = new javax.swing.JLabel();
        resetBtn = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        revenueRadioBtn = new javax.swing.JRadioButton();
        qtyRadioBtn = new javax.swing.JRadioButton();
        jLabel13 = new javax.swing.JLabel();
        regionComboBox = new javax.swing.JComboBox<>();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel5.setBackground(new java.awt.Color(0, 51, 102));
        jLabel5.setFont(new java.awt.Font("Microsoft JhengHei UI", 1, 22)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(1, 34, 98));
        jLabel5.setText("Sales Analysis");

        barChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
        barChartPanel.setLayout(barChartPanelLayout);
        barChartPanelLayout.setHorizontalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 553, Short.MAX_VALUE)
        );
        barChartPanelLayout.setVerticalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 396, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(barChartPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(294, 294, 294))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(barChartPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Chart View", jPanel3);

        productTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Rank", "Product Name", "Total Quantity", "Total Revenue", "Region"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(productTable);
        if (productTable.getColumnModel().getColumnCount() > 0) {
            productTable.getColumnModel().getColumn(0).setResizable(false);
            productTable.getColumnModel().getColumn(1).setResizable(false);
            productTable.getColumnModel().getColumn(2).setResizable(false);
            productTable.getColumnModel().getColumn(3).setResizable(false);
            productTable.getColumnModel().getColumn(4).setResizable(false);
        }

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel10.setText("Product Performance");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 525, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 23, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 349, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Table View", jPanel2);

        generateReportBtn.setBackground(new java.awt.Color(13, 42, 171));
        generateReportBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        generateReportBtn.setForeground(new java.awt.Color(255, 255, 255));
        generateReportBtn.setText("Generate Report");
        generateReportBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateReportBtnActionPerformed(evt);
            }
        });

        refreshBtn.setBackground(new java.awt.Color(215, 223, 255));
        refreshBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        refreshBtn.setText("Refresh");
        refreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshBtnActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel2.setText("From :");

        jLabel3.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel3.setText("To:");

        jLabel4.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Sales Analysis");

        jLabel8.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Top Product");

        jLabel9.setFont(new java.awt.Font("Microsoft Tai Le", 0, 14)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("(According to selected dates)");

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));

        topProductLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(topProductLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topProductLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel16.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel16.setText("Total Sold");

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        totalSoldLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalSoldLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(totalSoldLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel18.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel18.setText("Total Revenue");

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        totalRevenueLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalRevenueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(totalRevenueLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel19.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel19.setText("Total Products");

        jPanel14.setBackground(new java.awt.Color(255, 255, 255));

        totalProductLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalProductLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(totalProductLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                                .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel14, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9)
                        .addGap(16, 16, 16)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        resetBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        resetBtn.setText("Reset");
        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetBtnActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel12.setText("Metric:");

        jSeparator1.setForeground(new java.awt.Color(0, 0, 102));

        revenueRadioBtn.setBackground(new java.awt.Color(255, 255, 255));
        buttonGroup1.add(revenueRadioBtn);
        revenueRadioBtn.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        revenueRadioBtn.setText("Revenue");
        revenueRadioBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                revenueRadioBtnActionPerformed(evt);
            }
        });

        qtyRadioBtn.setBackground(new java.awt.Color(255, 255, 255));
        buttonGroup1.add(qtyRadioBtn);
        qtyRadioBtn.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        qtyRadioBtn.setText("Quantity Sold");
        qtyRadioBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                qtyRadioBtnActionPerformed(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel13.setText("Region :");

        regionComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        regionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regionComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(refreshBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(generateReportBtn))
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(revenueRadioBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(qtyRadioBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(dateFromChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(dateToChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 848, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 574, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(regionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(268, 268, 268)))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(resetBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(33, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(dateToChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(qtyRadioBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(revenueRadioBtn))
                    .addComponent(dateFromChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(regionComboBox)
                            .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(20, 20, 20)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 445, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(refreshBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(generateReportBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(17, 17, 17))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void refreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshBtnActionPerformed

        dataAnalysis();

    }//GEN-LAST:event_refreshBtnActionPerformed

    private void generateReportBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateReportBtnActionPerformed

        if (!qtyRadioBtn.isSelected() && !revenueRadioBtn.isSelected()) {
            JOptionPane.showMessageDialog(this, "Select a metric to analyze", "Metric Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String regionName = (String) regionComboBox.getSelectedItem();

        if (regionName == null) {
            JOptionPane.showMessageDialog(this, "Select a region", "Region Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();

        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(this, "Please select dates to analyze", "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean byQuantity = qtyRadioBtn.isSelected();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbDateFormat.format(fromSelectedDate);
        String toDate = dbDateFormat.format(toSelectedDate);

        updateTable(fromDate, toDate, byQuantity, regionName);
        generateReport(fromDate, toDate, byQuantity, regionName);

    }//GEN-LAST:event_generateReportBtnActionPerformed

    private void revenueRadioBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_revenueRadioBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_revenueRadioBtnActionPerformed

    private void qtyRadioBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_qtyRadioBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_qtyRadioBtnActionPerformed

    private void regionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_regionComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_regionComboBoxActionPerformed

    private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed

        topProductLabel.setText("");
        totalSoldLabel.setText("");
        totalRevenueLabel.setText("");
        totalProductLabel.setText("");
        dateFromChooser.setDate(null);
        dateToChooser.setDate(null);
        
    }//GEN-LAST:event_resetBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel barChartPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private com.toedter.calendar.JDateChooser dateFromChooser;
    private com.toedter.calendar.JDateChooser dateToChooser;
    private javax.swing.JButton generateReportBtn;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable productTable;
    private javax.swing.JRadioButton qtyRadioBtn;
    private javax.swing.JButton refreshBtn;
    private javax.swing.JComboBox<String> regionComboBox;
    private javax.swing.JButton resetBtn;
    private javax.swing.JRadioButton revenueRadioBtn;
    private javax.swing.JLabel topProductLabel;
    private javax.swing.JLabel totalProductLabel;
    private javax.swing.JLabel totalRevenueLabel;
    private javax.swing.JLabel totalSoldLabel;
    // End of variables declaration//GEN-END:variables
}
