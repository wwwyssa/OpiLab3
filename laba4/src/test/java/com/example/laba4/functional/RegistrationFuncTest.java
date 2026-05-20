package com.example.laba4.functional;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@Tag("functional")
@DisplayName("Selenium тесты для регистрации пользователей")
class RegistrationFuncTest {

    private static final String BASE_URL = "http://localhost:4200";
    private static final String AUTH_URL = BASE_URL + "/auth";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = Boolean.getBoolean("selenium.headless");
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Успешная регистрация с валидными данными")
    void testSuccessfulRegistration() {
        String username = "test_reg_ok" + System.currentTimeMillis();
        String password = "123";

        openRegistrationForm();
        fillRegistrationForm(username, password);
        submitRegistration();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("canvas#canvas")));

        Assertions.assertEquals(username, executeScript("return localStorage.getItem('username')"));
        Assertions.assertNotNull(executeScript("return localStorage.getItem('token')"));
        Assertions.assertTrue(driver.getCurrentUrl().startsWith(BASE_URL));
    }

    @Test
    @DisplayName("Ошибка регистрации с пустым именем пользователя")
    void testRegistrationWithEmptyUsername() {
        openRegistrationForm();
        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys("123");
        WebElement submitButton = driver.findElement(By.cssSelector("form .submit-btn[type='submit']"));

        Assertions.assertFalse(submitButton.isEnabled(), "Кнопка должна быть отключена с пустыми полями");
    }

    @Test
    @DisplayName("Ошибка регистрации с существующим именем пользователя")
    void testRegistrationWithDuplicateUsername() {
        String username = "test_fail_already_exists_" + System.currentTimeMillis();
        String password = "123";

        openRegistrationForm();
        fillRegistrationForm(username, password);
        submitRegistration();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("canvas#canvas")));

        driver.get(AUTH_URL);
        openRegistrationForm();
        fillRegistrationForm(username, password);
        submitRegistration();

        WebElement errorMessage = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.className("error-message"))
        );

        Assertions.assertTrue(errorMessage.getText().contains("уже существует"));
        Assertions.assertNull(executeScript("return localStorage.getItem('token')"));
    }

    @Test
    @DisplayName("Переключение между режимами входа и регистрации")
    void testToggleBetweenModes() {
        driver.get(AUTH_URL);

        WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        Assertions.assertTrue(title.getText().contains("Вход"));

        driver.findElement(By.cssSelector(".toggle-mode button")).click();

        title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        Assertions.assertTrue(title.getText().contains("Регистрация"));

        driver.findElement(By.cssSelector(".toggle-mode button")).click();

        title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        Assertions.assertTrue(title.getText().contains("Вход"));
    }

    @Test
    @DisplayName("Ошибка отображается при пустом пароле")
    void testRegistrationWithEmptyPassword() {
        openRegistrationForm();
        driver.findElement(By.id("username")).sendKeys("testuser_" + System.currentTimeMillis());
        WebElement submitButton = driver.findElement(By.cssSelector("form .submit-btn[type='submit']"));
        Assertions.assertFalse(submitButton.isEnabled(), "Кнопка регистрации должна быть отключена с пустым паролем");
    }

    

    private void openRegistrationForm() {
        driver.get(AUTH_URL);
        WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        if (title.getText().contains("Вход")) {
            driver.findElement(By.cssSelector(".toggle-mode button")).click();
        }
        wait.until(ExpectedConditions.textToBe(By.tagName("h1"), "Регистрация"));
    }

    private void fillRegistrationForm(String username, String password) {
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));

        usernameInput.clear();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    private void submitRegistration() {
        driver.findElement(By.cssSelector("form .submit-btn[type='submit']")).click();
    }

    private Object executeScript(String script) {
        return ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script);
    }
}
