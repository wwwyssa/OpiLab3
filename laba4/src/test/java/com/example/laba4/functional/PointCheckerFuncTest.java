package com.example.laba4.pointChecker;

import java.time.Duration;
import java.util.List;

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

@Tag("selenium")
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
        options.addArguments("--window-size=600,600");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
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
                label.click();
                wait.until(d -> input.isSelected());
                return;
            }
        }
        throw new IllegalStateException(fieldName + " value label not found: " + valueText);
    }

    private void selectR(int r) {
        selectByLabelText(".form-container .form-group:nth-of-type(3) .checkbox-group label", String.valueOf(r), "R");
    }

    private void selectX(int x) {
        selectByLabelText(".form-container .form-group:nth-of-type(1) .checkbox-group label", String.valueOf(x), "X");
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
        String script = "const canvas=arguments[0];const graphX=arguments[1];const graphY=arguments[2];" +
                "const rect=canvas.getBoundingClientRect();const centerX=rect.left+rect.width/2;const centerY=rect.top+rect.height/2;" +
                "const clientX=Math.round(centerX+graphX*60);const clientY=Math.round(centerY-graphY*60);" +
                "canvas.dispatchEvent(new MouseEvent('click',{clientX:clientX,clientY:clientY,bubbles:true,cancelable:true}));";
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script, canvas, graphX, graphY);
    }

    private void waitForNewPointAndAssertHit(boolean expectedHit, int previousCount) {
        long end = System.currentTimeMillis() + WAIT_TIMEOUT.toMillis();
        boolean found = false;
        Boolean lastHit = null;
        while (System.currentTimeMillis() < end) {
            try {
                int size = driver.findElements(By.cssSelector(".points-table tbody tr")).size();
                if (size > previousCount) {
                    lastHit = "Пробитие".equals(lastTableHitText());
                    found = true;
                    break;
                }

                // If table not updated, try to read Angular component model (dev mode only)
                Object compLen = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "try{const c=(window.ng&&ng.getComponent(document.querySelector('app-home')));if(c&&c.points)return {len:c.points.length,hit:c.points.length?c.points[c.points.length-1].hit:null};return {len:0,hit:null};}catch(e){return {len:0,hit:null};}");
                if (compLen instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String,Object> map = (java.util.Map<String,Object>) compLen;
                    Number len = (Number) map.get("len");
                    Object hitObj = map.get("hit");
                        if (len != null && len.intValue() > previousCount) {
                            found = true;
                            if (hitObj instanceof Boolean) lastHit = (Boolean) hitObj;
                            // attempt to fetch last point details for better diagnostics
                            Object lastPoint = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                                    "try{const c=(window.ng&&ng.getComponent(document.querySelector('app-home')));if(c&&c.points&&c.points.length){return c.points[c.points.length-1];}return null;}catch(e){return null;}"
                            );
                            if (lastPoint instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String,Object> lp = (java.util.Map<String,Object>) lastPoint;
                                System.out.println("DEBUG lastPoint model: " + lp);
                            }
                            break;
                        }
                } else if (compLen instanceof org.openqa.selenium.remote.RemoteWebElement) {
                    // ignore
                }
            } catch (org.openqa.selenium.UnhandledAlertException ua) {
                try {
                    driver.switchTo().alert().accept();
                } catch (Exception ignore) {
                }
            } catch (Exception ignore) {
                // ignore transient DOM/access issues
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!found) {
            // capture artifacts for debugging: screenshot, page source, browser console
            try {
                String base = "build/reports/tests/pointchecker-failure-" + System.currentTimeMillis();
                java.io.File screenshotsDir = new java.io.File("build/reports/tests/screenshots");
                screenshotsDir.mkdirs();
                try {
                    org.openqa.selenium.TakesScreenshot ts = (org.openqa.selenium.TakesScreenshot) driver;
                    java.io.File src = ts.getScreenshotAs(org.openqa.selenium.OutputType.FILE);
                    java.nio.file.Files.copy(src.toPath(), java.nio.file.Paths.get(base + ".png"));
                } catch (Exception e) {
                    // ignore screenshot errors
                }
                try {
                    String page = driver.getPageSource();
                    java.nio.file.Files.write(java.nio.file.Paths.get(base + ".html"), page.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                }
                try {
                    java.util.List<org.openqa.selenium.logging.LogEntry> logs = driver.manage().logs().get(org.openqa.selenium.logging.LogType.BROWSER).getAll();
                    StringBuilder sb = new StringBuilder();
                    for (org.openqa.selenium.logging.LogEntry le : logs) {
                        sb.append(new java.util.Date(le.getTimestamp())).append(" ").append(le.getLevel()).append(" ").append(le.getMessage()).append(System.lineSeparator());
                    }
                    java.nio.file.Files.write(java.nio.file.Paths.get(base + ".console.txt"), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                }
            } catch (Exception ignore) {}
            throw new org.openqa.selenium.TimeoutException("Timed out waiting for new point row");
        }

        // Determine hit result: prefer lastHit from model, fall back to table text
        if (lastHit != null) {
            if (expectedHit) Assertions.assertTrue(lastHit, "Expected hit but was miss");
            else Assertions.assertFalse(lastHit, "Expected miss but was hit");
        } else {
            String txt = lastTableHitText();
            if (expectedHit) Assertions.assertEquals("Пробитие", txt);
            else Assertions.assertEquals("Промах", txt);
        }
    }

    private int getPointsTableCount() {
        return driver.findElements(By.cssSelector(".points-table tbody tr")).size();
    }

    // --- 5 clicks: 3 hits, 2 misses ---
    @Test
    @DisplayName("Canvas click points: 3 hits, 2 misses")
    void testCanvasClicksHitsAndMisses() {
        String u = "pc_user_" + System.currentTimeMillis();
        String p = "pass123";
        registerAndGoHome(u, p);

        openHome();

        // choose R = 3
        selectR(3);

        // Hit1: (1,1) -> x^2+y^2=2 <=9 => hit
        int prev = getPointsTableCount();
        clickCanvasAt(1, 1);
        waitForNewPointAndAssertHit(true, prev);

        // Hit2: (2,0) -> 4 <=9 => hit
        prev = getPointsTableCount();
        clickCanvasAt(2, 0);
        waitForNewPointAndAssertHit(true, prev);

        // Hit3: (-1,-1) -> y > -x - r => -1 > 1 -3 => -1 > -2 => true
        prev = getPointsTableCount();
        clickCanvasAt(-1, -1);
        waitForNewPointAndAssertHit(true, prev);

        // Miss1: (4,4) -> clearly outside for R=3
        prev = getPointsTableCount();
        clickCanvasAt(4, 4);
        waitForNewPointAndAssertHit(false, prev);

        // Miss2: (-3,0) -> should be miss
        prev = getPointsTableCount();
        clickCanvasAt(-4, 0);
        waitForNewPointAndAssertHit(false, prev);
    }

    // --- 5 form inputs: mix hits and misses ---
    @Test
    @DisplayName("Form input points: 5 entries with expected hits/misses")
    void testFormInputsPoints() {
        String u = "pc_form_user_" + System.currentTimeMillis();
        String p = "pass123";
        registerAndGoHome(u, p);
        openHome();

        // 1) X=1, Y=1, R=3 => hit
        selectX(1);
        setY(1);
        selectR(3);
        int prevCount = getPointsTableCount();
        submitFormPoint();
        waitForNewPointAndAssertHit(true, prevCount);

        // 2) X=0, Y=0, R=1 => hit (on origin, circle)
        selectX(0);
        setY(0);
        selectR(1);
        prevCount = getPointsTableCount();
        submitFormPoint();
        waitForNewPointAndAssertHit(true, prevCount);

        // 3) X=-1, Y=-2, R=3 => check: y > -x - r => -2 > 1 -3 => -2 > -2 false -> boundary -> treat as miss
        selectX(-1);
        setY(-2);
        selectR(3);
        prevCount = getPointsTableCount();
        submitFormPoint();
        // here expected miss
        waitForNewPointAndAssertHit(false, prevCount);

        // 4) X=2, Y=0, R=2 => x<=r and y>=-r for x>=0,y<=0 branch? Actually y=0 -> hit in rectangle
        selectX(2);
        setY(0);
        selectR(2);
        prevCount = getPointsTableCount();
        submitFormPoint();
        waitForNewPointAndAssertHit(true, prevCount);

        // 5) X=5, Y=3, R=4 => miss (Y adjusted into valid range)
        selectX(5);
        setY(3);
        selectR(4);
        prevCount = getPointsTableCount();
        submitFormPoint();
        waitForNewPointAndAssertHit(false, prevCount);
    }
}
