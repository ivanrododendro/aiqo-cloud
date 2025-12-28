# Spécification Fonctionnelle pour le Projet de Gestion des Requêtes SQL

## 1. Liste des Requêtes

### Écran : Liste des Requêtes
- **Éléments visibles** :
  - ID de la requête
  - Texte de la requête
  - Statut de la requête (par exemple, "Traitée" ou "Non traitée")
  - Temps d'exécution (si disponible)
  - Double-click pour voir les détails de la requête
  - Bouton pour ajouter une nouvelle requête

- **Boutons** :
  1. **"Ajouter une nouvelle requête"** :
    - **Action** : Ouvre un formulaire pour ajouter une nouvelle requête.
    - **Service Backend appelé** : `POST /queries`
  2. **"Double Click"** (pour chaque requête) :
    - **Action** : Ouvre l'écran de détail de la requête sélectionnée. 
    - **Service Backend appelé** : `GET /queries/{id}/`

## 2. Détail d'une Requête

### Écran : Détail de la Requête
- **Éléments visibles** :
  - ID de la requête, non modifiable
  - Texte de la requête, non modifiable
  - Les commentaires, non modifiable
  - les notes
  - Plan d'exécution de la requête, dans le composant pev2 (VueJS), non modifiable
  - Cout total estimé en valeur, à partir du cout des tasks associées, non modifiable
  - Statut de la requête, non modifiable
  - Liste des "hints" générés par l'IA
  - Liste des exécutions optimisées de la requête

- **Boutons** :
  1. **"Enregistrer les modifications"** :
    - **Action** : Sauvegarde les modifications apportées à la requête.
    - **Service Backend appelé** : `PUT /queries/{id}`
  2. **"Supprimer"** :
    - **Action** : Supprime la requête sélectionnée.
    - **Service Backend appelé** : `DELETE /queries/{id}`
  3. **"Ajouter une execution"** :
    - **Action** : Ouvre un formulaire pour ajouter une nouvelle execution.
    - **Service Backend appelé** : `POST /queries/{id}/runs`
  4. **"Ajouter une execution optimisée"** :
    - **Action** : Ouvre un formulaire pour ajouter une nouvelle execution optimisée.
    - **Service Backend appelé** : `POST /queries/{id}/optimizedRuns`

## 3. Ajout d'une Requête

### Écran : Formulaire d'Ajout de Requête
- **Champs du formulaire** :
  - Texte de la requête
  - Plan d'exécution de la requête
  - Statut de la requête
  - Commentaires

- **Boutons** :
  1. **"Sauvegarder"** :
    - **Action** : Sauvegarde la nouvelle requête.
    - **Service Backend appelé** : `POST /queries`
  2. **"Annuler"** :
    - **Action** : Annule l'ajout de la requête et retourne à la liste des requêtes.
    - **Service Backend appelé** : Aucun

# Spécification Backend pour le Projet de Gestion des Requêtes SQL

## 1. Architecture du Backend

### 1.1 Technologie Utilisée
- **Framework** : Spring Boot 3 (avec la dernière version 21 LTS de Java).
- **Base de données** : PostgreSQL
- **JPA** : Spring Data JPA pour la gestion de la persistance des données.
- **Sécurisation** : Spring Security pour la gestion de l'authentification et de l'autorisation (optionnel pour cette version).
- **Validation des données** : Validation des entrées avec `@Valid` et `@NotNull`.
- **Spring DATA REST** : Exposition des services REST via Spring Data REST pour une configuration rapide
- **Spring AI** : Utilisation de l'IA pour l'optimisation des requêtes SQL.

### 1.2 Architecture des Services
1. **Service de Gestion des Requêtes** : Gestion de la création, lecture, mise à jour et suppression des requêtes SQL.
2. **Service des Exécutions** : Gestion des exécutions des requêtes et récupération des informations sur les exécutions.
3. **Service d'Optimisation (IA)** : Optimisation des requêtes SQL via l'IA et génération de suggestions (hints).
4. **Service de Gestion des Logs** : Téléchargement et gestion des fichiers de log, ainsi que leur association avec les requêtes.

### 1.3 Gestion de la Sécurité (optionnel)
- **Authentification via JWT ou OAuth2** pour sécuriser les points de terminaison REST.
- **Contrôle d'accès basé sur des rôles**, par exemple `ROLE_USER` et `ROLE_ADMIN`.

---

## 2. Services Backend

### 2.1 Service de Gestion des Requêtes
#### 2.1.1 Fonctionnalités
- **Créer une requête** : Permet de créer une nouvelle requête SQL dans le système.
- **Lire une requête** : Récupère les détails d'une requête spécifique.
- **Mettre à jour une requête** : Permet de modifier le texte ou le plan d'exécution d'une requête.
- **Supprimer une requête** : Permet de supprimer une requête du système.

#### 2.1.2 Endpoints
- **POST /queries** : Créer une nouvelle requête.
- **GET /queries/{id}** : Récupérer les détails d'une requête par son ID.
- **PUT /queries/{id}** : Mettre à jour les détails d'une requête.
- **DELETE /queries/{id}** : Supprimer une requête.

---

### 2.2 Service des Exécutions
#### 2.2.1 Fonctionnalités
- **Récupérer les exécutions d'une requête** : Permet d'afficher toutes les exécutions d'une requête donnée.
- **Afficher le statut des exécutions** : Permet de connaître le statut de chaque exécution.

#### 2.2.2 Points de terminaison
- **GET /queries/{id}/runs** : Récupérer les exécutions de la requête spécifiée.

---

## 4. Gestion des Erreurs

### 4.1 Gestion globale des erreurs
- Utilisation de `@ControllerAdvice` pour gérer les erreurs globalement et renvoyer des réponses uniformes avec des messages clairs.
- Erreurs communes :
  - **400 Bad Request** : Erreur de validation des données ou des paramètres.
  - **404 Not Found** : Ressource non trouvée.
  - **500 Internal Server Error** : Erreur serveur.

### 4.2 Exemple de gestion des erreurs
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResponse("Resource not found", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        return new ResponseEntity<>(new ErrorResponse("Internal Server Error", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

# Spécification Frontend pour le Projet de Gestion des Requêtes SQL

## 1. Architecture du Frontend

### 1.1 Technologie Utilisée
- **Framework** : Angular 13
- **Gestion des appels asynchrones** : RxJS pour gérer les flux de données et les requêtes API.
- **Internationalisation** : ngx-translate (pour la gestion future des traductions).
- **Gestion de l'état** : Utilisation de `BehaviorSubject` ou `Subject` pour la gestion de l'état global.
- **Gestion des formulaires** : Angular Reactive Forms pour la validation et l'envoi des données.
- **UI/UX** : Utilisation de **Angular Material** pour les composants de l'interface utilisateur (boutons, toasts, modals).
- **Toast notifications** : Utilisation des **toasts** pour afficher des notifications non intrusives pour l'utilisateur.

### 1.2 Structure des Composants
1. **Composant `QueryListComponent`** : Gère l'affichage de la liste des requêtes et des boutons d'action associés.
2. **Composant `QueryDetailComponent`** : Affiche les détails d'une requête et permet de la modifier ou de la supprimer.
3. **Composant `QueryAddComponent`** : Permet à l'utilisateur d'ajouter une nouvelle requête.
4. **Composant `QueryExecutionComponent`** : Permet d'afficher les détails de l'exécution d'une requête et de l'optimiser.
5. **Composant `LogUploadComponent`** : Permet de télécharger et de visualiser les fichiers de log.
6. **Composant `ToastComponent`** : Utilise Angular Material pour afficher des notifications de type toast.

---

## 2. Fonctionnalités Frontend

### 2.1 Liste des Requêtes
- **Écran** : Affiche toutes les requêtes enregistrées dans l'application.
  - **Éléments visibles** :
    - Liste des requêtes avec ID, texte, statut, et temps d'exécution.
    - **Boutons** : "Ajouter une nouvelle requête", "Voir les exécutions"
    - **Toast** : Affichage des messages de succès ou d'erreur via un toast (ex. : "Requête supprimée avec succès", "Erreur lors de l'ajout de la requête").
  - **Services Backend appelés** :
    - `GET /queries` pour récupérer toutes les requêtes.
    - `POST /queries` pour ajouter une nouvelle requête.
    - `GET /queries/{id}/runs` pour afficher les exécutions d'une requête spécifique.

- **Gestion de la pagination** :
  - Utilisation des **query params** (`page`, `size`, `sort`) pour gérer de grands ensembles de données et permettre à l'utilisateur de naviguer à travers les pages de requêtes.

- **Comportement** :
  - Lorsque l'utilisateur clique sur "Voir les exécutions", il est redirigé vers l'écran de détail de la requête.

- **Notifications Toast** :
  - Lors de la suppression ou modification réussie d'une requête, afficher un toast avec le message approprié (ex. : "Requête supprimée avec succès").
  - Si une erreur se produit, afficher un toast d'erreur.

### 2.2 Détail d'une Requête
- **Écran** : Affiche les détails d'une requête spécifique.
  - **Éléments visibles** :
    - Texte de la requête
    - Plan d'exécution de la requête
    - Liste des "hints" générés par l'IA
    - Liste des exécutions optimisées de la requête
  - **Boutons** : "Enregistrer les modifications", "Supprimer"
  - **Services Backend appelés** :
    - `GET /queries/{id}` pour récupérer les détails d'une requête.
    - `PUT /queries/{id}` pour sauvegarder les modifications.
    - `DELETE /queries/{id}` pour supprimer la requête.

- **Comportement** :
  - Les données sont chargées au chargement de l'écran. Si l'utilisateur modifie des informations, il peut cliquer sur "Enregistrer les modifications" pour les sauvegarder.

- **Notifications Toast** :
  - Lors de la sauvegarde ou suppression réussie, afficher un toast avec un message de succès.
  - En cas d'erreur (ex. : tentative de suppression d'une requête inexistante), un toast d'erreur doit s'afficher.

### 2.3 Ajout d'une Requête
- **Écran** : Formulaire d'ajout de requête.
  - **Champs du formulaire** :
    - Texte de la requête
    - Plan d'exécution de la requête
  - **Boutons** :
    1. **"Sauvegarder"** :
      - **Action** : Sauvegarde la nouvelle requête.
      - **Service Backend appelé** : `POST /queries`
    2. **"Annuler"** :
      - **Action** : Annule l'ajout de la requête et retourne à la liste des requêtes.
      - **Service Backend appelé** : Aucun

- **Comportement** :
  - Lorsque l'utilisateur clique sur "Sauvegarder", la nouvelle requête est envoyée au backend et l'utilisateur est redirigé vers la liste des requêtes.
  - En cas d'erreur (par exemple, champ manquant), un toast d'erreur s'affiche pour informer l'utilisateur.

### 2.4 Exécution de la Requête
- **Écran** : Affiche les détails de l'exécution d'une requête.
  - **Éléments visibles** :
    - Temps d'exécution, statut de l'exécution
    - Liste des exécutions optimisées
    - Liste des "hints" générés par l'IA
  - **Boutons** : "Exécuter l'optimisation", "Annuler"
  - **Services Backend appelés** :
    - `POST /queries/{id}/optimize` pour lancer l'optimisation.

- **Comportement** :
  - Lorsque l'utilisateur clique sur "Exécuter l'optimisation", un appel au backend est effectué pour optimiser la requête à l'aide de l'IA.
  - Un toast d'information est affiché pour informer l'utilisateur du lancement de l'optimisation.

### 2.5 Téléchargement des Logs
- **Écran** : Permet à l'utilisateur de télécharger des fichiers de log et de les consulter.
  - **Éléments visibles** :
    - Liste des fichiers de log
    - **Boutons** : "Télécharger un fichier de log", "Voir les détails"
  - **Services Backend appelés** :
    - `POST /logs/upload` pour télécharger un fichier de log.
    - `GET /logs/{id}` pour voir les détails d'un fichier de log.

- **Comportement** :
  - Le fichier de log est téléchargé via une requête `POST` et l'utilisateur peut consulter les détails des logs téléchargés.
  - En cas de problème avec le fichier (format incorrect, échec du téléchargement), un toast d'erreur est affiché.

---

# 4. Évolutions futures

## 5.1 Gestion de la Sécurité
- **Actuellement** : Nous n'avons pas intégré de mécanismes de sécurité spécifiques, tels que l'authentification ou la gestion des rôles.
- **Possible évolution** :
  - Intégration de **Spring Security** pour la gestion de l'authentification (par exemple avec JWT ou OAuth2).
  - Mise en place de **contrôles d'accès basés sur les rôles**, permettant de définir des permissions spécifiques pour les utilisateurs.

## 5.2 Gestion des Erreurs Avancée
- **Actuellement** : La gestion des erreurs est simple, avec des réponses par défaut de Spring Boot pour les erreurs communes (par exemple, 400, 404, 500).
- **Possible évolution** :
  - Ajout d'un système de gestion des erreurs plus détaillé, avec des **codes d'erreur personnalisés** et des **messages d'erreur plus précis**.
  - Mise en place d'une interface d'**administration pour visualiser les erreurs et suivre leur résolution**.

## 5.3 Localisation et Traduction
- **Actuellement** : Le projet est uniquement en anglais, sans support multilingue.
- **Possible évolution** :
  - Intégration de **ngx-translate** pour permettre une traduction facile de l'interface.
  - Ajout de **supports multilingues** (par exemple, français, espagnol, etc.) dans le frontend pour répondre à une audience internationale.

## 5.4 Optimisation et Scalabilité
- **Actuellement** : Nous avons choisi une architecture simple sans mécanisme d'autoscaling ou de mise en cache.
- **Possible évolution** :
  - **Scalabilité horizontale** des services backend pour gérer un volume plus élevé de requêtes ou d'utilisateurs.
  - Mise en place de **mécanismes de mise en cache**, par exemple avec **Redis**, pour améliorer la performance des requêtes fréquentes.
  - **Autoscaling** avec des outils comme **Kubernetes** pour ajuster la capacité en fonction de la charge.

## 5.5 Gestion des Fichiers de Log
- **Actuellement** : Les fichiers de log sont téléchargés et associés à des requêtes, mais il n'y a pas de gestion avancée des logs.
- **Possible évolution** :
  - **Stockage à long terme des logs** dans un service cloud comme **AWS S3** ou **Google Cloud Storage**, et mise en place d'un système de **recherche avancée** dans les logs.
  - **Traitement et analyse des logs** avec des outils comme **Elasticsearch** pour permettre des recherches en temps réel et une visualisation via **Kibana**.

## 5.6 Gestion des Performances et Monitoring
- **Actuellement** : Il n'y a pas de mécanismes de surveillance ou d'optimisation des performances en place.
- **Possible évolution** :
  - Intégration d'outils de **monitoring des performances** comme **Prometheus** ou **New Relic** pour surveiller l'application en production.
  - Mise en place de mécanismes de **profiling** des requêtes et des processus pour identifier les goulets d'étranglement.

## 5.7 Automatisation des Tests
- **Actuellement** : Nous n'avons pas de tests automatisés en place.
- **Possible évolution** :
  - Mise en place de tests unitaires et d'intégration avec **JUnit**, **Mockito** et **Spring Test** pour garantir la fiabilité du backend.
  - Tests end-to-end avec **Cypress** ou **Protractor** pour tester l'interface utilisateur de manière automatisée.

## 5.8 Gestion des Fichiers de Grande Taille
- **Actuellement** : Les fichiers de log sont traités de manière simple, sans validation de taille ou contrôle complexe.
- **Possible évolution** :
  - Implémentation d'une gestion avancée des fichiers de **grande taille**, avec **limites de taille configurables** et une validation du type de fichier pour éviter les fichiers malveillants.
  - Ajout de fonctionnalités pour traiter les fichiers en **flux**, en permettant un traitement asynchrone pour éviter des problèmes de performance pour les fichiers volumineux.

---
