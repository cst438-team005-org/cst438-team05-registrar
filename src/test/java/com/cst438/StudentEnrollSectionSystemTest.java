package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Assignment 4 system test: a student drops CST599, enrolls again,
 * verifies the transcript, and an instructor verifies the roster.
 *
 * Required services before running:
 * - Registrar: http://localhost:8080
 * - Gradebook: http://localhost:8081
 * - React/Vite frontend: http://localhost:5173
 * - RabbitMQ
 */
public class StudentEnrollSectionSystemTest {

    private static final String FRONTEND_URL = "http://localhost:5173";

    private static final String STUDENT_EMAIL = "sama@csumb.edu";
    private static final String STUDENT_PASSWORD = "sam2025";
    private static final String INSTRUCTOR_EMAIL = "ted@csumb.edu";
    private static final String INSTRUCTOR_PASSWORD = "ted2025";

    private static final String COURSE_ID = "cst599";
    private static final String YEAR = "2025";
    private static final String SEMESTER = "Fall";

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.manage().window().maximize();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void studentDropsAndReEnrollsInCst599() {
        login(STUDENT_EMAIL, STUDENT_PASSWORD, "Student Home");

        viewStudentSchedule();
        selectTerm("Get Schedule");
        dropCourse(COURSE_ID);

        openEnrollmentPage();
        enrollInCourse(COURSE_ID);

        openTranscript();
        verifyTranscriptContainsCourseWithoutGrade(COURSE_ID);

        logout();
        login(INSTRUCTOR_EMAIL, INSTRUCTOR_PASSWORD, "Instructor Home");

        selectTerm("Get Sections");
        openCourseEnrollments(COURSE_ID);
        verifyStudentAppearsExactlyOnce(STUDENT_EMAIL);
    }

    private void login(String email, String password, String expectedHeading) {
        driver.get(FRONTEND_URL);

        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        WebElement passwordInput = driver.findElement(By.id("password"));

        emailInput.clear();
        emailInput.sendKeys(email);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[normalize-space()='" + expectedHeading + "']")));
    }

    private void viewStudentSchedule() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("scheduleLink"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("year")));
    }

    private void selectTerm(String expectedButtonText) {
        WebElement yearInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("year")));
        WebElement semesterInput = driver.findElement(By.id("semester"));

        yearInput.clear();
        yearInput.sendKeys(YEAR);
        semesterInput.clear();
        semesterInput.sendKeys(SEMESTER);

        WebElement button = driver.findElement(By.id("selectTermButton"));
        assertEquals(expectedButtonText, button.getText().trim());
        button.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(courseRow(COURSE_ID)));
    }

    private void dropCourse(String courseId) {
        WebElement courseRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(courseRow(courseId)));
        courseRow.findElement(By.xpath(".//button[normalize-space()='Drop']")).click();

        By yesButton = By.xpath(
                "//div[@class='react-confirm-alert-button-group']/button[@label='Yes']");
        wait.until(ExpectedConditions.elementToBeClickable(yesButton)).click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(yesButton));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(courseRow(courseId)));
    }

    private void openEnrollmentPage() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addCourseLink"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[normalize-space()='Open Sections Available for Enrollment']")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(courseRow(COURSE_ID)));
    }

    private void enrollInCourse(String courseId) {
        WebElement courseRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(courseRow(courseId)));
        courseRow.findElement(By.xpath(".//button[normalize-space()='Add']")).click();

        By yesButton = By.xpath(
                "//div[@class='react-confirm-alert-button-group']/button[@label='Yes']");
        wait.until(ExpectedConditions.elementToBeClickable(yesButton)).click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(yesButton));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(courseRow(courseId)));
    }

    private void openTranscript() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("transcriptLink"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[normalize-space()='Transcript']")));
    }

    private void verifyTranscriptContainsCourseWithoutGrade(String courseId) {
        WebElement row = wait.until(
                ExpectedConditions.visibilityOfElementLocated(courseRow(courseId)));
        List<WebElement> cells = row.findElements(By.tagName("td"));

        assertEquals(7, cells.size(), "Unexpected transcript table format.");
        assertEquals(YEAR, cells.get(0).getText().trim());
        assertEquals(SEMESTER, cells.get(1).getText().trim());
        assertEquals(courseId, cells.get(2).getText().trim().toLowerCase());
        assertTrue(cells.get(6).getText().trim().isEmpty(),
                "CST599 should appear without a grade.");
    }

    private void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("logoutLink"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginButton")));
    }

    private void openCourseEnrollments(String courseId) {
        WebElement row = wait.until(
                ExpectedConditions.visibilityOfElementLocated(courseRow(courseId)));
        row.findElement(By.id("enrollmentsLink")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[contains(normalize-space(), 'Enrollments')]")));
    }

    private void verifyStudentAppearsExactlyOnce(String studentEmail) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("body"), studentEmail));

        List<WebElement> rows = driver.findElements(By.xpath(
                "//tbody/tr[td[normalize-space()='" + studentEmail + "']]"));

        assertEquals(1, rows.size(),
                "Student sama should appear exactly once in the CST599 roster.");
    }

    private By courseRow(String courseId) {
        return By.xpath(
                "//tbody/tr[td[translate(normalize-space(), "
                        + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
                        + courseId.toLowerCase() + "']]"
        );
    }
}
