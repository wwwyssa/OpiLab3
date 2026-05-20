package com.example.laba4.functional;

import java.time.Duration;
import java.util.List;

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

@Tag("functional")
@DisplayName("Selenium: PointChecker functional tests")
class PointCheckerFuncTest {

    private static final String BASE_URL = "http://localhost:4200";
    private static final String AUTH_URL = BASE_URL + "/auth";
    private static final String HOME_URL = BASE_URL + "/";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = false;
        if (Boolean.getBoolean("selenium.headless")) headless = true;
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,108080");
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

    private void registerAndGoHome(String username, String password) {
        driver.get(AUTH_URL);
        WebElement title = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        if (title.getText().contains("Вход")) {
            driver.findElement(By.cssSelector(".toggle-mode button")).click();
        }
        wait.until(ExpectedConditions.textToBe(By.tagName("h1"), "Регистрация"));

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("form .submit-btn[type='submit']")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("canvas#canvas")));
    }

    private void openHome() {
        driver.get(HOME_URL);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("canvas#canvas")));
    }


    private void selectByLabelText(String labelsSelector, String valueText, String fieldName) {
        List<WebElement> labels = driver.findElements(By.cssSelector(labelsSelector));
        for (WebElement label : labels) {
            if (label.getText().trim().equals(valueText)) {
                WebElement input = label.findElement(By.tagName("input"));
                try {
                    input.click();
                } catch (Exception e) {
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                            "const input=arguments[0];" +
                                    "input.click();" +
                                    "input.dispatchEvent(new Event('change',{bubbles:true}));",
                            input);
                }
                wait.until(d -> input.isSelected());
                return;
            }
        }
        throw new IllegalStateException(fieldName + " value label not found: " + valueText);
    }

    private void selectR(int r) {
        selectByLabelText(".form-container .form-group:nth-of-type(3) .checkbox-group label", String.valueOf(r), "R");
        wait.until(d -> {
            try {
                Object v = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "const app=document.querySelector('app-home');const c=window.ng && window.ng.getComponent ? window.ng.getComponent(app) : null;return c ? c.rValue : null;"
                );
                return (v != null) && ((Number) v).intValue() == r;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void selectX(int x) {
        selectByLabelText(".form-container .form-group:nth-of-type(1) .checkbox-group label", String.valueOf(x), "X");
        wait.until(d -> {
            try{
                Object v = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "const c=window.ng?.getComponent?.(document.querySelector('app-home'));return c?.xValue??null;");
                return v instanceof Number && ((Number) v).intValue() == x;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void selectY(double y) {
        WebElement yInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("y-input")));
        yInput.clear();
        yInput.sendKeys(String.valueOf(y));
        
        // Триггерим события для Angular
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            yInput);
    }

    

    private String lastTableHitText() {
        List<WebElement> rows = driver.findElements(By.cssSelector(".points-table tbody tr"));
        WebElement last = rows.get(rows.size() - 1);
        List<WebElement> cols = last.findElements(By.tagName("td"));
        return cols.get(3).getText().trim();
    }

    private void clickCanvasAt(double graphX, double graphY) {
        WebElement canvas = driver.findElement(By.id("canvas"));
        int offsetX = (int) Math.round(graphX * 60.0);
        int offsetY = (int) Math.round(-graphY * 60.0);
        new org.openqa.selenium.interactions.Actions(driver)
            .moveToElement(canvas, offsetX, offsetY)
            .click()
            .perform();
    }

    private void checkHit(boolean expectedHit) {
        String txt = lastTableHitText();
        if (expectedHit) Assertions.assertEquals("Пробитие", txt);
        else Assertions.assertEquals("Промах", txt);
        
    }


    private void triggerR3Clicks() {
        String value = "3";
        List<WebElement> labels = driver.findElements(By.cssSelector(".form-container .form-group:nth-of-type(3) .checkbox-group label"));
        for (WebElement label : labels) {
            if (label.getText().trim().equals(value)) {
                label.click();
                label.click();
                return;
            }
        }
    }

    private void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".logout-btn"))).click();
        wait.until(ExpectedConditions.urlContains("/auth"));
        wait.until(ExpectedConditions.textToBe(By.tagName("h1"), "Вход"));
    }

    @Test
    @DisplayName("Canvas click points: 3 hits")
    void testCanvasClicksHits() {
        String u = "test_" + System.currentTimeMillis();
        String p = "123";
        registerAndGoHome(u, p);

        openHome();

        selectR(3);

        clickCanvasAt(1, 1);
        triggerR3Clicks();
        checkHit(true);

        clickCanvasAt(2, 0);
        triggerR3Clicks();
        checkHit(true);

        clickCanvasAt(-1, -1);
        triggerR3Clicks();
        checkHit(true);
    }

    @Test
    @DisplayName("Check misses")
    void testCanvasClickMisses(){
        String u = "test_" + System.currentTimeMillis();
        String p = "123";
        registerAndGoHome(u, p);
        openHome();

        selectR(5);
        clickCanvasAt(4, 4);
        triggerR3Clicks();
        checkHit(false);


        clickCanvasAt(-4, 0);
        triggerR3Clicks();
        checkHit(false);
        
    }

    @Test
    @DisplayName("Same (x,y) different R")
    void testHitDetectionForDifferentRValues() {
        String u = "test_r_values_" + System.currentTimeMillis();
        String p = "123";
        registerAndGoHome(u, p);
        openHome();

        double testX = 2;
        double testY = 2;
        selectR(2);
        clickCanvasAt(testX, testY);
        triggerR3Clicks();
        checkHit(false);
        selectR(3);
        clickCanvasAt(testX, testY);
        triggerR3Clicks();
        checkHit(true);

        selectR(4);
        clickCanvasAt(testX, testY);
        triggerR3Clicks();
        checkHit(true);

        selectR(1);
        clickCanvasAt(testX, testY);
        triggerR3Clicks();
        checkHit(false);
        List<WebElement> rows = driver.findElements(By.cssSelector(".points-table tbody tr"));
        Assertions.assertEquals(4, rows.size(), "Should have 4 points in table");
    }

    @Test
    @DisplayName("Logout clears auth state and redirects to auth page")
    void testLogout() {
        String u = "test_" + System.currentTimeMillis();
        String p = "123";
        registerAndGoHome(u, p);

        openHome();
        logout();

        Assertions.assertEquals("", (String) ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('token') ?? '';"));
    }

    @Test
    @DisplayName("Graph updates correctly when R value changes")
    void testGraphUpdateOnRChange() throws InterruptedException {
        String u = "test_" + System.currentTimeMillis();
        registerAndGoHome(u, "123");
        openHome();

        selectR(1);
        String canvasData1 = (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const canvas = document.getElementById('canvas');" +
                "const ctx = canvas.getContext('2d');" +
                "return canvas.toDataURL();");

        selectR(5);
        String canvasData5 = (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const canvas = document.getElementById('canvas');" +
                "const ctx = canvas.getContext('2d');" +
                "return canvas.toDataURL();");

        Assertions.assertNotEquals(canvasData1, canvasData5, "Canvas should be redrawn with different R values");
    }

    @Test
    @DisplayName("unauthtorazed user")
    void testUnAuthUser() {
        String expectedUrl = AUTH_URL;
        driver.get(HOME_URL);
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/auth"),
                ExpectedConditions.urlToBe(AUTH_URL)
        ));
        String currentUrl = driver.getCurrentUrl();
        Assertions.assertEquals(expectedUrl, currentUrl,
                String.format("Expected URL: %s, but got: %s", expectedUrl, currentUrl));
    }



}
