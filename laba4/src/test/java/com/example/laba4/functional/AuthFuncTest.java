package com.example.laba4.auth;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

@Tag("functionalal")
@DisplayName("Selenium тесты для авторизации пользователей")
class AuthFuncTest {

    private static final String BASE_URL = "http://localhost:4200";
    private static final String AUTH_URL = BASE_URL + "/auth";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = false;
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
    @DisplayName("Успешный вход с валидными данными")
    void testSuccessfulLogin() {
        String username = uniqueUsername("login_user_");
        String password = "password123";

        registerUser(username, password);
        openLoginForm();
        fillAuthForm(username, password);
        submitAuthForm();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("canvas#canvas")));

        Assertions.assertEquals(username, executeScript("return localStorage.getItem('username')"));
        Assertions.assertNotNull(executeScript("return localStorage.getItem('token')"));
        Assertions.assertTrue(driver.getCurrentUrl().startsWith(BASE_URL));
    }

    @Test
    @DisplayName("Ошибка входа с неверным паролем")
    void testLoginWithWrongPassword() {
        String username = uniqueUsername("wrong_password_user_");
        String password = "password123";

        registerUser(username, password);
        openLoginForm();
        fillAuthForm(username, "wrong-pass");
        submitAuthForm();

        WebElement errorMessage = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.className("error-message"))
        );

        Assertions.assertTrue(errorMessage.getText().contains("Неверное имя пользователя или пароль"));
        Assertions.assertNull(executeScript("return localStorage.getItem('token')"));
        Assertions.assertEquals("", driver.findElement(By.id("password")).getAttribute("value"));
    }

    @Test
    @DisplayName("Кнопка входа отключена при пустых полях")
    void testLoginButtonDisabledWhenFieldsAreEmpty() {
        openLoginForm();

        WebElement submitButton = driver.findElement(By.cssSelector("form .submit-btn[type='submit']"));

        Assertions.assertFalse(submitButton.isEnabled(), "Кнопка входа должна быть отключена без данных");
    }

    @Test
    @DisplayName("Переключение между входом и регистрацией очищает форму")
    void testToggleModeClearsAuthForm() {
        openLoginForm();

        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement passwordInput = driver.findElement(By.id("password"));

        usernameInput.sendKeys("temp_user");
        passwordInput.sendKeys("temp_password");

        driver.findElement(By.cssSelector(".toggle-mode button")).click();
        driver.findElement(By.cssSelector(".toggle-mode button")).click();

        usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        passwordInput = driver.findElement(By.id("password"));

        Assertions.assertEquals("", usernameInput.getAttribute("value"));
        Assertions.assertEquals("", passwordInput.getAttribute("value"));
    }

    private void openLoginForm() {
        driver.get(AUTH_URL);
        WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        if (title.getText().contains("Регистрация")) {
            driver.findElement(By.cssSelector(".toggle-mode button")).click();
        }
        wait.until(ExpectedConditions.textToBe(By.tagName("h1"), "Вход"));
    }

    private void registerUser(String username, String password) {
        driver.get(AUTH_URL);
        WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        if (title.getText().contains("Вход")) {
            driver.findElement(By.cssSelector(".toggle-mode button")).click();
        }
        wait.until(ExpectedConditions.textToBe(By.tagName("h1"), "Регистрация"));

        fillAuthForm(username, password);
        submitAuthForm();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("canvas#canvas")));
    }

    private void fillAuthForm(String username, String password) {
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));

        usernameInput.clear();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    private void submitAuthForm() {
        driver.findElement(By.cssSelector("form .submit-btn[type='submit']")).click();
    }

    private Object executeScript(String script) {
        return ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script);
    }

    private String uniqueUsername(String prefix) {
        return prefix + System.currentTimeMillis();
    }
}