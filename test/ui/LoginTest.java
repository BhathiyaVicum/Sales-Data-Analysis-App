package ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginTest {

    @Test
    public void testValidateInput_EmptyUsername() {

        Login login = new Login();
        boolean result = login.validateInput("", "1234");

        assertFalse(result);
    }

    @Test
    public void testValidateInput_EmptyPassword() {

        Login login = new Login();
        boolean result = login.validateInput("admin", "");

        assertFalse(result);
    }

    @Test
    public void testValidateInput_ValidInput() {

        Login login = new Login();
        boolean result = login.validateInput("admin", "1234");

        assertTrue(result);
    }
}
