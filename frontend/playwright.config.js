import { defineConfig } from '@playwright/test';
import { fileURLToPath } from 'node:url';

export default defineConfig({
  testDir: './tests', workers: 1, fullyParallel: false,
  use: { baseURL: 'http://127.0.0.1:15173', channel: 'chrome', screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  webServer: [
    {
      command: 'java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=18080 --spring.datasource.url=jdbc:h2:mem:browser-tests --spring.jpa.hibernate.ddl-auto=create-drop --lms.allowed-origins=http://127.0.0.1:15173',
      cwd: fileURLToPath(new URL('../backend/demo', import.meta.url)),
      url: 'http://127.0.0.1:18080/api/users', timeout: 90000, reuseExistingServer: false,
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 15173 --strictPort',
      env: { LMS_API_TARGET: 'http://127.0.0.1:18080' },
      url: 'http://127.0.0.1:15173', reuseExistingServer: false,
    },
  ],
});
