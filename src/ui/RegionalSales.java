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
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 *
 * @author bathi
 */
public class RegionalSales extends javax.swing.JPanel {

    /**
     * Creates new form RegionalSales
     */
    public RegionalSales() {
        initComponents();
        totalSales();
        topRegion();
        totalRegions();
        avgOrders();
        createBarChart();
        createPieChart();
        loadTable();
    }

    //  Top sales main card
    public void totalSales() {

        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT SUM(total_price) AS total FROM transactions;";
            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            if (rs.next()) {
                float total = rs.getFloat("total");
                total_sales_lbl.setText("Rs. " + total);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Average orders main card
    public void avgOrders() {

        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT SUM(total_price) AS total_sales, "
                    + "COUNT(DISTINCT region) AS total_regions, "
                    + "SUM(total_price) / COUNT(DISTINCT region) AS avg_sales_per_region "
                    + "FROM transactions";

            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            if (rs.next()) {
                String avg_orders = rs.getString("avg_sales_per_region");
                average_per_region.setText("Rs. " + avg_orders);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Top region main card
    public void topRegion() {

        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT region, SUM(total_price) AS total_sales\n"
                    + "FROM transactions\n"
                    + "GROUP BY region\n"
                    + "ORDER BY total_sales DESC\n"
                    + "LIMIT 1;";

            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            if (rs.next()) {
                String regionName = rs.getString("region");
                top_region.setText(regionName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Total region main card
    public void totalRegions() {

        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT COUNT(DISTINCT region) AS totalRegions FROM transactions";

            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            if (rs.next()) {
                String totalRegions = rs.getString("totalRegions");
                total_regions.setText(totalRegions);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Data analysis method (When date choosen and refresh button click)
    public void dataAnalysis() {

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();

        // Check if dates are selected
        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(null, "Please choose dates for analysis.");
            return;
        }

        SimpleDateFormat dbDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbDateFormat.format(fromSelectedDate);
        String toDate = dbDateFormat.format(toSelectedDate);

        System.out.println("From Date: " + fromDate);
        System.out.println("To Date: " + toDate);

        try {
            updatePieChart(fromDate, toDate);
            updateBarChart(fromDate, toDate);
            updateSidePanel(fromDate, toDate);
            updateTable(fromDate, toDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  Method to update the side panel according to selected date range    
    public void updateSidePanel(String fromDate, String toDate) {

        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {

            con = db.getConnection();

            String query = "SELECT region, pro_name, SUM(total_price) as total_sales "
                    + "FROM transactions "
                    + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') "
                    + "AND STR_TO_DATE(?, '%m/%d/%Y') "
                    + "GROUP BY region "
                    + "ORDER BY total_sales DESC "
                    + "LIMIT 1";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            if (rs.next()) {
                String topRegion = rs.getString("region");
                String topProduct = rs.getString("pro_name");
                String totalSales = rs.getString("total_sales");

                topRegionDate.setText(topRegion);
                topProductDate.setText(topProduct);
                totalSalesDates.setText("Rs. " + totalSales);
            } else {
                topProductDate.setText("No data");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Load table data method
    private void loadTable() {

        try {
            Connection con = db.getConnection();

            String query
                    = "SELECT region, "
                    + "SUM(total_price) AS total_sales, "
                    + "COUNT(*) AS transactions, "
                    + "AVG(total_price) AS average_sales "
                    + "FROM transactions "
                    + "GROUP BY region";

            PreparedStatement pst = con.prepareStatement(query);

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) regionalTable.getModel();

            while (rs.next()) {

                Object[] row = new Object[4];
                row[0] = rs.getString("region");
                row[1] = rs.getDouble("total_sales");
                row[2] = rs.getInt("transactions");
                row[3] = rs.getDouble("average_sales");

                model.addRow(row);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    // Update table data according to dates selected method
    private void updateTable(String fromDate, String toDate) {

        try {
            Connection con = db.getConnection();

            String query
                    = "SELECT region, "
                    + "SUM(total_price) AS total_sales, "
                    + "COUNT(*) AS transactions, "
                    + "AVG(total_price) AS average_sales "
                    + "FROM transactions "
                    + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') "
                    + "AND STR_TO_DATE(?, '%m/%d/%Y') "
                    + "GROUP BY region";

            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) regionalTable.getModel();
            model.setRowCount(0);

            while (rs.next()) {

                Object[] row = new Object[4];
                row[0] = rs.getString("region");
                row[1] = rs.getDouble("total_sales");
                row[2] = rs.getInt("transactions");
                row[3] = String.format("%,.2f", rs.getDouble("average_sales"));

                model.addRow(row);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    //  Method to get regional sales by date range to update the pie and bar chart
    public Map<String, Double> getRegionalSalesByDateRange(String fromDate, String toDate) {

        Map<String, Double> regionSales = new HashMap<>();

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query = "SELECT region, SUM(total_price) AS total_sales "
                    + "FROM transactions "
                    + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') "
                    + "AND STR_TO_DATE(?, '%m/%d/%Y') "
                    + "GROUP BY region";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            while (rs.next()) {
                String region = rs.getString("region");
                double sales = rs.getDouble("total_sales");
                regionSales.put(region, sales);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return regionSales;
    }

    //  Pie chart update method according to selected dates    
    private void updatePieChart(String fromDate, String toDate) {

        // Fetch data from DB
        Map<String, Double> data = getRegionalSalesByDateRange(fromDate, toDate);

        // Create dataset
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        // Create chart
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Sales by Region",
                dataset,
                true, true, false
        );

        pieChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        pieChart.getLegend().setItemFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));

        // Panel settings
        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(550, 330));

        pieChartPanel.removeAll();
        pieChartPanel.setLayout(new java.awt.BorderLayout());
        pieChartPanel.add(chartPanel, BorderLayout.CENTER);
        pieChartPanel.revalidate();
        pieChartPanel.repaint();
    }

    //  Bar chart update method according to selected dates
    private void updateBarChart(String fromDate, String toDate) {

        Map<String, Double> data = getRegionalSalesByDateRange(fromDate, toDate);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.addValue(entry.getValue(), "Sales", entry.getKey());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Sales by Region",
                "Region",
                "Sales (Rs.)",
                dataset
        );

        barChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));

        barChart.getCategoryPlot().getDomainAxis().setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        barChart.getCategoryPlot().getRangeAxis().setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));

        barChart.getCategoryPlot().getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
        barChart.getCategoryPlot().getRangeAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(550, 330));
        chartPanel.setMaximumDrawWidth(550);
        chartPanel.setMaximumDrawHeight(330);

        barChartPanel.removeAll();
        barChartPanel.setLayout(new java.awt.BorderLayout());
        barChartPanel.add(chartPanel, BorderLayout.CENTER);
        barChartPanel.revalidate();
        barChartPanel.repaint();
    }

    public Map<String, Double> getRegionalSalesData() {
        Map<String, Double> regionSales = new HashMap<>();

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT region, SUM(total_price) as total_sales "
                    + "FROM transactions GROUP BY region";
            pst = con.prepareStatement(query);
            rs = pst.executeQuery();

            while (rs.next()) {
                String region = rs.getString("region");
                double sales = rs.getDouble("total_sales");
                regionSales.put(region, sales);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return regionSales;
    }

    private void createPieChart() {
        Map<String, Double> data = getRegionalSalesData();

        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                "Sales by Region",
                dataset,
                true, true, false
        );

        pieChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));

        pieChart.getLegend().setItemFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));

        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(550, 330));
        chartPanel.setMaximumDrawWidth(550);
        chartPanel.setMaximumDrawHeight(330);

        pieChartPanel.removeAll();
        pieChartPanel.setLayout(new java.awt.BorderLayout());
        pieChartPanel.add(chartPanel, BorderLayout.CENTER);
        pieChartPanel.revalidate();
        pieChartPanel.repaint();
    }

    private void createBarChart() {
        Map<String, Double> data = getRegionalSalesData();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            dataset.addValue(entry.getValue(), "Sales", entry.getKey());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                "Sales by Region",
                "Region",
                "Sales (Rs.)",
                dataset
        );

        barChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));

        barChart.getCategoryPlot().getDomainAxis().setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        barChart.getCategoryPlot().getRangeAxis().setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));

        barChart.getCategoryPlot().getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
        barChart.getCategoryPlot().getRangeAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(550, 330));
        chartPanel.setMaximumDrawWidth(550);
        chartPanel.setMaximumDrawHeight(330);

        barChartPanel.removeAll();
        barChartPanel.setLayout(new java.awt.BorderLayout());
        barChartPanel.add(chartPanel, BorderLayout.CENTER);
        barChartPanel.revalidate();
        barChartPanel.repaint();
    }

    public void generateReport(String fromDate, String toDate) {
        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        // Initialize variables with defaults
        String topRegion = "";
        String topProduct = "";
        String totalSales = "";

        try {
            con = db.getConnection();
            String query = "SELECT region, pro_name, SUM(total_price) as total_sales "
                    + "FROM transactions "
                    + "WHERE STR_TO_DATE(date, '%m/%d/%Y') BETWEEN STR_TO_DATE(?, '%m/%d/%Y') "
                    + "AND STR_TO_DATE(?, '%m/%d/%Y') "
                    + "GROUP BY region "
                    + "ORDER BY total_sales DESC "
                    + "LIMIT 1";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            if (rs.next()) {
                topRegion = rs.getString("region");
                topProduct = rs.getString("pro_name");
                double sales = rs.getDouble("total_sales");
                totalSales = "Rs. " + String.format("%,.2f", sales);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        generatePDFReport(topRegion, topProduct, totalSales);
    }

    private void generatePDFReport(String topRegion, String topProduct, String totalSales) {
        OutputStream os = null;

        try {
            Date fromSelectedDate = dateFromChooser.getDate();
            Date toSelectedDate = dateToChooser.getDate();
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy");
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

            // XHTML content for PDF - OPTIMIZED FOR SINGLE PAGE
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
            html.append("<h1>Regional Sales Report</h1>");
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
            html.append("<h2>Top Region Summary</h2>");
            html.append("<div class=\"summary-container\">");
            html.append("<table class=\"summary-table\">");
            html.append("<tr>");
            html.append("<td width=\"35%\"><div class=\"summary-card\"><div class=\"card-title\">Top Region</div><div class=\"card-value\">").append(topRegion).append("</div></div></td>");
            html.append("<td width=\"35%\"><div class=\"summary-card\"><div class=\"card-title\">Total Sales</div><div class=\"card-value\">").append(totalSales).append("</div></div></td>");
            html.append("<td width=\"35%\"><div class=\"summary-card\"><div class=\"card-title\">Top Product</div><div class=\"card-value\">").append(topProduct).append("</div></div></td>");
            html.append("</tr>");
            html.append("</table>");
            html.append("</div>");

            // Table Data
            html.append("<h2>Regional Sales Data</h2>");
            html.append("<table class=\"data-table\">");
            html.append("<tr><th>Region</th><th>Total Sales (Rs.)</th><th>Transactions</th><th>Average Sales (Rs.)</th></tr>");

            DefaultTableModel model = (DefaultTableModel) regionalTable.getModel();

            for (int row = 0; row < model.getRowCount(); row++) {
                html.append("<tr>");
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    String cellValue = value != null ? value.toString() : "";

                    // Format numbers
                    if (col > 0 && value instanceof Number) {
                        if (col == 1 || col == 3) {
                            cellValue = String.format("%,.2f", ((Number) value).doubleValue());
                        } else if (col == 2) {
                            cellValue = String.format("%,d", ((Number) value).intValue());
                        }
                    }

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
            String defaultFileName = "Regional_Sales_Report_" + fileNameFormat.format(new Date()) + ".pdf";
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
        jPanel6 = new javax.swing.JPanel();
        total_sales_lbl = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        top_region = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        average_per_region = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        total_regions = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        barChartPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        pieChartPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        regionalTable = new javax.swing.JTable();
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
        topRegionDate = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        topProductDate = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        totalSalesDates = new javax.swing.JLabel();
        resetBtn = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel5.setBackground(new java.awt.Color(0, 51, 102));
        jLabel5.setFont(new java.awt.Font("Microsoft JhengHei UI", 1, 22)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(1, 34, 98));
        jLabel5.setText("Regional Sales Analysis");

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 102), 1, true));

        total_sales_lbl.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 20)); // NOI18N
        total_sales_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        total_sales_lbl.setText("Rs. 140,000.00");

        jLabel1.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Total Sales");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(total_sales_lbl, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(jLabel1)
                .addGap(3, 3, 3)
                .addComponent(total_sales_lbl, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel15.setBackground(new java.awt.Color(255, 255, 255));
        jPanel15.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 102), 1, true));

        top_region.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 20)); // NOI18N
        top_region.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        top_region.setText("Central");

        jLabel6.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Top Region");

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(top_region, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(jLabel6)
                .addGap(3, 3, 3)
                .addComponent(top_region, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel16.setBackground(new java.awt.Color(255, 255, 255));
        jPanel16.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 102), 1, true));

        average_per_region.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 20)); // NOI18N
        average_per_region.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        average_per_region.setText("Rs. 140,000.00");

        jLabel7.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Average Per Region");

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(average_per_region, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(jLabel7)
                .addGap(3, 3, 3)
                .addComponent(average_per_region, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel20.setBackground(new java.awt.Color(255, 255, 255));
        jPanel20.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 102), 1, true));

        total_regions.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 20)); // NOI18N
        total_regions.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        total_regions.setText("5");

        jLabel11.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Regions");

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(total_regions, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(jLabel11)
                .addGap(3, 3, 3)
                .addComponent(total_regions, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        barChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
        barChartPanel.setLayout(barChartPanelLayout);
        barChartPanelLayout.setHorizontalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 550, Short.MAX_VALUE)
        );
        barChartPanelLayout.setVerticalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 383, Short.MAX_VALUE)
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

        pieChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout pieChartPanelLayout = new javax.swing.GroupLayout(pieChartPanel);
        pieChartPanel.setLayout(pieChartPanelLayout);
        pieChartPanelLayout.setHorizontalGroup(
            pieChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 550, Short.MAX_VALUE)
        );
        pieChartPanelLayout.setVerticalGroup(
            pieChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 376, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pieChartPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pieChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Chart View 2", jPanel4);

        regionalTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Region", "Total Sales", "Transactions", "Average Sales"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(regionalTable);
        if (regionalTable.getColumnModel().getColumnCount() > 0) {
            regionalTable.getColumnModel().getColumn(0).setResizable(false);
            regionalTable.getColumnModel().getColumn(1).setResizable(false);
            regionalTable.getColumnModel().getColumn(2).setResizable(false);
            regionalTable.getColumnModel().getColumn(3).setResizable(false);
        }

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel10.setText("Regional Sales Data");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 529, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(238, Short.MAX_VALUE))
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
        jLabel4.setText("Top Region Analysis");

        jLabel8.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Top Region");

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

        topRegionDate.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(topRegionDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topRegionDate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel16.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel16.setText("Top Product in Region");

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        topProductDate.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(topProductDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topProductDate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        jLabel18.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel18.setText("Total Sales in Region");

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        totalSalesDates.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalSalesDates, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(totalSalesDates, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
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
                        .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
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
                .addContainerGap(43, Short.MAX_VALUE))
        );

        resetBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        resetBtn.setText("Reset");
        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(dateFromChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(dateToChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(449, 449, 449))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 567, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(resetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(refreshBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(generateReportBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(24, 24, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(refreshBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(generateReportBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(dateFromChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(dateToChooser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(resetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    //  Generate report button    
    private void generateReportBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateReportBtnActionPerformed

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();

        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(this,"Select dates to analyze", "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat dbFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbFormat.format(fromSelectedDate);
        String toDate = dbFormat.format(toSelectedDate);

        // Generate the report
        generateReport(fromDate, toDate);

    }//GEN-LAST:event_generateReportBtnActionPerformed

    private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed

        topRegionDate.setText("");
        topProductDate.setText("");
        totalSalesDates.setText("");
        createBarChart();
        createPieChart();
        dateFromChooser.setDate(null);
        dateToChooser.setDate(null);

    }//GEN-LAST:event_resetBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel average_per_region;
    private javax.swing.JPanel barChartPanel;
    private com.toedter.calendar.JDateChooser dateFromChooser;
    private com.toedter.calendar.JDateChooser dateToChooser;
    private javax.swing.JButton generateReportBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel pieChartPanel;
    private javax.swing.JButton refreshBtn;
    private javax.swing.JTable regionalTable;
    private javax.swing.JButton resetBtn;
    private javax.swing.JLabel topProductDate;
    private javax.swing.JLabel topRegionDate;
    private javax.swing.JLabel top_region;
    private javax.swing.JLabel totalSalesDates;
    private javax.swing.JLabel total_regions;
    private javax.swing.JLabel total_sales_lbl;
    // End of variables declaration//GEN-END:variables
}
