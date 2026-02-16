/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package ui;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;

public class RegionalSalesTest {

    @Test
    public void testGetRegionalSalesData_NotNull() {
        RegionalSales rs = new RegionalSales();
        Map<String, Double> data = rs.getRegionalSalesData();

        assertNotNull("Regional sales data should not be null", data);
    }

    @Test
    public void testGetRegionalSalesData_HasEntries() {
        RegionalSales rs = new RegionalSales();
        Map<String, Double> data = rs.getRegionalSalesData();

        assertTrue("There should be at least one region in data", data.size() > 0);
    }

    @Test
    public void testGetRegionalSalesByDateRange() {
        RegionalSales rs = new RegionalSales();

        Map<String, Double> data
                = rs.getRegionalSalesByDateRange("01/01/2024", "12/31/2024");

        assertNotNull("Regional sales by date range should not be null", data);
        assertTrue("Data should have entries or be empty but not null", data.size() >= 0);
    }

}
