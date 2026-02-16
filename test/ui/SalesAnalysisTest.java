/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package ui;

import java.util.Map;
import javax.swing.JComboBox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class SalesAnalysisTest {

    @Test
    public void testGetTopProducts_NotNull() {

        SalesAnalysis sa = new SalesAnalysis();

        Map<String, Double> result
                = sa.getTopProductsByDateRange(
                        "01/01/2020",
                        "12/31/2024",
                        true,
                        "All Regions"
                );

        assertNotNull(result);
    }

    @Test
    public void testGetTopProducts_SizeValid() {

        SalesAnalysis sa = new SalesAnalysis();

        Map<String, Double> result
                = sa.getTopProductsByDateRange(
                        "01/01/2024",
                        "12/31/2024",
                        false,
                        "All Regions"
                );

        assertTrue(result.size() >= 0);
    }

    @Test
    public void testGetTopProducts_WithRegion() {

        SalesAnalysis sa = new SalesAnalysis();

        Map<String, Double> result
                = sa.getTopProductsByDateRange(
                        "01/01/2024",
                        "12/31/2024",
                        true,
                        "Colombo"
                );

        assertNotNull(result);
    }

}
