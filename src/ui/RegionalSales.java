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
    }

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

    public void avgOrders() {
        
        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT SUM(total_price) AS total_sales, " +
               "COUNT(DISTINCT region) AS total_regions, " +
               "SUM(total_price) / COUNT(DISTINCT region) AS avg_sales_per_region " +
               "FROM transactions";
           
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
    
    public void topRegion() {
        
        Connection con = null;
        PreparedStatement pst = null;
        java.sql.ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT region, sum(qty) AS top_region from transactions ORDER BY top_region DESC LIMIT 1";
           
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
    
    public Map<String, Double> getRegionalSalesData() {
        Map<String, Double> regionSales = new HashMap<>();

        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;

        try {
            con = db.getConnection();
            String query = "SELECT region, SUM(total_price) as total_sales " +
                           "FROM transactions GROUP BY region";
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
        chartPanel.setPreferredSize(new java.awt.Dimension(350, 250));
        chartPanel.setMaximumDrawWidth(300);
        chartPanel.setMaximumDrawHeight(250);

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
        chartPanel.setPreferredSize(new java.awt.Dimension(400, 250));
        chartPanel.setMaximumDrawWidth(390);
        chartPanel.setMaximumDrawHeight(250);

        barChartPanel.removeAll();
        barChartPanel.setLayout(new java.awt.BorderLayout());
        barChartPanel.add(chartPanel, BorderLayout.CENTER);
        barChartPanel.revalidate();
        barChartPanel.repaint();
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
        pieChartPanel = new javax.swing.JPanel();
        barChartPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        uploadBtn = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        uploadBtn1 = new javax.swing.JButton();

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
            .addComponent(top_region, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
            .addComponent(average_per_region, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        pieChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout pieChartPanelLayout = new javax.swing.GroupLayout(pieChartPanel);
        pieChartPanel.setLayout(pieChartPanelLayout);
        pieChartPanelLayout.setHorizontalGroup(
            pieChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 482, Short.MAX_VALUE)
        );
        pieChartPanelLayout.setVerticalGroup(
            pieChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 351, Short.MAX_VALUE)
        );

        barChartPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
        barChartPanel.setLayout(barChartPanelLayout);
        barChartPanelLayout.setHorizontalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 355, Short.MAX_VALUE)
        );
        barChartPanelLayout.setVerticalGroup(
            barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(barChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(5, 5, 5)
                .addComponent(pieChartPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pieChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(barChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(9, 9, 9))
        );

        jTabbedPane1.addTab("Chart View", jPanel3);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 854, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 366, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Table View", jPanel4);

        uploadBtn.setBackground(new java.awt.Color(13, 42, 171));
        uploadBtn.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 14)); // NOI18N
        uploadBtn.setForeground(new java.awt.Color(255, 255, 255));
        uploadBtn.setText("Generate Report");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        uploadBtn1.setBackground(new java.awt.Color(215, 223, 255));
        uploadBtn1.setFont(new java.awt.Font("Microsoft Sans Serif", 0, 14)); // NOI18N
        uploadBtn1.setText("Refresh");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTabbedPane1)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(uploadBtn1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(uploadBtn))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(50, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(uploadBtn1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(uploadBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
                    .addComponent(jComboBox1))
                .addGap(18, 18, 18)
                .addComponent(jTabbedPane1)
                .addGap(22, 22, 22))
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

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel average_per_region;
    private javax.swing.JPanel barChartPanel;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
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
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel pieChartPanel;
    private javax.swing.JLabel top_region;
    private javax.swing.JLabel total_regions;
    private javax.swing.JLabel total_sales_lbl;
    private javax.swing.JLabel total_sales_lbl1;
    private javax.swing.JLabel total_sales_lbl2;
    private javax.swing.JLabel total_sales_lbl3;
    private javax.swing.JLabel total_sales_lbl6;
    private javax.swing.JLabel total_sales_lbl7;
    private javax.swing.JLabel total_sales_lbl8;
    private javax.swing.JButton uploadBtn;
    private javax.swing.JButton uploadBtn1;
    // End of variables declaration//GEN-END:variables
}
