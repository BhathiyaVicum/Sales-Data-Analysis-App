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
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 *
 * @author bathi
 */
public class CustomerBehavior extends javax.swing.JPanel {

    /**
     * Creates new form RegionalSales
     */
    public CustomerBehavior() {
        initComponents();
        loadComboBox();
        setDefaultDates();
    }

    private void setDefaultDates() {
        Calendar cal = Calendar.getInstance();
        dateToChooser.setDate(cal.getTime());
        cal.add(Calendar.MONTH, -6);
        dateFromChooser.setDate(cal.getTime());
    }

    public void loadComboBox() {
        analysisComboBox.removeAllItems();
        analysisComboBox.addItem("-- Select Analysis Type --");
        analysisComboBox.addItem("Day of Week Patterns");
        analysisComboBox.addItem("Monthly Sales Trends");
        analysisComboBox.addItem("Popular Products");
        analysisComboBox.addItem("Customer Purchase Frequency");
    }

    //  Data analysis method
    public void dataAnalysis() {
        String analysisType = (String) analysisComboBox.getSelectedItem();

        if (analysisType == null || analysisType.equals("-- Select Analysis Type --")) {
            JOptionPane.showMessageDialog(this, "Please select an analysis type",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();

        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(this, "Please select both From and To dates",
                    "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat dbFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbFormat.format(fromSelectedDate);
        String toDate = dbFormat.format(toSelectedDate);

        // Convert to database format
        String dbFromDate = convertToDbFormat(fromDate);
        String dbToDate = convertToDbFormat(toDate);

        try {
            // Clear previous data
            clearChartsAndTable();

            switch (analysisType) {
                case "Day of Week Patterns":
                    //titleLabel.setText("Day of Week Analysis");
                    analyzeDayOfWeekPatterns(dbFromDate, dbToDate);
                    break;

                case "Monthly Sales Trends":
                    //titleLabel.setText("Monthly Sales Trends");
                    analyzeMonthlyTrends(dbFromDate, dbToDate);
                    break;

                case "Popular Products":
                    //titleLabel.setText("Popular Products");
                    analyzePopularProducts(dbFromDate, dbToDate);
                    break;

                case "Customer Purchase Frequency":
                    //titleLabel.setText("Customer Purchase Frequency");
                    analyzeCustomerPurchaseFrequency(dbFromDate, dbToDate);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error during analysis: " + e.getMessage(),
                    "Analysis Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Convert from MM/dd/yyyy to M/d/yyyy
    private String convertToDbFormat(String mmddyyyy) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("M/d/yyyy");
            Date date = inputFormat.parse(mmddyyyy);
            return outputFormat.format(date);
        } catch (Exception e) {
            return mmddyyyy;
        }
    }

    private void clearChartsAndTable() {
        // Clear table
        DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
        model.setRowCount(0);

        // Clear both chart panels
        barChartPanel.removeAll();
        pieChartPanel.removeAll();
        barChartPanel.revalidate();
        barChartPanel.repaint();
        pieChartPanel.revalidate();
        pieChartPanel.repaint();

        // Clear labels
        label1.setText("");
        label2.setText("");
        label3.setText("");
        label4.setText("");
    }

    private void analyzeDayOfWeekPatterns(String fromDate, String toDate) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query = "SELECT "
                    + "DAYNAME(STR_TO_DATE(date, '%m/%d/%Y')) as day_name, "
                    + "COUNT(DISTINCT tra_id) as transaction_count, "
                    + "SUM(total_price) as total_revenue, "
                    + "AVG(total_price) as avg_transaction, "
                    + "COUNT(DISTINCT cus_id) as customer_count, "
                    + "COUNT(*) as total_items "
                    + "FROM transactions "
                    + "WHERE date >= ? AND date <= ? "
                    + "GROUP BY DAYNAME(STR_TO_DATE(date, '%m/%d/%Y')) "
                    + "ORDER BY FIELD(day_name, 'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday')";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            // Update data table
            DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
            model.setRowCount(0);
            model.setColumnIdentifiers(new String[]{"Day", "Transactions", "Customers", "Items", "Avg Sale", "Revenue"});

            DecimalFormat df = new DecimalFormat("#,##0.00");
            int totalTransactions = 0;
            double totalRevenue = 0;
            String peakDay = "";
            double peakRevenue = 0;

            // For datasets
            DefaultCategoryDataset revenueDataset = new DefaultCategoryDataset();
            DefaultPieDataset pieDataset = new DefaultPieDataset();

            while (rs.next()) {
                String day = rs.getString("day_name");
                int transactions = rs.getInt("transaction_count");
                double revenue = rs.getDouble("total_revenue");
                double avgSale = rs.getDouble("avg_transaction");
                int customers = rs.getInt("customer_count");
                int items = rs.getInt("total_items");

                // Add to table
                Object[] row = new Object[6];
                row[0] = day;
                row[1] = transactions;
                row[2] = customers;
                row[3] = items;
                row[4] = "Rs. " + df.format(avgSale);
                row[5] = "Rs. " + df.format(revenue);
                model.addRow(row);

                // Add to datasets for charts
                revenueDataset.addValue(revenue, "Revenue", day);
                pieDataset.setValue(day, revenue);

                // Calculate totals
                totalTransactions += transactions;
                totalRevenue += revenue;

                if (revenue > peakRevenue) {
                    peakRevenue = revenue;
                    peakDay = day;
                }
            }

            // Create charts
            createBarChart(revenueDataset, "Revenue by Day of Week", "Day", "Revenue (Rs.)");
            createPieChart(pieDataset, "Revenue Distribution by Day");

            // Set headings
            heading1.setText("Peak Day");
            heading2.setText("Peak Revenue");
            heading3.setText("Total Transactions");
            heading4.setText("Total Revenue");

            // Set labels with insights
            if (label1 != null) {
                label1.setText(peakDay);
            }
            if (label2 != null) {
                label2.setText("Rs. " + df.format(peakRevenue));
            }
            if (label3 != null) {
                label3.setText(String.valueOf(totalTransactions));
            }
            if (label4 != null) {
                label4.setText("Rs. " + df.format(totalRevenue));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //MONTHLY SALES TRENDS
    private void analyzeMonthlyTrends(String fromDate, String toDate) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query = "SELECT "
                    + "DATE_FORMAT(STR_TO_DATE(date, '%m/%d/%Y'), '%Y-%m') as month_key, "
                    + "DATE_FORMAT(STR_TO_DATE(date, '%m/%d/%Y'), '%M %Y') as month_name, "
                    + "COUNT(DISTINCT tra_id) as transaction_count, "
                    + "SUM(total_price) as total_revenue, "
                    + "COUNT(DISTINCT cus_id) as customer_count, "
                    + "AVG(total_price) as avg_transaction, "
                    + "COUNT(*) as total_items "
                    + "FROM transactions "
                    + "WHERE date >= ? AND date <= ? "
                    + "GROUP BY DATE_FORMAT(STR_TO_DATE(date, '%m/%d/%Y'), '%Y-%m'), "
                    + "DATE_FORMAT(STR_TO_DATE(date, '%m/%d/%Y'), '%M %Y') "
                    + "ORDER BY month_key";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            // Update table
            DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
            model.setRowCount(0);
            model.setColumnIdentifiers(new String[]{"Month", "Transactions", "Customers", "Items", "Avg Sale", "Revenue"});

            DecimalFormat df = new DecimalFormat("#,##0.00");
            int totalTransactions = 0;
            double totalRevenue = 0;
            String bestMonth = "";
            double maxRevenue = 0;
            int monthCount = 0;

            // For datasets
            DefaultCategoryDataset revenueDataset = new DefaultCategoryDataset();
            DefaultPieDataset pieDataset = new DefaultPieDataset();

            while (rs.next()) {
                monthCount++;
                String monthName = rs.getString("month_name");
                int transactions = rs.getInt("transaction_count");
                double revenue = rs.getDouble("total_revenue");
                int customers = rs.getInt("customer_count");
                double avgSale = rs.getDouble("avg_transaction");
                int items = rs.getInt("total_items");

                // Add to table
                Object[] row = new Object[6];
                row[0] = monthName;
                row[1] = transactions;
                row[2] = customers;
                row[3] = items;
                row[4] = "Rs. " + df.format(avgSale);
                row[5] = "Rs. " + df.format(revenue);
                model.addRow(row);

                // Add to datasets for charts
                revenueDataset.addValue(revenue, "Revenue", monthName);
                pieDataset.setValue(monthName, revenue);

                // Calculate totals
                totalTransactions += transactions;
                totalRevenue += revenue;

                if (revenue > maxRevenue) {
                    maxRevenue = revenue;
                    bestMonth = monthName;
                }
            }

            // Create charts
            createBarChart(revenueDataset, "Monthly Revenue", "Month", "Revenue (Rs.)");
            createPieChart(pieDataset, "Revenue Distribution by Month");

            // Set headings
            heading1.setText("Best Month");
            heading2.setText("Peak Revenue");
            heading3.setText("Monthly Avg");
            heading4.setText("Total Revenue");

            // Set labels with insights
            if (label1 != null) {
                label1.setText(bestMonth);
            }
            if (label2 != null) {
                label2.setText("Rs. " + df.format(maxRevenue));
            }
            if (label3 != null) {
                double monthlyAvg = monthCount > 0 ? totalRevenue / monthCount : 0;
                label3.setText("Rs. " + df.format(monthlyAvg));
            }
            if (label4 != null) {
                label4.setText("Rs. " + df.format(totalRevenue));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //POPULAR PRODUCTS
    private void analyzePopularProducts(String fromDate, String toDate) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query = "SELECT "
                    + "pro_name, "
                    + "COUNT(*) as purchase_count, "
                    + "SUM(qty) as total_quantity, "
                    + "SUM(total_price) as total_revenue, "
                    + "COUNT(DISTINCT cus_id) as unique_customers, "
                    + "AVG(per_unit) as avg_price "
                    + "FROM transactions "
                    + "WHERE date >= ? AND date <= ? "
                    + "GROUP BY pro_name "
                    + "ORDER BY purchase_count DESC "
                    + "LIMIT 20";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            // Update table
            DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
            model.setRowCount(0);
            model.setColumnIdentifiers(new String[]{"Rank", "Product", "Purchases", "Quantity", "Customers", "Revenue"});

            DecimalFormat df = new DecimalFormat("#,##0.00");
            int rank = 1;
            int totalProducts = 0;
            String topProduct = "";
            int maxPurchases = 0;

            // For datasets
            DefaultCategoryDataset popularityDataset = new DefaultCategoryDataset();
            DefaultPieDataset pieDataset = new DefaultPieDataset();

            while (rs.next()) {
                totalProducts++;
                String product = rs.getString("pro_name");
                int purchases = rs.getInt("purchase_count");
                int quantity = rs.getInt("total_quantity");
                double revenue = rs.getDouble("total_revenue");
                int customers = rs.getInt("unique_customers");

                // Add to table
                Object[] row = new Object[6];
                row[0] = rank++;
                row[1] = product;
                row[2] = purchases;
                row[3] = quantity;
                row[4] = customers;
                row[5] = "Rs. " + df.format(revenue);
                model.addRow(row);

                // Add to datasets
                if (rank <= 11) {
                    popularityDataset.addValue(purchases, "Purchases", product);
                    pieDataset.setValue(product, purchases);
                }

                if (purchases > maxPurchases) {
                    maxPurchases = purchases;
                    topProduct = product;
                }
            }

            // Create charts
            createBarChart(popularityDataset, "Top Products by Purchases", "Product", "Number of Purchases");
            createPieChart(pieDataset, "Purchase Distribution");

            // Set headings
            heading1.setText("Top Product");
            heading2.setText("Purchases");
            heading3.setText("Total Products");
            heading4.setText("Top Revenue");

            // Set labels
            if (label1 != null) {
                label1.setText(topProduct);
            }
            if (label2 != null) {
                label2.setText(String.valueOf(maxPurchases));
            }
            if (label3 != null) {
                label3.setText(String.valueOf(totalProducts));
            }
            if (label4 != null) {
                label4.setText("See table");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //CUSTOMER PURCHASE FREQUENCY
    private void analyzeCustomerPurchaseFrequency(String fromDate, String toDate) {
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();

            String query = "SELECT "
                    + "cus_id, "
                    + "COUNT(DISTINCT tra_id) as visit_count, "
                    + "COUNT(*) as total_items, "
                    + "SUM(qty) as total_quantity, "
                    + "SUM(total_price) as total_spent, "
                    + "MIN(date) as first_purchase, "
                    + "MAX(date) as last_purchase "
                    + "FROM transactions "
                    + "WHERE date >= ? AND date <= ? "
                    + "GROUP BY cus_id "
                    + "HAVING COUNT(DISTINCT tra_id) > 1 "
                    + "ORDER BY visit_count DESC "
                    + "LIMIT 20";

            pst = con.prepareStatement(query);
            pst.setString(1, fromDate);
            pst.setString(2, toDate);

            rs = pst.executeQuery();

            // Update table
            DefaultTableModel model = (DefaultTableModel) dataTable.getModel();
            model.setRowCount(0);
            model.setColumnIdentifiers(new String[]{"Rank", "Customer ID", "Visits", "Items", "Quantity", "Total Spent"});

            DecimalFormat df = new DecimalFormat("#,##0.00");
            int rank = 1;
            int totalCustomers = 0;
            String topCustomer = "";
            int maxVisits = 0;

            // For datasets
            DefaultCategoryDataset customerDataset = new DefaultCategoryDataset();

            while (rs.next()) {
                totalCustomers++;
                String cusId = rs.getString("cus_id");
                int visits = rs.getInt("visit_count");
                int items = rs.getInt("total_items");
                int quantity = rs.getInt("total_quantity");
                double spent = rs.getDouble("total_spent");

                // Add to table
                Object[] row = new Object[6];
                row[0] = rank++;
                row[1] = cusId;
                row[2] = visits;
                row[3] = items;
                row[4] = quantity;
                row[5] = "Rs. " + df.format(spent);
                model.addRow(row);

                // Add to dataset for chart (limit to top 10)
                if (rank <= 11) {
                    customerDataset.addValue(visits, "Visits", "Cust " + cusId);
                }

                if (visits > maxVisits) {
                    maxVisits = visits;
                    topCustomer = cusId;
                }
            }

            // Create bar chart for top customers
            createBarChart(customerDataset, "Top Customers by Visit Frequency", "Customer", "Number of Visits");

            // Clear pie chart
            DefaultPieDataset emptyPie = new DefaultPieDataset();
            createPieChart(emptyPie, "Customer Distribution");

            // Set headings
            heading1.setText("Top Customer");
            heading2.setText("Visit Count");
            heading3.setText("Total Customers");
            heading4.setText("Avg per Customer");

            // Set labels
            if (label1 != null) {
                label1.setText("Cust " + topCustomer);
            }
            if (label2 != null) {
                label2.setText(maxVisits + " visits");
            }
            if (label3 != null) {
                label3.setText(totalCustomers + " customers");
            }
            if (label4 != null) {
                // Show average
                label4.setText("See table");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    //CHART METHODS
    private void createBarChart(DefaultCategoryDataset dataset, String title, String xAxis, String yAxis) {
        if (dataset.getRowCount() == 0) {
            
            // Show empty chart with message
            DefaultCategoryDataset emptyDataset = new DefaultCategoryDataset();
            JFreeChart emptyChart = ChartFactory.createBarChart(
                    title + " (No Data)",
                    xAxis,
                    yAxis,
                    emptyDataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );

            ChartPanel chartPanel = new ChartPanel(emptyChart);
            chartPanel.setPreferredSize(new Dimension(542, 441));

            barChartPanel.removeAll();
            barChartPanel.setLayout(new BorderLayout());
            barChartPanel.add(chartPanel, BorderLayout.CENTER);
            barChartPanel.revalidate();
            barChartPanel.repaint();
            return;
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                title,
                xAxis,
                yAxis,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // Customize chart
        barChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
        CategoryPlot plot = barChart.getCategoryPlot();
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 11));

        // Rotate labels for better fit if many categories
        if (dataset.getColumnCount() > 5) {
            plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }

        // Color bars
        CategoryItemRenderer renderer = plot.getRenderer();
        for (int i = 0; i < dataset.getRowCount(); i++) {
            renderer.setSeriesPaint(i, new Color(52, 152, 219)); // Blue
        }

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(542, 441));
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setMaximumDrawHeight(2000);

        barChartPanel.removeAll();
        barChartPanel.setLayout(new BorderLayout());
        barChartPanel.add(chartPanel, BorderLayout.CENTER);
        barChartPanel.revalidate();
        barChartPanel.repaint();
    }

    private void createPieChart(DefaultPieDataset dataset, String title) {
        if (dataset.getItemCount() == 0) {
            // Show empty chart with message
            DefaultPieDataset emptyDataset = new DefaultPieDataset();
            JFreeChart emptyChart = ChartFactory.createPieChart(
                    title + " (No Data)",
                    emptyDataset,
                    true, true, false
            );

            ChartPanel chartPanel = new ChartPanel(emptyChart);
            chartPanel.setPreferredSize(new Dimension(542, 441));

            pieChartPanel.removeAll();
            pieChartPanel.setLayout(new BorderLayout());
            pieChartPanel.add(chartPanel, BorderLayout.CENTER);
            pieChartPanel.revalidate();
            pieChartPanel.repaint();
            return;
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                title,
                dataset,
                true, true, false
        );

        // Customize chart
        pieChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 14));
        pieChart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 11));

        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(542, 441));
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setMaximumDrawHeight(2000);

        pieChartPanel.removeAll();
        pieChartPanel.setLayout(new BorderLayout());
        pieChartPanel.add(chartPanel, BorderLayout.CENTER);
        pieChartPanel.revalidate();
        pieChartPanel.repaint();
    }

    private void generatePDFReport(String analysisType, String fromDate, String toDate) {
        // You can implement PDF generation similar to your SalesAnalysis class
        // For now, show a message
        JOptionPane.showMessageDialog(this, "PDF Export for " + analysisType + "\nFrom: " + fromDate + "\nTo: " + toDate,
                "Generate Report", JOptionPane.INFORMATION_MESSAGE);
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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        barChartPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        pieChartPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        dataTable = new javax.swing.JTable();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        analysisComboBox = new javax.swing.JComboBox<>();
        dateFromChooser = new com.toedter.calendar.JDateChooser();
        jLabel5 = new javax.swing.JLabel();
        dateToChooser = new com.toedter.calendar.JDateChooser();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        refreshBtn = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        heading1 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        label1 = new javax.swing.JLabel();
        heading2 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        label2 = new javax.swing.JLabel();
        heading3 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        label3 = new javax.swing.JLabel();
        heading4 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        label4 = new javax.swing.JLabel();
        resetBtn = new javax.swing.JButton();
        generateReportBtn = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        barChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
        barChartPanel.setLayout(barChartPanelLayout);
        barChartPanelLayout.setHorizontalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 542, Short.MAX_VALUE)
        );
        barChartPanelLayout.setVerticalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 441, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(barChartPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(305, 305, 305))
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
            .addGap(0, 434, Short.MAX_VALUE)
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

        dataTable.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(dataTable);
        if (dataTable.getColumnModel().getColumnCount() > 0) {
            dataTable.getColumnModel().getColumn(0).setResizable(false);
            dataTable.getColumnModel().getColumn(1).setResizable(false);
            dataTable.getColumnModel().getColumn(2).setResizable(false);
            dataTable.getColumnModel().getColumn(3).setResizable(false);
        }

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel10.setText("Regional Sales Data");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 529, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 353, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(62, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Table View", jPanel2);

        jLabel12.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel12.setText("Analysis Type:");

        jSeparator1.setForeground(new java.awt.Color(0, 0, 102));

        analysisComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        analysisComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analysisComboBoxActionPerformed(evt);
            }
        });

        jLabel5.setBackground(new java.awt.Color(0, 51, 102));
        jLabel5.setFont(new java.awt.Font("Microsoft JhengHei UI", 1, 22)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(1, 34, 98));
        jLabel5.setText("Customer Behavior Analysis");

        jLabel2.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel2.setText("From :");

        jLabel3.setFont(new java.awt.Font("Microsoft PhagsPa", 0, 18)); // NOI18N
        jLabel3.setText("To:");

        refreshBtn.setBackground(new java.awt.Color(215, 223, 255));
        refreshBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        refreshBtn.setText("Refresh");
        refreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshBtnActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Product Analysis");

        heading1.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        heading1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        heading1.setText("Total Sales");

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 29, Short.MAX_VALUE)
        );

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));

        label1.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        heading2.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        heading2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        heading2.setText("Total Revenue ");

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        label2.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        heading3.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        heading3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        heading3.setText("Top Region for This Product");

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));

        label3.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        heading4.setFont(new java.awt.Font("Microsoft Tai Le", 0, 16)); // NOI18N
        heading4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        heading4.setText("Top Region for This Product");

        jPanel14.setBackground(new java.awt.Color(255, 255, 255));

        label4.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 17)); // NOI18N

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(heading2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(heading3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                        .addComponent(heading1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel14, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(heading4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(113, 113, 113)
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(heading1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(heading2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(heading3, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(heading4, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        resetBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        resetBtn.setText("Reset");
        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetBtnActionPerformed(evt);
            }
        });

        generateReportBtn.setBackground(new java.awt.Color(13, 42, 171));
        generateReportBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 16)); // NOI18N
        generateReportBtn.setForeground(new java.awt.Color(255, 255, 255));
        generateReportBtn.setText("Generate Report");
        generateReportBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateReportBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 562, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(resetBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(refreshBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(generateReportBtn))))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(analysisComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGap(37, 37, 37)
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(dateFromChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(dateToChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 848, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(27, 27, 27))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(dateFromChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(dateToChooser, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(analysisComboBox))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(refreshBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(generateReportBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 490, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
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

    private void analysisComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analysisComboBoxActionPerformed

    }//GEN-LAST:event_analysisComboBoxActionPerformed

    private void refreshBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshBtnActionPerformed
        dataAnalysis();
    }//GEN-LAST:event_refreshBtnActionPerformed

    private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed

        analysisComboBox.setSelectedIndex(0);
        setDefaultDates();
        clearChartsAndTable();

    }//GEN-LAST:event_resetBtnActionPerformed

    private void generateReportBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateReportBtnActionPerformed

        String analysisType = (String) analysisComboBox.getSelectedItem();

        if (analysisType == null || analysisType.equals("-- Select Analysis Type --")) {
            JOptionPane.showMessageDialog(this, "Please select an analysis type first",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date fromSelectedDate = dateFromChooser.getDate();
        Date toSelectedDate = dateToChooser.getDate();

        if (fromSelectedDate == null || toSelectedDate == null) {
            JOptionPane.showMessageDialog(this, "Please select both From and To dates",
                    "Date Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat dbFormat = new SimpleDateFormat("MM/dd/yyyy");
        String fromDate = dbFormat.format(fromSelectedDate);
        String toDate = dbFormat.format(toSelectedDate);
        String dbFromDate = convertToDbFormat(fromDate);
        String dbToDate = convertToDbFormat(toDate);

        JOptionPane.showMessageDialog(this,
                "PDF Export for " + analysisType + "\nFrom: " + fromDate + "\nTo: " + toDate,
                "Generate Report", JOptionPane.INFORMATION_MESSAGE);

    }//GEN-LAST:event_generateReportBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> analysisComboBox;
    private javax.swing.JPanel barChartPanel;
    private javax.swing.JTable dataTable;
    private com.toedter.calendar.JDateChooser dateFromChooser;
    private com.toedter.calendar.JDateChooser dateToChooser;
    private javax.swing.JButton generateReportBtn;
    private javax.swing.JLabel heading1;
    private javax.swing.JLabel heading2;
    private javax.swing.JLabel heading3;
    private javax.swing.JLabel heading4;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel label1;
    private javax.swing.JLabel label2;
    private javax.swing.JLabel label3;
    private javax.swing.JLabel label4;
    private javax.swing.JPanel pieChartPanel;
    private javax.swing.JButton refreshBtn;
    private javax.swing.JButton resetBtn;
    // End of variables declaration//GEN-END:variables
}
