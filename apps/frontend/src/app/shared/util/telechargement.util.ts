// ============================================================
// Utilitaire de téléchargement de fichier binaire (Blob) côté navigateur.
// Sert aux exports PDF / Excel : le service récupère le Blob (requête
// authentifiée par l'intercepteur), puis on déclenche le téléchargement ici.
// Aucune dépendance Angular : fonction pure.
// ============================================================

// Déclenche le téléchargement d'un Blob sous le nom de fichier indiqué.
// Crée une URL objet temporaire, un lien <a download>, le clique, puis nettoie.
export function telechargerBlob(blob: Blob, nomFichier: string): void {
  // URL temporaire pointant vers le contenu du Blob en mémoire.
  const url = URL.createObjectURL(blob);
  // Lien d'ancrage invisible portant l'attribut download.
  const lien = document.createElement('a');
  lien.href = url;
  lien.download = nomFichier;
  // Le click() déclenche le téléchargement navigateur.
  document.body.appendChild(lien);
  lien.click();
  // Nettoyage : on retire le lien et on libère l'URL objet.
  document.body.removeChild(lien);
  URL.revokeObjectURL(url);
}
