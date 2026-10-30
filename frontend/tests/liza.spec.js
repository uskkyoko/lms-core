import { test, expect } from '@playwright/test';

async function role(page, name, sample) {
  await page.getByRole('combobox', { name: 'Role', exact: true }).selectOption({ label: name });
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  if (sample) {
    await page.getByRole('combobox', { name: 'Sample user', exact: true }).selectOption({ label: sample });
    await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  }
}

test('TC-19, TC-21, TC-35: student takes a multiple-choice quiz once and reviews answers', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  await role(page, 'Student', 'Ksenia Maranda');
  await page.getByRole('button', { name: 'Quizzes', exact: true }).first().click();
  const week1 = page.locator('tr', { hasText: 'Week 1 Quiz' });
  await week1.getByRole('button', { name: 'Start quiz' }).click();
  await expect(page.getByText('Time left')).toBeVisible();
  await page.getByRole('radio', { name: 'Student, Instructor, and Administrator' }).check();
  await page.getByRole('radio', { name: 'Exactly one attempt' }).check();
  await page.getByRole('button', { name: 'Submit quiz' }).click();
  await expect(page.getByRole('heading', { name: 'Quiz results' })).toBeVisible();
  await expect(page.getByText('Score:')).toContainText('20');
  await page.getByRole('button', { name: 'Review answers' }).click();
  await expect(page.getByText('correct', { exact: false }).first()).toBeVisible();
  await page.getByRole('button', { name: 'Back to quizzes' }).click();
  await expect(week1.getByRole('button', { name: 'Start quiz' })).toHaveCount(0);
});

test('TC-25 and TC-36: student grades are own-only; instructor gradebook lists everyone', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  await role(page, 'Student', 'Yelyzaveta Maranda');
  await page.getByRole('navigation', { name: 'Workspace' }).getByRole('button', { name: 'Grades' }).click();
  const grades = page.getByRole('article', { name: 'My grades' });
  await expect(grades).toContainText('Yelyzaveta Maranda');
  await expect(grades).not.toContainText('Ksenia Maranda');
  await expect(grades).toContainText('Week 1 Quiz');
  await expect(grades).toContainText('Pending grading');
  await role(page, 'Instructor');
  await page.getByRole('navigation', { name: 'Workspace' }).getByRole('button', { name: 'Gradebook' }).click();
  const book = page.getByRole('article', { name: 'Gradebook' });
  await expect(book).toContainText('Ksenia Maranda');
  await expect(book).toContainText('Yelyzaveta Maranda');
  await expect(book).toContainText('Week 1 Quiz');
  await expect(book.getByRole('button', { name: 'Change' }).first()).toBeVisible();
});

test('TC-26: instructor grade change is written to history', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  await role(page, 'Instructor');
  await page.getByRole('navigation', { name: 'Workspace' }).getByRole('button', { name: 'Gradebook' }).click();
  await page.getByRole('button', { name: 'Change' }).first().click();
  await page.getByLabel('Points', { exact: false }).fill('18');
  await page.getByLabel('Reason').fill('Partial credit');
  await page.getByRole('button', { name: 'Save grade' }).click();
  await expect(page.getByRole('status')).toContainText('Grade updated');
  await expect(page.getByRole('heading', { name: 'Grade history' })).toBeVisible();
  await expect(page.getByText('Andrii Shevchenko')).toBeVisible();
  await expect(page.getByText('Partial credit')).toBeVisible();
});
