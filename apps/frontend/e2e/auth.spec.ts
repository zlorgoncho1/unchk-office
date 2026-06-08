import { test, expect } from '@playwright/test';

// Identifiants du compte de démonstration (migration Flyway V3 d'identity-service).
const ADMIN = { email: 'admin@unchk.sn', motDePasse: 'Admin123!' };

// Sélecteurs robustes (les libellés Material sont ambigus : le bouton « Afficher le
// mot de passe » porte aussi « mot de passe »), on cible donc les formControlName.
const champEmail = 'input[formControlName="email"]';
const champMotDePasse = 'input[formControlName="motDePasse"]';

test.describe('Parcours d\'authentification', () => {
  test('redirige vers /login quand on accède à un espace protégé sans session', async ({ page }) => {
    await page.goto('/accueil');
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('heading', { name: 'UNCHK Office' })).toBeVisible();
  });

  test('refuse la connexion avec de mauvais identifiants', async ({ page }) => {
    await page.goto('/login');
    await page.locator(champEmail).fill('admin@unchk.sn');
    await page.locator(champMotDePasse).fill('MauvaisMotDePasse1');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    // Un message d'erreur s'affiche et on reste sur /login.
    await expect(page.locator('[role="alert"]')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('connexion réussie : redirige vers l\'espace d\'accueil et quitte le login', async ({ page }) => {
    await page.goto('/login');
    await page.locator(champEmail).fill(ADMIN.email);
    await page.locator(champMotDePasse).fill(ADMIN.motDePasse);
    await page.getByRole('button', { name: 'Se connecter' }).click();
    // Redirection vers /accueil (puis le dashboard du rôle).
    await expect(page).toHaveURL(/\/accueil/, { timeout: 15_000 });
    // Le formulaire de connexion a disparu (on est dans le shell applicatif).
    await expect(page.locator(champMotDePasse)).toHaveCount(0);
  });
});
