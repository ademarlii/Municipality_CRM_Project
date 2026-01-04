package com.ademarli.municipality_service.integration.e2e.ui.admin;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AdminLoginAndCreateDepartmentTest extends BaseUiE2ETest {

    private static final String TID_EMAIL  = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASS   = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT = "[data-testid='auth-login-submit']";

    private static final String NAV_DEPTS  = "[data-testid='nav-Departmanlar']";

    private static final String BTN_NEW_DEPT = "[data-testid='admin-dept-new']";
    private static final String INP_NAME     = "#department-name-input";
    private static final String SW_ACTIVE    = ".department-active-checkbox";
    private static final String BTN_SAVE     = ".department-submit-button";

    private static final By DEPT_CARDS  = By.cssSelector("[data-testid^='dept-card-']");
    private static final By DIALOG_ROOT = By.cssSelector(".MuiDialog-root");

    private void loginAsAdmin() {
        driver.get(UI_BASE + "/auth/login");
        waitForDocumentReady();

        typeCss(TID_EMAIL, "admin@local.com");
        typeCss(TID_PASS, "Admin123!");
        safeClick(By.cssSelector(TID_SUBMIT));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(NAV_DEPTS)));
    }

    private void goDepartmentsPage() {
        safeClick(By.cssSelector(NAV_DEPTS));
        wait.until(ExpectedConditions.urlContains("/admin/departments"));

        // sayfa render olsun: ya kart gelsin ya da yeni oluştur butonu gelsin
        wait.until(ExpectedConditions.or(
                ExpectedConditions.numberOfElementsToBeMoreThan(DEPT_CARDS, 0),
                ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_NEW_DEPT))
        ));
    }

    // -------------------- TESTS --------------------

    @Test
    void admin_can_create_department() {
        loginAsAdmin();
        goDepartmentsPage();

        String deptName = "Bilgi işlem " + UUID.randomUUID();

        safeClick(By.cssSelector(BTN_NEW_DEPT));

        WebElement nameInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(INP_NAME)));
        clearAndType(nameInput, deptName);

        setActiveSwitch(true);

        safeClick(By.cssSelector(BTN_SAVE));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG_ROOT));

        WebElement created = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[normalize-space(text())='" + deptName + "']")
        ));
        assertTrue(created.isDisplayed());
    }

    @Test
    void admin_toogle_status_departments() {
        loginAsAdmin();
        goDepartmentsPage();

        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(DEPT_CARDS, 0));

        String passiveId = findFirstDepartmentIdByStatus("Pasif");
        Assertions.assertNotNull(passiveId, "Pasif departman bulunamadı! (Hepsi aktif olabilir)");

        safeClick(By.cssSelector("[data-testid='dept-edit-" + passiveId + "']"));
        setActiveSwitch(true);

        safeClick(By.cssSelector(BTN_SAVE));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG_ROOT));

        // ✅ tekrar DOM'dan oku (stale olmasın)
        assertEventuallyTextEquals(
                By.cssSelector("[data-testid='dept-status-" + passiveId + "']"),
                "Aktif",
                Duration.ofSeconds(8)
        );
    }

    @Test
    void soft_delete_target_department() {
        loginAsAdmin();
        goDepartmentsPage();

        String targetDeptName = "Fen İşleri";

        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(DEPT_CARDS, 0));

        String targetId = findDepartmentIdByName(targetDeptName);
        Assertions.assertNotNull(targetId, targetDeptName + " departmanı bulunamadı!");

        safeClick(By.cssSelector("[data-testid='dept-delete-" + targetId + "']"));

        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();

        // ✅ Kritik fix: stale olmaması için element'i her seferinde yeniden bul
        assertEventuallyTextEquals(
                By.cssSelector("[data-testid='dept-status-" + targetId + "']"),
                "Pasif",
                Duration.ofSeconds(10)
        );
    }

    @Test
    void admin_make_passife_departments_departmentName_knowlage() {
        loginAsAdmin();
        goDepartmentsPage();

        String targetDeptName = "Zabıta";

        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(DEPT_CARDS, 0));

        String targetId = findDepartmentIdByName(targetDeptName);
        Assertions.assertNotNull(targetId, targetDeptName + " departmanı bulunamadı!");

        safeClick(By.cssSelector("[data-testid='dept-edit-" + targetId + "']"));
        setActiveSwitch(false);

        safeClick(By.cssSelector(BTN_SAVE));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DIALOG_ROOT));

        assertEventuallyTextEquals(
                By.cssSelector("[data-testid='dept-status-" + targetId + "']"),
                "Pasif",
                Duration.ofSeconds(8)
        );
    }

    // -------------------- FIND HELPERS --------------------

    private String findDepartmentIdByName(String deptName) {
        // DOM re-render olabilir -> her çağrıda taze liste
        List<WebElement> cards = driver.findElements(DEPT_CARDS);
        for (WebElement card : cards) {
            try {
                WebElement nameEl = card.findElement(By.cssSelector("[data-testid^='dept-name-']"));
                String name = nameEl.getText().trim();
                if (name.equalsIgnoreCase(deptName)) {
                    String tid = card.getAttribute("data-testid");
                    return tid.replace("dept-card-", "");
                }
            } catch (StaleElementReferenceException ignored) {
                // re-render oldu, bir sonraki çağrıda zaten tekrar bulacağız
                return findDepartmentIdByName(deptName);
            }
        }
        return null;
    }

    private String findFirstDepartmentIdByStatus(String statusTr) {
        List<WebElement> cards = driver.findElements(DEPT_CARDS);
        for (WebElement card : cards) {
            try {
                WebElement statusEl = card.findElement(By.cssSelector("[data-testid^='dept-status-']"));
                String status = statusEl.getText().trim();
                if (statusTr.equalsIgnoreCase(status)) {
                    String tid = card.getAttribute("data-testid");
                    return tid.replace("dept-card-", "");
                }
            } catch (StaleElementReferenceException ignored) {
                return findFirstDepartmentIdByStatus(statusTr);
            }
        }
        return null;
    }

    // -------------------- SWITCH HELPERS --------------------

    private void setActiveSwitch(boolean shouldBeActive) {
        WebElement sw = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SW_ACTIVE)));

        boolean current = readMuiChecked(sw);
        if (current != shouldBeActive) {
            sw.click();
            // sw stale olabilir, tekrar oku:
            wait.until(d -> {
                try {
                    WebElement again = d.findElement(By.cssSelector(SW_ACTIVE));
                    return readMuiChecked(again) == shouldBeActive;
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });
        }
    }

    private boolean readMuiChecked(WebElement swRoot) {
        try {
            String aria = swRoot.getAttribute("aria-checked");
            if (aria != null) return Boolean.parseBoolean(aria);
        } catch (Exception ignored) {}

        try {
            WebElement input = swRoot.findElement(By.cssSelector("input[type='checkbox']"));
            return input.isSelected();
        } catch (Exception ignored) {}

        try {
            String cls = swRoot.getAttribute("class");
            return cls != null && cls.contains("Mui-checked");
        } catch (Exception ignored) {}

        return false;
    }

    // -------------------- ASSERT / RETRY (stale-safe) --------------------

    private void assertEventuallyTextEquals(By by, String expected, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        Throwable last = null;

        while (System.currentTimeMillis() < end) {
            try {
                WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(by));
                String txt = el.getText().trim();
                if (expected.equalsIgnoreCase(txt)) return;
            } catch (StaleElementReferenceException | TimeoutException e) {
                last = e;
            }

            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        fail("Beklenen text='" + expected + "' ama gelmedi. Locator=" + by + (last != null ? (" last=" + last) : ""));
    }
}
