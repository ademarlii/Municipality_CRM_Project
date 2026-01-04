package com.ademarli.municipality_service.integration.e2e.ui.support;

import com.ademarli.municipality_service.TestcontainersConfiguration;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseUiE2ETest {

    protected static final String UI_BASE =
            System.getProperty("ui.baseUrl", "http://localhost:5173");

    @LocalServerPort
    protected int port;

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeAll
    void setupDriverManagerOnce() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setupWebDriver() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "true"));
        if (headless) options.addArguments("--headless=new");

        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // origin'e git -> localStorage'a E2E_API_BASE yaz -> refresh
        driver.get(UI_BASE + "/");
        waitForDocumentReady();

        String apiBase = "http://localhost:" + port;
        ((JavascriptExecutor) driver).executeScript(
                "window.localStorage.setItem('E2E_API_BASE', arguments[0]);", apiBase
        );

        driver.navigate().refresh();
        waitForDocumentReady();
    }

    @AfterEach
    void teardownWebDriver() {
        if (driver != null) driver.quit();
    }


    protected void waitForDocumentReady() {
        wait.until(d -> {
            try {
                Object state = ((JavascriptExecutor) d).executeScript("return document.readyState");
                return "complete".equals(String.valueOf(state));
            } catch (Exception e) {
                return true;
            }
        });
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    protected void safeClick(By by) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(by));
        safeClick(el);
    }

    protected void safeClick(WebElement el) {
        try {
            wait.until(ExpectedConditions.visibilityOf(el));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            } catch (Exception ignored) {
                // son çare: tekrar bulup tıkla
                el.click();
            }
        }
    }

    protected void typeCss(String css, String value) {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css)));
        clearAndType(el, value);
    }

    protected void clearAndType(WebElement el, String value) {
        wait.until(ExpectedConditions.visibilityOf(el));
        safeClick(el);
        el.sendKeys(Keys.CONTROL, "a");
        el.sendKeys(Keys.BACK_SPACE);
        el.sendKeys(value);
    }

    protected void selectMuiOptionByText(String selectCss, String optionText) {
        WebElement select = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selectCss)));
        safeClick(select);

        By LISTBOX = By.cssSelector("ul[role='listbox']");
        WebElement listbox = wait.until(ExpectedConditions.visibilityOfElementLocated(LISTBOX));

        By OPTION = By.xpath("//li[@role='option' and normalize-space(.)='" + optionText + "']");
        wait.until(ExpectedConditions.presenceOfElementLocated(OPTION));

        WebElement option = driver.findElement(OPTION);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", option);
        safeClick(option);

        wait.until(ExpectedConditions.invisibilityOf(listbox));
    }

    protected void dismissToastIfPresent() {
        By TOAST = By.cssSelector("[data-testid='toast']");
        By TOAST_CLOSE = By.cssSelector("[data-testid='toast-close']");

        List<WebElement> toasts = driver.findElements(TOAST);
        if (toasts.isEmpty()) return;

        try {
            List<WebElement> closeButtons = driver.findElements(TOAST_CLOSE);
            if (!closeButtons.isEmpty()) {
                safeClick(TOAST_CLOSE);
            }
            wait.until(ExpectedConditions.invisibilityOfElementLocated(TOAST));
        } catch (TimeoutException ignored) {}
    }

    protected static String randomPhoneTr11() {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        sb.append('5');
        for (int i = 0; i < 9; i++) sb.append(r.nextInt(0, 10));
        return "0" + sb;
    }
}
