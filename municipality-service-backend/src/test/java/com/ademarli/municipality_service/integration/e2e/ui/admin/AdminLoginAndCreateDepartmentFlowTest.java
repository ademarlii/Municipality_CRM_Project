package com.ademarli.municipality_service.integration.e2e.ui.admin;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AdminLoginAndCreateDepartmentFlowTest extends BaseUiE2ETest {

    private static final String TID_EMAIL  = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASS   = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT = "[data-testid='auth-login-submit']";

    private static final String NAV_DEPTS = "[data-testid='nav-Departmanlar']";

    private static final String BTN_NEW_DEPT = "[data-testid='admin-dept-new']";
    private static final String INP_NAME     = "#department-name-input";
    private static final String SW_ACTIVE    = ".department-active-checkbox";
    private static final String BTN_SAVE     = ".department-submit-button";

    @Test
    void admin_create_department_passive_then_activate_flow() {
        loginAsAdmin();
        goDepartmentsPage();

        // 1) create department PASSIVE
        String deptName = "E2E-Pasif-" + UUID.randomUUID();
        createDepartment(deptName, false);

        // 2) find created card id
        String deptId = findDeptIdByName(deptName);
        assertNotNull(deptId, "Oluşturulan departman kartı bulunamadı: " + deptName);

        // 3) assert status = Pasif
        assertDeptStatus(deptId, "Pasif");

        // 4) edit -> make ACTIVE
        openEditDialog(deptId);
        setActiveSwitch(true);
        saveDialog();

        // 5) assert status = Aktif
        assertDeptStatus(deptId, "Aktif");
    }

    // -------------------- flow helpers --------------------

    private void loginAsAdmin() {
        driver.get(UI_BASE + "/auth/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TID_EMAIL)))
                .sendKeys("admin@local.com");
        driver.findElement(By.cssSelector(TID_PASS)).sendKeys("Admin123!");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(TID_SUBMIT))).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(NAV_DEPTS)));
    }

    private void goDepartmentsPage() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(NAV_DEPTS))).click();
        wait.until(ExpectedConditions.urlContains("/admin/departments"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(BTN_NEW_DEPT)));
    }

    private void createDepartment(String name, boolean active) {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_NEW_DEPT))).click();

        WebElement nameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(INP_NAME)));
        nameInput.clear();
        nameInput.sendKeys(name);

        setActiveSwitch(active);

        saveDialog();

        // created row visible by name (listeye eklendi mi)
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[normalize-space(text())='" + name + "']")
        ));
    }

    private void openEditDialog(String deptId) {
        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("[data-testid='dept-edit-" + deptId + "']")
        ));
        safeClick(editBtn);

        // dialog açıldı mı
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".MuiDialog-root")));
    }

    private void saveDialog() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_SAVE))).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".MuiDialog-root")));
    }

    // -------------------- assertions & finders --------------------

    private String findDeptIdByName(String targetDeptName) {
        List<WebElement> cards = wait.until(d -> d.findElements(By.cssSelector("[data-testid^='dept-card-']")));
        for (WebElement card : cards) {
            WebElement nameEl = card.findElement(By.cssSelector("[data-testid^='dept-name-']"));
            String name = nameEl.getText().trim();
            if (name.equalsIgnoreCase(targetDeptName)) {
                String tid = card.getAttribute("data-testid");   // dept-card-123
                return tid.replace("dept-card-", "");
            }
        }
        return null;
    }

    private void assertDeptStatus(String deptId, String expected) {
        WebElement statusEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='dept-status-" + deptId + "']")
        ));
        assertEquals(expected, statusEl.getText().trim(), "Departman status beklenen değil!");
    }


    private void setActiveSwitch(boolean shouldBeActive) {
        WebElement sw = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SW_ACTIVE)));

        boolean current = readMuiChecked(sw);
        if (current != shouldBeActive) {
            safeClick(sw);
            wait.until(d -> readMuiChecked(sw) == shouldBeActive);
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

}
