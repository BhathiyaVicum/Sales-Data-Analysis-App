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

/**
 *
 * @author bathi
 */
public class ProductPerformance extends javax.swing.JPanel {

    /**
     * Creates new form RegionalSales
     */
    public ProductPerformance() {
        initComponents();
        loadProductsToComboBox(productComboBox);
    }

    //  Data analysis method
    public void dataAnalysis() {

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();
        String pro_name = (String) productComboBox.getSelectedItem();

        // Check if dates are selected
        if (fromSelectedDate == null || toSelectedDate == null || pro_name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select dates and product name to analyze", "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat dbDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbDateFormat.format(fromSelectedDate);
        String toDate = dbDateFormat.format(toSelectedDate);

        try {
            updateSidePanel(fromDate, toDate, pro_name);
            updateLineChart(pro_name, fromDate, toDate);
            updateTable(fromDate, toDate, pro_name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Load products to combo box    
    public void loadProductsToComboBox(JComboBox<String> productComboBox) {

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            // Get product names
            String query = "SELECT DISTINCT pro_name FROM transactions ORDER BY pro_name";
            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            productComboBox.removeAllItems();

            productComboBox.addItem("-- Select Product --");

            // Add all products
            while (rs.next()) {
                String productName = rs.getString("pro_name");
                productComboBox.addItem(productName);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading products: " + e.getMessage());
        }

    }

    //  Method to update the side panel according to selected date range    
    public void updateSidePanel(String fromDate, String toDate, String pro_name) {

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query = "SELECT "
                    + "SUM(qty) as total_sold, "
                    + "SUM(total_price) as total_revenue, "
                    + "(SELECT region FROM transactions "
                    + " WHERE pro_name LIKE '" + pro_name + "%' "
                    + " AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                    + " GROUP BY region "
                    + " ORDER BY SUM(qty) DESC "
                    + " LIMIT 1) as top_region "
                    + "FROM transactions "
                    + "WHERE pro_name LIKE '" + pro_name + "%' "
                    + "AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y')";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);
            pst.setString(3, fromDate);
            pst.setString(4, toDate);

            rs = pst.executeQuery();

            if (rs.next()) {

                int totalSold = rs.getInt("total_sold");
                totalSoldLabel.setText(String.valueOf(totalSold));

                double totalRevenue = rs.getDouble("total_revenue");
                DecimalFormat df = new DecimalFormat("#,##0.00");
                totalRevenueLabel.setText("Rs. " + df.format(totalRevenue));

                String topRegion = rs.getString("top_region");
                if (topRegion != null) {
                    topRegionLabel.setText(topRegion);
                } else {
                    topRegionLabel.setText("No regional data");
                }
            } else {
                totalSoldLabel.setText("0");
                totalRevenueLabel.setText("Rs. 0.00");
                topRegionLabel.setText("No data");
            }

            //System.out.println("Side Panel - Product: " + pro_name + ", From: " + fromDate + ", To: " + toDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Update table data according to dates selected method
    private void updateTable(String fromDate, String toDate, String pro_name) {
        
        try {
            Connection con = db.getConnection();

            String query = "SELECT date, qty, per_unit, region, total_price "
                    + "FROM transactions "
                    + "WHERE pro_name LIKE ? "
                    + "AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y')";

            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, pro_name + "%");
            pst.setString(2, fromDate);
            pst.setString(3, toDate);

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) productTable.getModel();
            model.setRowCount(0);

            DecimalFormat df = new DecimalFormat("#,##0.00");

            while (rs.next()) {
                Object[] row = new Object[5];
                row[0] = rs.getString("date");
                row[1] = rs.getInt("qty");
                row[2] = df.format(rs.getDouble("per_unit"));
                row[3] = rs.getString("region");
                row[4] = df.format(rs.getDouble("total_price"));

                model.addRow(row);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Double> getProductSalesOverTime(String productName, String fromDate, String toDate, boolean groupByMonth) {
        Map<String, Double> salesData = new HashMap<>();

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query;
            if (groupByMonth) {
                // Group by monthly (for > 12 months)
                query = "SELECT "
                        + "DATE_FORMAT(STR_TO_DATE(date, '%m/%d/%Y'), '%Y-%m') as month, "
                        + "SUM(qty) as total_qty, "
                        + "SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE pro_name LIKE ? "
                        + "AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "GROUP BY DATE_FORMAT(STR_TO_DATE(date, '%m/%d/%Y'), '%Y-%m') "
                        + "ORDER BY month";
            } else {
                // Group weekly (for < 12 months)
                query = "SELECT "
                        + "CONCAT(YEAR(STR_TO_DATE(date, '%m/%d/%Y')), '-W', "
                        + "LPAD(WEEK(STR_TO_DATE(date, '%m/%d/%Y')), 2, '0')) as week, "
                        + "SUM(qty) as total_qty, "
                        + "SUM(total_price) as total_revenue "
                        + "FROM transactions "
                        + "WHERE pro_name LIKE ? "
                        + "AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                        + "GROUP BY YEAR(STR_TO_DATE(date, '%m/%d/%Y')), WEEK(STR_TO_DATE(date, '%m/%d/%Y')) "
                        + "ORDER BY week";
            }

            pst = con.prepareStatement(query);
            pst.setString(1, productName + "%");
            pst.setString(2, fromDate);
            pst.setString(3, toDate);

            rs = pst.executeQuery();

            while (rs.next()) {
                String timePeriod;
                if (groupByMonth) {
                    timePeriod = rs.getString("month");
                } else {
                    timePeriod = rs.getString("week");
                }
                double quantity = rs.getDouble("total_qty");
                salesData.put(timePeriod, quantity);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return salesData;
    }

    // Line charttt for product performance over time
    private void updateLineChart(String productName, String fromDate, String toDate) {

        try {
            // Calculate if period is more than 12 months
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            Date startDate = sdf.parse(fromDate);
            Date endDate = sdf.parse(toDate);

            long diffInMillies = Math.abs(endDate.getTime() - startDate.getTime());
            long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
            long diffInMonths = diffInDays / 30;

            boolean groupByMonth = diffInMonths > 12;

            // Fetch data with grouping
            Map<String, Double> data = getProductSalesOverTime(productName, fromDate, toDate, groupByMonth);

            // Create dataset for line chart
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Sort keys
            List<String> sortedPeriods = new ArrayList<>(data.keySet());
            Collections.sort(sortedPeriods);

            // Add sorted data to dataset
            for (String period : sortedPeriods) {
                Double value = data.get(period);
                String displayLabel = formatTimePeriod(period, groupByMonth);
                dataset.addValue(value, "Quantity Sold", displayLabel);
            }

            // Create line chart with title
            String chartTitle;
            if (groupByMonth) {
                chartTitle = productName + " - Monthly Sales";
            } else {
                chartTitle = productName + " - Weekly Sales";
            }

            JFreeChart lineChart = ChartFactory.createLineChart(
                    chartTitle,
                    groupByMonth ? "Month" : "Week",
                    "Quantity Sold",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );

            // Customize chart appearance
            lineChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
            CategoryPlot plot = lineChart.getCategoryPlot();

            CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

            // Adjust label density based on data points
            if (data.size() > 20) {
                domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 8));
                domainAxis.setMaximumCategoryLabelLines(3);
            } else {
                domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
            }

            // Adjust margins
            domainAxis.setLowerMargin(0.02);
            domainAxis.setUpperMargin(0.02);

            // Range axis
            plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 11));

            CategoryItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesPaint(0, Color.BLUE);
            ((LineAndShapeRenderer) renderer).setSeriesStroke(0, new BasicStroke(2.0f));

            // Add data points
            ((LineAndShapeRenderer) renderer).setSeriesShapesVisible(0, true);
            ((LineAndShapeRenderer) renderer).setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));

            // Create chart panel
            ChartPanel chartPanel = new ChartPanel(lineChart);
            chartPanel.setPreferredSize(new Dimension(560, 350));
            chartPanel.setMaximumDrawWidth(2000);
            chartPanel.setMaximumDrawHeight(2000);
            chartPanel.setMouseWheelEnabled(true);

            // Update the panel
            barChartPanel.removeAll();
            barChartPanel.setLayout(new BorderLayout());
            barChartPanel.add(chartPanel, BorderLayout.CENTER);
            barChartPanel.revalidate();
            barChartPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error creating chart: " + e.getMessage());
        }
    }

    // New helper method for date formatting
    private String formatTimePeriod(String period, boolean isMonthly) {
        try {
            if (isMonthly) {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM");
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM yyyy");
                Date date = inputFormat.parse(period);
                return outputFormat.format(date);
            } else {
                if (period.contains("-W")) {
                    String[] parts = period.split("-W");
                    if (parts.length == 2) {
                        return "Week " + parts[1] + ", " + parts[0];
                    }
                }
                return period;
            }
        } catch (Exception e) {
            return period;
        }
    }

    public void generateReport(String fromDate, String toDate, String pro_name) {

        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        String totalSales = "";
        String totalRevenue = "";
        String topRegion = "";

        try {
            con = db.getConnection();

            String query = "SELECT "
                    + "SUM(qty) as total_sold, "
                    + "SUM(total_price) as total_revenue, "
                    + "(SELECT region FROM transactions "
                    + " WHERE pro_name LIKE ? "
                    + " AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y') "
                    + " GROUP BY region "
                    + " ORDER BY SUM(qty) DESC "
                    + " LIMIT 1) as top_region "
                    + "FROM transactions "
                    + "WHERE pro_name LIKE ? "
                    + "AND STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') AND STR_TO_DATE(?, '%m/%d/%Y')";

            pst = con.prepareStatement(query);
            pst.setString(1, pro_name + "%");
            pst.setString(2, fromDate);
            pst.setString(3, toDate);
            pst.setString(4, pro_name + "%");
            pst.setString(5, fromDate);
            pst.setString(6, toDate);

            rs = pst.executeQuery();

            if (rs.next()) {
                int qty = rs.getInt("total_sold");
                totalSales = String.valueOf(qty);

                double revenue = rs.getDouble("total_revenue");
                DecimalFormat df = new DecimalFormat("#,##0.00");
                totalRevenue = "Rs. " + df.format(revenue);

                topRegion = rs.getString("top_region");
                if (topRegion == null) {
                    topRegion = "No data";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        generatePDFReport(topRegion, totalRevenue, totalSales, pro_name);
    }

    private void generatePDFReport(String topRegion, String totalRevenue, String totalSales, String pro_name) {

        OutputStream os = null;

        try {

            Date fromSelectedDate = dateFromChooser.getDate();
            Date toSelectedDate = dateToChooser.getDate();
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy");
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

            // XHTML content
            StringBuilder html = new StringBuilder();
            html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            html.append("<head>");
            html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
            html.append("<title>Regional Sales Report</title>");
            html.append("<style type=\"text/css\">");
            html.append("@page { size: A4; margin: 1.5cm; }");
            html.append("body { font-family: Arial, sans-serif; margin: 15px; line-height: 1.4; font-size: 11px; }");
            html.append("h1 { color: #2c3e50; text-align: center; margin-bottom: 8px; font-size: 18px; }");
            html.append("h2 { color: #34495e; border-bottom: 1px solid #3498db; padding-bottom: 3px; margin-top: 15px; margin-bottom: 10px; font-size: 14px; }");
            html.append(".report-info { text-align: center; color: #7f8c8d; margin-bottom: 10px; font-size: 10px; }");

            // Summary rows
            html.append(".summary-container { width: 100%; margin: 10px 0; }");
            html.append(".summary-table { width: 100%; border-collapse: collapse; }");
            html.append(".summary-card { padding: 10px; background: #f8f9fa; border: 1px solid #dee2e6; bor text-align: center; }");
            html.append(".card-title { font-weight: bold; color: #495057; font-size: 10px; margin-bottom: 5px; }");
            html.append(".card-value { font-size: 13px; color: #28a745; font-weight: bold; }");

            html.append("table.data-table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 10px; }");
            html.append("th { background-color: #3498db; color: white; padding: 8px; text-align: left; font-weight: bold; }");
            html.append("td { padding: 6px; border: 1px solid #ddd; }");
            html.append(".avoid-break { page-break-inside: avoid; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body class=\"avoid-break\">");

            // Report header
            html.append("<h1>Product Performance Report</h1>");
            html.append("<div class=\"report-info\">");

            if (fromSelectedDate != null && toSelectedDate != null) {
                html.append("<p><strong>Report Period:</strong> " + displayFormat.format(fromSelectedDate)
                        + " to " + displayFormat.format(toSelectedDate) + "</p>");
            } else {
                html.append("<p><strong>Report Period:</strong> All Time</p>");
            }

            html.append("<p><strong>Generated On:</strong> " + displayFormat.format(new Date()) + "</p>");
            html.append("</div>");

            // Summary Cards - Using single row table
            html.append("<h2>Product - ").append(pro_name).append("</h2>");
            html.append("<div class=\"summary-container\">");
            html.append("<table class=\"summary-table\">");
            html.append("<tr>");
            html.append("<td width=\"35%\"><div class=\"summary-card\"><div class=\"card-title\">Total Sales</div><div class=\"card-value\">").append(totalSales).append("</div></div></td>");
            html.append("<td width=\"35%\"><div class=\"summary-card\"><div class=\"card-title\">Total Revenue</div><div class=\"card-value\">").append(totalRevenue).append("</div></div></td>");
            html.append("<td width=\"35%\"><div class=\"summary-card\"><div class=\"card-title\">Top Region of Product</div><div class=\"card-value\">").append(topRegion).append("</div></div></td>");
            html.append("</tr>");
            html.append("</table>");
            html.append("</div>");

            // Table Data
            html.append("<h2>Regional Sales Data</h2>");
            html.append("<table class=\"data-table\">");
            html.append("<tr><th>Date</th><th>Quantity</th><th>Per Unit</th><th>Region</th><th>Total Sales (Rs.)</th></tr>");

            DefaultTableModel model = (DefaultTableModel) productTable.getModel();

            for (int row = 0; row < model.getRowCount(); row++) {
                html.append("<tr>");
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    String cellValue = value != null ? value.toString() : "";

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
            String defaultFileName = "Product_Performance_Report_" + fileNameFormat.format(new Date()) + ".pdf";
            fileChooser.setSelectedFile(new File(defaultFileName));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Ensure .pdf extension
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }

                // Create PDF from HTML
                os = new FileOutputStream(file);
                ITextRenderer renderer = new ITextRenderer();

                // Set document with HTML content
                renderer.setDocumentFromString(html.toString());
                renderer.layout();
                renderer.createPDF(os);

                os.close();

                JOptionPane.showMessageDialog(this, "PDF report generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                // Open the PDF after generating
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

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
        totalSoldLabel = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        totalRevenueLabel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        topRegionLabel = new javax.swing.JLabel();
        resetBtn = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        productComboBox = new javax.swing.JComboBox<>();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel5.setBackground(new java.awt.Color(0, 51, 102));
        jLabel5.setFont(new java.awt.Font("Microsoft JhengHei UI", 1, 22)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(1, 34, 98));
        jLabel5.setText("Product Performance Analysis");

        barChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
        barChartPanel.setLayout(barChartPanelLayout);
        barChartPanelLayout.setHorizontalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 550, Short.MAX_VALUE)
        );
        barChartPanelLayout.setVerticalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 423, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(barChartPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(297, 297, 297))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(barChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Chart View 1", jPanel3);

        productTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Date", "Quantity", "Per Unit", "Region", "Total Price"
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 372, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
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
        jLabel4.setText("Product Analysis");

        jLabel8.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Total Sales");

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

        totalSoldLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalSoldLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(totalSoldLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel16.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel16.setText("Total Revenue ");

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        totalRevenueLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalRevenueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(totalRevenueLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel18.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel18.setText("Top Region for This Product");

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        topRegionLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(topRegionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topRegionLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(113, 113, 113)
                        .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)
                        .addComponent(jLabel9)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(53, Short.MAX_VALUE))
        );

        resetBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        resetBtn.setText("Reset");
        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetBtnActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel12.setText("Product Name");

        jSeparator1.setForeground(new java.awt.Color(0, 0, 102));

        productComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        productComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                productComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(productComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGap(37, 37, 37)
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(dateFromChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(dateToChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 848, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 574, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(21, 21, 21)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(refreshBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(generateReportBtn))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(resetBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap(24, Short.MAX_VALUE))
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
                    .addComponent(dateFromChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(dateToChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(productComboBox))
                .addGap(36, 36, 36)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(refreshBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(generateReportBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9)
                        .addComponent(resetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 472, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();
        String pro_name = (String) productComboBox.getSelectedItem();

        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(this, "Select dates to analyze", "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat dbFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbFormat.format(fromSelectedDate);
        String toDate = dbFormat.format(toSelectedDate);

        // Generate the report
        updateTable(fromDate, toDate, pro_name);
        generateReport(fromDate, toDate, pro_name);

    }//GEN-LAST:event_generateReportBtnActionPerformed

    private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed

        totalSoldLabel.setText("");
        totalRevenueLabel.setText("");
        topRegionLabel.setText("");
        dateFromChooser.setDate(null);
        dateToChooser.setDate(null);
        
    }//GEN-LAST:event_resetBtnActionPerformed

    private void productComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_productComboBoxActionPerformed

    }//GEN-LAST:event_productComboBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel barChartPanel;
    private com.toedter.calendar.JDateChooser dateFromChooser;
    private com.toedter.calendar.JDateChooser dateToChooser;
    private javax.swing.JButton generateReportBtn;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox<String> productComboBox;
    private javax.swing.JTable productTable;
    private javax.swing.JButton refreshBtn;
    private javax.swing.JButton resetBtn;
    private javax.swing.JLabel topRegionLabel;
    private javax.swing.JLabel totalRevenueLabel;
    private javax.swing.JLabel totalSoldLabel;
    // End of variables declaration//GEN-END:variables
}
