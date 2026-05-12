package com.example.laba4.functional;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
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

    private void setY(double y) {
        WebElement yInput = driver.findElement(By.cssSelector(".form-container input[type='number']"));
        yInput.clear();
        yInput.sendKeys(String.valueOf(y));
    }

    private void submitFormPoint() {
        driver.findElement(By.cssSelector(".form-container .submit-btn")).click();
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

    private void waitForNewPointAndAssertHit(boolean expectedHit) {
        String txt = lastTableHitText();
        if (expectedHit) Assertions.assertEquals("Пробитие", txt);
        else Assertions.assertEquals("Промах", txt);
        
    }

    private int getPointsTableCount() {
        return driver.findElements(By.cssSelector(".points-table tbody tr")).size();
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

    @Test
    @DisplayName("Canvas click points: 3 hits, 2 misses")
    void testCanvasClicksHitsAndMisses() {
        String u = "test_" + System.currentTimeMillis();
        String p = "123";
        registerAndGoHome(u, p);

        openHome();

        selectR(3);

        clickCanvasAt(1, 1);
        triggerR3Clicks();
        waitForNewPointAndAssertHit(true);

        clickCanvasAt(2, 0);
        triggerR3Clicks();
        waitForNewPointAndAssertHit(true);

        clickCanvasAt(-1, -1);
        triggerR3Clicks();
        waitForNewPointAndAssertHit(true);

        clickCanvasAt(4, 4);
        triggerR3Clicks();
        waitForNewPointAndAssertHit(false);

        clickCanvasAt(-4, 0);
        triggerR3Clicks();
        waitForNewPointAndAssertHit(false);
    }


}
