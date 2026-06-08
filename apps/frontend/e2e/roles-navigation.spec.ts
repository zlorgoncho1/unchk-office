import { expect, test } from '@playwright/test';

/**
 * Tests E2E de navigation par rôle (RBAC).
 * Vérifie que chaque rôle accède à une page autorisée (l'en-tête s'affiche) et
 * qu'il est redirigé hors d'une page interdite par le `roleGuard` (cohérent avec
 * la matrice OPA : un rôle ne peut atteindre que les pages qu'il peut réellement utiliser).
 */

const BASE = 'http://localhost:4200';

interface CompteRole {
  role: string;
  email: string;
  motDePasse: string;
  autorise: string; // page accessible
  interdit: string; // page bloquée par le roleGuard
}

const COMPTES: CompteRole[] = [
  { role: 'admin', email: 'admin@unchk.sn', motDePasse: 'Admin123!', autorise: 'budgets', interdit: 'mon-dossier' },
  { role: 'administratif', email: 'administratif@unchk.sn', motDePasse: 'Demo1234!', autorise: 'documents', interdit: 'mon-dossier' },
  { role: 'enseignant', email: 'enseignant@unchk.sn', motDePasse: 'Demo1234!', autorise: 'formations', interdit: 'budgets' },
  { role: 'appui-insertion', email: 'appui@unchk.sn', motDePasse: 'Demo1234!', autorise: 'partenaires', interdit: 'formations' },
  { role: 'etudiant', email: 'etudiant@unchk.sn', motDePasse: 'Demo1234!', autorise: 'mon-dossier', interdit: 'budgets' },
];

async function seConnecter(page: import('@playwright/test').Page, email: string, motDePasse: string): Promise<void> {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[formControlName="email"]').fill(email);
  await page.locator('input[formControlName="motDePasse"]').fill(motDePasse);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await page.waitForURL(/\/accueil/, { timeout: 20000 });
}

for (const c of COMPTES) {
  test(`RBAC ${c.role} : page autorisée affichée, page interdite redirigée`, async ({ page }) => {
    await seConnecter(page, c.email, c.motDePasse);

    // Page autorisée : l'URL est conservée et l'en-tête de page s'affiche.
    await page.goto(`${BASE}/${c.autorise}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    expect(page.url()).toContain(c.autorise);
    await expect(page.locator('unchk-page-header')).toBeVisible();

    // Page interdite : le roleGuard redirige hors de la route demandée.
    await page.goto(`${BASE}/${c.interdit}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    expect(page.url()).not.toContain(c.interdit);
  });
}

test('Connexion invalide : reste sur /login', async ({ page }) => {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[formControlName="email"]').fill('admin@unchk.sn');
  await page.locator('input[formControlName="motDePasse"]').fill('MAUVAIS_MOT_DE_PASSE');
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await page.waitForTimeout(2500);
  expect(page.url()).toContain('/login');
});

test('Déconnexion : retour à /login et routes protégées inaccessibles', async ({ page }) => {
  await seConnecter(page, 'admin@unchk.sn', 'Admin123!');
  await page.locator('button.topbar__avatar-btn').click();
  await page.getByRole('menuitem', { name: /déconnecter/i }).click();
  await page.waitForURL(/\/login/, { timeout: 10000 });
  expect(page.url()).toContain('/login');

  await page.goto(`${BASE}/formations`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1500);
  expect(page.url()).toContain('/login');
});
