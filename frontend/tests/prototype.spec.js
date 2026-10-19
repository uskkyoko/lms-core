import { test, expect } from '@playwright/test';

async function switchRole(page, role) {
  const labels = { STUDENT: 'Student - Kseniia', INSTRUCTOR: 'Instructor - Liza', ADMINISTRATOR: 'Administrator - Uliana' };
  await page.getByRole('banner').getByRole('combobox', { name: 'Role', exact: true }).selectOption({ label: labels[role] });
  await expect(page.getByRole('status').filter({ hasText: 'Now viewing as' })).toBeVisible();
  await expect(page.getByText('Loading your workspace\u2026')).toHaveCount(0);
}

test('TC-01 to TC-03, TC-08: role switching, scoped courses and reload', async ({ page }) => {
  await page.goto('/');
  await switchRole(page, 'STUDENT');
  await expect(page.getByRole('heading', { name: 'Introduction to Software Engineering' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Database Fundamentals' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Create course' })).toHaveCount(0);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Introduction to Software Engineering' })).toBeVisible();
  await switchRole(page, 'INSTRUCTOR');
  await expect(page.getByRole('heading', { name: 'Introduction to Software Engineering' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create course' })).toHaveCount(0);
  await switchRole(page, 'ADMINISTRATOR');
  await expect(page.getByRole('button', { name: 'Create course' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Database Fundamentals' })).toBeVisible();
  await page.screenshot({ path: '../tmp/prototype-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('banner').getByRole('combobox', { name: 'Role' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: '../tmp/prototype-mobile.png', fullPage: true });
});

test('TC-04, TC-05, TC-32: create, validate duplicate, edit, archive and delete', async ({ page }) => {
  await page.goto('/');
  await switchRole(page, 'ADMINISTRATOR');
  const code = `E2E-${Date.now()}`;
  const title = 'Prototype demonstration';
  const card = () => page.getByRole('article').filter({ hasText: code });
  try {
    await page.getByRole('button', { name: 'Create course' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Course title').fill(title);
    await dialog.getByLabel('Course code').fill(code);
    await dialog.getByLabel('Description').fill('A course created through the React interface.');
    await dialog.getByRole('button', { name: 'Create course' }).click();
    await expect(dialog.getByRole('alert')).toContainText('Select at least one instructor');
    await dialog.getByLabel('Liza').check();
    await dialog.getByRole('button', { name: 'Create course' }).click();
    await expect(card()).toContainText(title);
    await page.reload();
    await expect(card()).toContainText(title);
    await page.getByRole('button', { name: 'Create course' }).click();
    await dialog.getByLabel('Course title').fill('Duplicate');
    await dialog.getByLabel('Course code').fill(code.toLowerCase());
    await dialog.getByLabel('Description').fill('Should not be saved.');
    await dialog.getByLabel('Liza').check();
    await dialog.getByRole('button', { name: 'Create course' }).click();
    await expect(dialog.getByRole('alert')).toContainText('already exists');
    await dialog.getByRole('button', { name: 'Cancel' }).click();
    await card().getByRole('button', { name: 'Edit course' }).click();
    await dialog.getByLabel('Course title').fill('Updated prototype demonstration');
    await dialog.getByRole('button', { name: 'Save changes' }).click();
    await expect(card()).toContainText('Updated prototype demonstration');
    await card().getByRole('button', { name: 'Archive', exact: true }).click();
    await dialog.getByRole('button', { name: 'Archive course' }).click();
    await expect(card().getByText('Archived', { exact: true })).toBeVisible();
    await card().getByRole('button', { name: 'Delete', exact: true }).click();
    await dialog.getByRole('button', { name: 'Cancel' }).click();
    await expect(card()).toBeVisible();
    await card().getByRole('button', { name: 'Delete', exact: true }).click();
    await dialog.getByRole('button', { name: 'Delete course' }).click();
    await expect(card()).toHaveCount(0);
  } finally {
    const response = await page.request.get('/api/courses');
    for (const course of await response.json()) {
      if (course.code === code) await page.request.delete(`/api/courses/${course.id}`);
    }
  }
});

test('shows a recoverable error when the backend is unavailable', async ({ page }) => {
  await page.route('**/api/users', route => route.abort());
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('Cannot reach the server');
  await page.unroute('**/api/users');
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.getByRole('banner').getByRole('combobox', { name: 'Role' })).toBeEnabled();
});
