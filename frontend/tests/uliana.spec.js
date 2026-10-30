import { test, expect } from '@playwright/test';

async function role(page, name, sample) {
  await page.getByRole('combobox', { name: 'Role', exact: true }).selectOption({ label: name });
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  if (sample) {
    await page.getByRole('combobox', { name: 'Sample user', exact: true }).selectOption({ label: sample });
    await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  }
}

test('TC-01 to TC-03 and TC-08: switch role and active user, preserve selection and scope courses', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Student Dashboard' })).toBeVisible();
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  await role(page, 'Student', 'Uliana Sova');
  await expect(page.getByRole('combobox', { name: 'Selected course' })).toContainText('AI101');
  await expect(page.getByRole('combobox', { name: 'Selected course' })).not.toContainText('SE331');
  await page.reload();
  await expect(page.getByRole('combobox', { name: 'Sample user', exact: true })).toHaveValue('5');
  await role(page, 'Instructor');
  await expect(page.getByRole('heading', { name: 'Instructor Dashboard' })).toBeVisible();
  await role(page, 'Administrator');
  await expect(page.getByRole('button', { name: 'Create course', exact: true })).toBeVisible();
});

test('TC-04 to TC-07, TC-32 and TC-33: complete course and membership management', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  await role(page, 'Administrator');
  const code = `E2E-${Date.now()}`;
  let courseId;
  try {
    await page.getByRole('button', { name: 'Create course', exact: true }).click();
    const form = page.getByRole('form', { name: 'Create course' });
    await form.getByLabel('Title', { exact: true }).fill('Browser test course');
    await form.getByLabel('Course code', { exact: true }).fill(code);
    await form.getByLabel('Description', { exact: true }).fill('Temporary test course.');
    await form.getByRole('button', { name: 'Save course' }).click();
    await expect(form.getByRole('alert')).toContainText('Assign at least one instructor');
    await form.getByRole('checkbox', { name: 'Andrii Shevchenko' }).check();
    await form.getByRole('button', { name: 'Save course' }).click();
    await expect(page.getByRole('status')).toContainText('Course saved');
    courseId = Number(await page.getByRole('combobox', { name: 'Selected course' }).inputValue());
    const people = page.getByRole('region', { name: 'Manage people' });
    await people.getByRole('button', { name: 'Save instructors' }).click();
    await expect(people.getByRole('status')).toContainText('Instructors updated');
    await people.getByRole('combobox', { name: 'Student', exact: true }).selectOption({ label: 'Ksenia Maranda' });
    await people.getByRole('button', { name: 'Enroll student' }).click();
    await expect(people.getByRole('button', { name: 'Remove Ksenia Maranda' })).toBeVisible();
    await people.getByRole('button', { name: 'Enroll student' }).click();
    await expect(people.getByRole('alert')).toContainText('already enrolled');
    await role(page, 'Student', 'Ksenia Maranda');
    await expect(page.getByRole('combobox', { name: 'Selected course' })).toContainText(code);
    await role(page, 'Administrator');
    await page.getByRole('combobox', { name: 'Selected course' }).selectOption(String(courseId));
    await people.getByRole('button', { name: 'Remove Ksenia Maranda' }).click();
    await expect(people.getByRole('button', { name: 'Remove Ksenia Maranda' })).toHaveCount(0);
    await page.getByRole('button', { name: 'Edit course', exact: true }).click();
    const edit = page.getByRole('form', { name: 'Edit course' });
    await edit.getByLabel('Course code', { exact: true }).fill('se331');
    await edit.getByRole('button', { name: 'Save course' }).click();
    await expect(edit.getByRole('alert')).toContainText('already exists');
    await edit.getByLabel('Course code', { exact: true }).fill(code);
    await edit.getByLabel('Title', { exact: true }).fill('Updated browser course');
    await edit.getByRole('button', { name: 'Save course' }).click();
    await expect(page.getByRole('heading', { name: `${code} — Updated browser course` })).toBeVisible();
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Archive course', exact: true }).click();
    await expect(page.getByRole('combobox', { name: 'Selected course' })).toContainText('(Archived)');
    await role(page, 'Student', 'Ksenia Maranda');
    await expect(page.getByRole('combobox', { name: 'Selected course' })).not.toContainText(code);
    await role(page, 'Administrator');
    await page.getByRole('combobox', { name: 'Selected course' }).selectOption(String(courseId));
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Delete course', exact: true }).click();
    await expect(page.getByRole('combobox', { name: 'Selected course' })).not.toContainText(code);
  } finally {
    if (courseId) await page.request.delete(`/api/courses/${courseId}`, { headers: { 'X-User-Id': '1' } });
  }
});

test('TC-30 and TC-31: real quiz progress, gradebook agreement, analytics and missing-work list', async ({ page }, testInfo) => {
  await page.goto('/');
  await expect(page.getByText('Loading courses...', { exact: true })).toHaveCount(0);
  await role(page, 'Student', 'Yelyzaveta Maranda');
  await page.getByRole('button', { name: 'View progress' }).click();
  const report = page.getByRole('region', { name: 'Student progress' });
  await expect(report.getByText('2 / 3', { exact: true })).toBeVisible();
  await expect(report.getByText('44.4% (20 / 45 points)')).toBeVisible();
  await expect(report.getByText('1 submitted assessment(s) pending grading.')).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('student-progress.png'), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath('student-progress-mobile.png'), fullPage: true });
  await role(page, 'Instructor');
  await page.getByRole('combobox', { name: 'Selected course' }).selectOption('1');
  await page.getByRole('button', { name: 'View analytics' }).click();
  const analytics = page.getByRole('region', { name: 'Instructor analytics' });
  await expect(analytics.getByRole('row').filter({ hasText: 'Week 1 Quiz' })).toContainText('50%');
  await expect(analytics.getByText('Ksenia Maranda — Week 1 Quiz')).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('instructor-analytics.png'), fullPage: true });
});
