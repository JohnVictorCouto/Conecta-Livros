import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MySqlDriverClasspathTest {

    @Test
    void mysqlDriverIsAvailableOnTheTestClasspath() {
        assertDoesNotThrow(() -> Class.forName("com.mysql.cj.jdbc.Driver"));
    }
}
